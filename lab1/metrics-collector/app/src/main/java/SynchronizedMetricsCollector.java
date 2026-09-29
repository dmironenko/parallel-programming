public class SynchronizedMetricsCollector implements MetricsCollector {
  private final MetricsCollector collector = new SimpleMetricsCollector();

  @Override
  public synchronized void record(long value) {
    this.collector.record(value);
  }

  @Override
  public synchronized Snapshot snapshot() {
    return this.collector.snapshot();
  }
}
