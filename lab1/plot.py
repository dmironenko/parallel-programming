from pathlib import Path
import matplotlib.pyplot as plt

threads = [1, 2, 4, 8, 16]
simple_threads = [1]

simple = [201695936]
synchronized = [54564911, 16324173, 13503784, 16007419, 14581795]
lock_only = [64734140, 19935380, 16500480, 16486307, 18267795]
sharded = [55767927, 14532288, 10206668, 9880229, 10024285]
thread_local = [85814851, 148377203, 245759632, 302397123, 263546773]
double_buffer = [63835921, 113246614, 175967916, 245219246, 228055369]

plt.plot(simple_threads, simple, marker='o', label='simple')
plt.plot(threads, synchronized, marker='o', label='synchronized')
plt.plot(threads, lock_only, marker='o', label='lock only')
plt.plot(threads, sharded, marker='o', label='sharded')
plt.plot(threads, thread_local, marker='o', label='thread local')
plt.plot(threads, double_buffer, marker='o', label='double buffer')

plt.xlabel('threads')
plt.ylabel('op/sec')
plt.xticks(threads)
plt.legend()
plt.tight_layout()
plt.savefig(Path(__file__).parent / 'results/plot.png', dpi=150)
