import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class InconsistencyTest {
  private final int threadsCount;
  private final int snapshotsCount;

  public InconsistencyTest(int threadsCount, int snapshotsCount) {
    this.threadsCount = threadsCount;
    this.snapshotsCount = snapshotsCount;
  }

  public static void main(String[] args) throws InterruptedException {
    ZipfGenerator generator = new ZipfGenerator(1023, 1.15);
    long[] values = generator.generate(1 << 20, 42);

    MetricsCollector collector = new DoubleBufferedMetricsCollector();
    InconsistencyTest test = new InconsistencyTest(4, 10_000);

    test.run(collector, values);
  }

  public void run(MetricsCollector collector, long[] values) throws InterruptedException {
    CountDownLatch writing = new CountDownLatch(this.threadsCount);
    AtomicBoolean stop = new AtomicBoolean(false);

    Thread[] threads = new Thread[this.threadsCount];
    long[] operations = new long[this.threadsCount];

    for (int i = 0; i < this.threadsCount; i++) {
      int id = i;

      threads[i] =
          new Thread(
              () -> {
                long count = 0;
                int index = id * 1000;

                collector.record(values[index]);
                count++;

                writing.countDown();

                while (!stop.get()) {
                  index++;
                  if (index == values.length) {
                    index = 0;
                  }

                  collector.record(values[index]);
                  count++;
                }

                operations[id] = count;
              });

      threads[i].start();
    }

    writing.await();

    int sumLess = 0;
    int sumGreater = 0;

    for (int i = 0; i < this.snapshotsCount; i++) {
      Snapshot snapshot = collector.snapshot();

      long sum = 0;
      for (long bucket : snapshot.buckets()) {
        sum += bucket;
      }

      if (sum < snapshot.count()) {
        sumLess++;
      } else if (sum > snapshot.count()) {
        sumGreater++;
      }
    }

    stop.set(true);

    for (Thread thread : threads) {
      thread.join();
    }

    long actualCount = 0;
    for (long count : operations) {
      actualCount += count;
    }

    long finalCount = collector.snapshot().count();
    double brokenPercent = 100.0 * (sumLess + sumGreater) / this.snapshotsCount;

    System.out.printf("inconsistency result: %.2f%%%n", brokenPercent);
    System.out.printf("sum < count: %d, sum > count: %d%n", sumLess, sumGreater);
    System.out.printf(
        "count: %d, calls: %d, difference: %d%n",
        finalCount, actualCount, finalCount - actualCount);
  }
}
