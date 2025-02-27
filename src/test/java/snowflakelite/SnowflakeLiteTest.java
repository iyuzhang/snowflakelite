package snowflakelite;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.github.snowflakelite.SnowflakeLite;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class SnowflakeLiteTest {

    public static void main(String[] args) throws InterruptedException, IOException {
        SnowflakeLite snowflakeLite = new SnowflakeLite(0, 0);
        int count = 100000000;
        ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue<>(count);

        int producer = 20;
        CountDownLatch countDownLatch = new CountDownLatch(producer);
        for (int i = 0; i < producer; i++) {
            new Thread(() -> {
                for (;;) {
                    long id = snowflakeLite.nextId();
                    boolean offer = queue.offer(id);
                    if (!offer) {
                        countDownLatch.countDown();
                        System.out.println(Thread.currentThread().getName() + " has exited");
                        return;
                    }
                }
            }).start();
        }
        countDownLatch.await();

        BloomFilter<Long> bloomFilter = BloomFilter.create(Funnels.longFunnel(), count, 0.000000001);
        while (!queue.isEmpty()) {
            long id = queue.poll();
            if (!bloomFilter.mightContain(id)) {
                bloomFilter.put(id);
            } else {
                System.out.println(id + " is already in the bloom filter");
            }
        }
        System.out.println("over=====================");
        Thread.sleep(1000 * 100);
    }
}
