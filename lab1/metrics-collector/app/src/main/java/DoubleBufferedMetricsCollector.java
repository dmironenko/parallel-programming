import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DoubleBufferedMetricsCollector implements MetricsCollector {
  private final Object lock = new Object();
  private final List<ThreadBuffers> states = new ArrayList<>();

  private final ThreadLocal<ThreadBuffers> local =
      ThreadLocal.withInitial(
          () -> {
            ThreadBuffers state = new ThreadBuffers();

            synchronized (this.lock) {
              this.states.add(state);
            }

            return state;
          });

  private volatile int active;

  private final long[] globalBuckets = new long[256];

  private long globalCount;
  private long globalSum;
  private long globalMin = Long.MAX_VALUE;
  private long globalMax;

  @Override
  public void record(long value) {
    ThreadBuffers state = this.local.get();

    int buffer;
    while (true) {
      buffer = this.active;
      state.inside.set(buffer);

      if (this.active == buffer) {
        break;
      }

      state.inside.setRelease(ThreadBuffers.NOWHERE);
    }

    int bucketIndex = (int) Math.min(value / 4, 255);
    state.buckets[buffer][bucketIndex]++;

    state.count[buffer]++;
    state.sum[buffer] += value;
    state.min[buffer] = Math.min(state.min[buffer], value);
    state.max[buffer] = Math.max(state.max[buffer], value);

    state.inside.setRelease(ThreadBuffers.NOWHERE);
  }

  @Override
  public Snapshot snapshot() {
    synchronized (this.lock) {
      int old = this.active;
      this.active = 1 - old;

      for (ThreadBuffers state : this.states) {
        while (state.inside.get() == old) {
          Thread.onSpinWait();
        }
      }

      for (ThreadBuffers state : this.states) {
        for (int i = 0; i < this.globalBuckets.length; i++) {
          this.globalBuckets[i] += state.buckets[old][i];
        }

        this.globalCount += state.count[old];
        this.globalSum += state.sum[old];
        this.globalMin = Math.min(this.globalMin, state.min[old]);
        this.globalMax = Math.max(this.globalMax, state.max[old]);

        Arrays.fill(state.buckets[old], 0);

        state.count[old] = 0;
        state.sum[old] = 0;
        state.min[old] = Long.MAX_VALUE;
        state.max[old] = 0;
      }

      long[] bucketsSnapshot = this.globalBuckets.clone();

      long p50 = this.percentile(bucketsSnapshot, this.globalCount, 0.50);
      long p99 = this.percentile(bucketsSnapshot, this.globalCount, 0.99);

      return new Snapshot(
          bucketsSnapshot,
          this.globalCount,
          this.globalSum,
          this.globalMin,
          this.globalMax,
          p50,
          p99);
    }
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
