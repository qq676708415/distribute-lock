package com.llj.lock;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.List;
import java.util.stream.Collectors;

public class ZookeeperLock {
    // 连接zk Server
    private ZkClient zkClient;
    // 创建临时序号节点
    public ZookeeperLock() {
        zkClient = new ZkClient("127.0.0.1:2181", 30000, 20000);
    }

    // 1、获得锁
    public Lock lock(String lockId, long timeout) {
        // 创建写锁
        Lock lockNode = createLockNode(lockId);
        lockNode = tryActiveLock(lockNode);
        if (!lockNode.isActive()) {
            try {
                synchronized (lockNode) {
                    lockNode.wait(timeout);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }

        return lockNode;
    }

    //2、激活锁
    public Lock tryActiveLock(Lock lockNode) {
        // 判断是否获得锁
        List<String> list = zkClient.getChildren("/lock")
                .stream()
                .sorted()
                .map(p -> "/lock/" + p)
                .collect(Collectors.toList());
        String firstPath = list.get(0);
        if (firstPath.equals(lockNode.getPath())) {
            lockNode.setActive(true);
        } else {
            // 添加上个节点的变更监听
            int lastPathIndex = list.indexOf(lockNode.getPath()) - 1;
            String lastPath = list.get(lastPathIndex);
            zkClient.subscribeDataChanges(lastPath, new IZkDataListener() {
                @Override
                public void handleDataChange(String s, Object o) throws Exception {

                }

                @Override
                public void handleDataDeleted(String s) throws Exception {
                    // 事件处理与心跳在同一线程，如果debug时占用太多时间，将导致本节点被删除，从而影响锁逻辑
                    System.out.println("节点删除：" + s);
                    Lock lock = tryActiveLock(lockNode);
                    synchronized (lockNode) {
                        if (lock.isActive()) {
                            lockNode.notify();
                        }
                    }
                    zkClient.unsubscribeDataChanges(lastPath, this);
                }
            });
        }
        return lockNode;
    }

    // 3、释放锁
    public void unlock(Lock lock) {
        if (lock.isActive()) {
            zkClient.delete(lock.getPath());
        }
    }

    private Lock createLockNode(String lockId) {
        String path = zkClient.createEphemeralSequential("/lock/" + lockId, "w");
        Lock lock = new Lock();
        lock.setActive(false);
        lock.setLockId(lockId);
        lock.setPath(path);
        return lock;
    }


}
