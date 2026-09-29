import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Benchmark {
  private final BenchmarkConfig config;

  public Benchmark(BenchmarkConfig config) {
    this.config = config;
  }

  public long measure(MetricsCollector collector, long[] values) throws InterruptedException {
    this.run(collector, values, this.config.warmupDurationSeconds());

    long[] results = new long[this.config.experimentsCount()];
    for (int i = 0; i < results.length; i++) {
      results[i] = this.run(collector, values, this.config.benchmarkDurationSeconds());
    }

    System.out.println(collector.snapshot().count());
    System.out.println(Arrays.toString(results));

    Arrays.sort(results);
    return results[results.length / 2];
  }

  private long run(MetricsCollector collector, long[] values, long durationSeconds)
      throws InterruptedException {
    CountDownLatch start = new CountDownLatch(1);
    AtomicBoolean stop = new AtomicBoolean(false);

    long[] threadsOperations = new long[this.config.threadsCount()];
    Thread[] threads = new Thread[this.config.threadsCount()];

    for (int i = 0; i < this.config.threadsCount(); i++) {
      int id = i;

      threads[i] =
          new Thread(
              () -> {
                long operations = 0;
                int j = id * 1000;

                try {
                  start.await();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return;
                }

                while (!stop.get()) {
                  collector.record(values[j]);
                  operations++;

                  j++;
                  if (j == values.length) {
                    j = 0;
                  }
                }

                threadsOperations[id] = operations;
              });

      threads[i].start();
    }

    long startTime = System.nanoTime();
    start.countDown();
    Thread.sleep(durationSeconds * 1000);
    stop.set(true);
    long finishTime = System.nanoTime();

    for (Thread thread : threads) {
      thread.join();
    }

    long totalOperations = 0;
    for (long operations : threadsOperations) {
      totalOperations += operations;
    }

    double elapsedSeconds = (finishTime - startTime) / 1e9;
    return Math.round(totalOperations / elapsedSeconds);
  }
}
