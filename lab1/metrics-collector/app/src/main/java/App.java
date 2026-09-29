public class App {
  public static void main(String[] args) throws InterruptedException {
    ZipfGenerator generator = new ZipfGenerator(1023, 1.15);
    long[] values = generator.generate(1 << 20, 42);

    MetricsCollector collector = new DoubleBufferedMetricsCollector();
    BenchmarkConfig config = new BenchmarkConfig(5, 5, 1, 5);
    Benchmark benchmark = new Benchmark(config);

    long ops = benchmark.measure(collector, values);
    System.out.printf("benchmark result: %d op/sec%n", ops);
  }
}
