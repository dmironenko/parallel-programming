import java.util.ArrayList;
import java.util.List;

public class ThreadLocalMetricsCollector implements MetricsCollector {
  private final List<ThreadState> states = new ArrayList<>();
  private final Object lock = new Object();

  private final ThreadLocal<ThreadState> local =
      ThreadLocal.withInitial(
          () -> {
            ThreadState state = new ThreadState();

            synchronized (this.lock) {
              this.states.add(state);
            }

            return state;
          });

  @Override
  public void record(long value) {
    ThreadState state = this.local.get();
    int bucketIndex = (int) Math.min(value / 4, 255);

    state.buckets.setRelease(bucketIndex, state.buckets.getPlain(bucketIndex) + 1);

    state.count.setRelease(state.count.getPlain() + 1);
    state.sum.setRelease(state.sum.getPlain() + value);

    if (value < state.min.getPlain()) {
      state.min.setRelease(value);
    }

    if (value > state.max.getPlain()) {
      state.max.setRelease(value);
    }
  }

  @Override
  public Snapshot snapshot() {
    List<ThreadState> statesCopy;
    synchronized (this.lock) {
      statesCopy = new ArrayList<>(this.states);
    }

    long[] buckets = new long[256];

    long count = 0;
    long sum = 0;
    long min = Long.MAX_VALUE;
    long max = 0;

    for (ThreadState state : statesCopy) {
      for (int i = 0; i < buckets.length; i++) {
        buckets[i] += state.buckets.get(i);
      }

      count += state.count.get();
      sum += state.sum.get();
      min = Math.min(min, state.min.get());
      max = Math.max(max, state.max.get());
    }

    long p50 = this.percentile(buckets, count, 0.50);
    long p99 = this.percentile(buckets, count, 0.99);

    return new Snapshot(buckets, count, sum, min, max, p50, p99);
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
