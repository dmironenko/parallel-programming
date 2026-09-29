public class SimpleMetricsCollector implements MetricsCollector {
  private final long[] buckets = new long[256];
  private long count;
  private long sum;
  private long min = Long.MAX_VALUE;
  private long max;

  @Override
  public void record(long value) {
    int bucketIndex = (int) Math.min(value / 4, 255);
    this.buckets[bucketIndex]++;

    this.count += 1;
    this.sum += value;
    this.min = Math.min(this.min, value);
    this.max = Math.max(this.max, value);
  }

  @Override
  public Snapshot snapshot() {
    long[] bucketsSnapshot = this.buckets.clone();

    long p50 = this.percentile(bucketsSnapshot, this.count, 0.50);
    long p99 = this.percentile(bucketsSnapshot, this.count, 0.99);

    return new Snapshot(bucketsSnapshot, this.count, this.sum, this.min, this.max, p50, p99);
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

    throw new IllegalStateException("invalid count");
  }
}
