import java.util.concurrent.atomic.AtomicInteger;

class ThreadBuffers {
  static final int NOWHERE = -1;

  final long[][] buckets = new long[2][256];

  final long[] count = new long[2];
  final long[] sum = new long[2];
  final long[] min = {Long.MAX_VALUE, Long.MAX_VALUE};
  final long[] max = new long[2];

  final AtomicInteger inside = new AtomicInteger(NOWHERE);
}
