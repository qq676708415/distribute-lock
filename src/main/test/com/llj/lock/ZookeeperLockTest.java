package com.llj.lock;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ZookeeperLockTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() throws InterruptedException {
        ZookeeperLock zookeeperLock = new ZookeeperLock();
        Lock nana = zookeeperLock.lock("nana", 365 * 24 * 3600 * 1000);
        System.out.println("获得了nana锁：");
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    public void run() throws InterruptedException, IOException {
        ZookeeperLock zookeeperLock = new ZookeeperLock();
        File file = new File("/Users/llj/Downloads/test.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 1000; i++) {
            executorService.execute(() -> {

                Lock lock = zookeeperLock.lock("test.txt", 60 * 1000);
                System.out.println(lock.getPath());
                try {
                    String firstLine = Files.lines(file.toPath()).findFirst().orElse("0");
                    System.out.println("===="+firstLine);
                    int count = Integer.parseInt(firstLine);
                    count++;
                    Files.write(file.toPath(), String.valueOf(count).getBytes());
                } catch (IOException e)  {
                    e.printStackTrace();
                } finally {
                    zookeeperLock.unlock(lock);
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        String firstLine = Files.lines(file.toPath()).findFirst().orElse("0");
        System.out.println(firstLine);
        /**
         * 节点删除：/lock/test.txt0000002681
         * /lock/test.txt0000002682
         * ====994
         * 节点删除：/lock/test.txt0000002682
         * /lock/test.txt0000002683
         * ====995
         * 节点删除：/lock/test.txt0000002683
         * /lock/test.txt0000002684
         * ====996
         * 节点删除：/lock/test.txt0000002684
         * /lock/test.txt0000002685
         * ====997
         * 节点删除：/lock/test.txt0000002685
         * /lock/test.txt0000002686
         * ====998
         * 节点删除：/lock/test.txt0000002686
         * /lock/test.txt0000002687
         * ====999
         * 1000
         */
    }
}
