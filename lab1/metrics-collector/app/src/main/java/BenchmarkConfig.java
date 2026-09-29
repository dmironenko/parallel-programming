public record BenchmarkConfig(
    int warmupDurationSeconds,
    int benchmarkDurationSeconds,
    int threadsCount,
    int experimentsCount) {}
