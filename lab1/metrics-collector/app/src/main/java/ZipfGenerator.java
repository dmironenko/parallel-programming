import java.util.Random;

public class ZipfGenerator {
  private final int maxValue;
  private final double exponent;

  public ZipfGenerator(int maxValue, double exponent) {
    this.maxValue = maxValue;
    this.exponent = exponent;
  }

  public long[] generate(int size, long seed) {
    Random random = new Random(seed);

    double[] distribution = this.createDistribution();

    long[] values = new long[size];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.sample(distribution, random);
    }

    return values;
  }

  private double[] createDistribution() {
    double[] distribution = new double[this.maxValue];

    double sum = 0;
    for (int k = 1; k <= this.maxValue; k++) {
      sum += 1.0 / Math.pow(k, this.exponent);
      distribution[k - 1] = sum;
    }

    for (int i = 0; i < distribution.length; i++) {
      distribution[i] /= sum;
    }

    return distribution;
  }

  private long sample(double[] distribution, Random random) {
    double value = random.nextDouble();

    int left = 0;
    int right = distribution.length - 1;

    while (left < right) {
      int middle = (left + right) / 2;
      if (value < distribution[middle]) {
        right = middle;
      } else {
        left = middle + 1;
      }
    }

    return left + 1;
  }
}
