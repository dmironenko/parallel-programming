import java.util.concurrent.atomic.AtomicLong;

public class ShardedMetricsCollector implements MetricsCollector {
  private final long[] buckets = new long[256];
  private final Object[] locks;

  private final AtomicLong count = new AtomicLong();
  private final AtomicLong sum = new AtomicLong();
  private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong max = new AtomicLong();

  public ShardedMetricsCollector(int shardsCount) {
    this.locks = new Object[shardsCount];
    for (int i = 0; i < this.locks.length; i++) {
      this.locks[i] = new Object();
    }
  }

  @Override
  public void record(long value) {
    int bucketIndex = (int) Math.min(value / 4, 255);
    int shard = bucketIndex % this.locks.length;

    synchronized (this.locks[shard]) {
      this.buckets[bucketIndex]++;
    }

    this.count.incrementAndGet();
    this.sum.addAndGet(value);

    long currentMin;
    do {
      currentMin = this.min.get();
      if (value >= currentMin) {
        break;
      }
    } while (!this.min.compareAndSet(currentMin, value));

    long currentMax;
    do {
      currentMax = this.max.get();
      if (value <= currentMax) {
        break;
      }
    } while (!this.max.compareAndSet(currentMax, value));
  }

  @Override
  public Snapshot snapshot() {
    long[] bucketsSnapshot = new long[this.buckets.length];

    for (int shard = 0; shard < this.locks.length; shard++) {
      synchronized (this.locks[shard]) {
        for (int i = shard; i < this.buckets.length; i += this.locks.length) {
          bucketsSnapshot[i] = this.buckets[i];
        }
      }
    }

    long count = this.count.get();
    long sum = this.sum.get();
    long min = this.min.get();
    long max = this.max.get();

    long p50 = this.percentile(bucketsSnapshot, count, 0.50);
    long p99 = this.percentile(bucketsSnapshot, count, 0.99);

    return new Snapshot(bucketsSnapshot, count, sum, min, max, p50, p99);
  }

  private long percentile(long[] buckets, long count, double percent) {
    double threshold = count * percent;
    long total = 0;

    for (int i = 0; i < buckets.length; i++) {
      total += buckets[i];
      if (total >= threshold) {
        return i * 4;
      }
    }

    return (buckets.length - 1) * 4;
  }
}
