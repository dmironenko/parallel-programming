package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/* Стратегии выбора следующего потока.
 * FirstAlive — этап 1, Replay — этап 2, RandomPick — этап 3, Dfs — этап 4 (ограничение bound — этап 5).
 * enabled — номера потоков, которые могут сделать шаг, по возрастанию; last — прошлый выбранный (-1 в начале). */
public final class Strategies {
    private Strategies() { }

    /** Всегда первый поток из enabled. */
    public static final class FirstAlive implements Strategy {
        public int pick(List<Integer> enabled, int last) {
            return enabled.get(0);
        }
    }

    /** Проигрывает трассу: на i-м решении выбирает поток trace[i] ('A' -> 0, 'B' -> 1). */
    public static final class Replay implements Strategy {
        private final String trace;
        private int traceIndex;

        public Replay(String trace) {
            this.trace = trace;
        }

        /** Сбросить позицию в трассе перед прогоном. */
        public void beginRun() {
            traceIndex = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            if (traceIndex == trace.length()) {
                throw new IllegalArgumentException("invalid trace");
            }

            int next = trace.charAt(traceIndex) - 'A';
            if (!enabled.contains(next)) {
                throw new IllegalArgumentException("invalid trace");
            }

            traceIndex++;
            return next;
        }
    }

    /** Случайный поток: enabled.get(rnd.nextInt(enabled.size())), rnd = new java.util.Random(seed). */
    public static final class RandomPick implements Strategy {
        private final Random rnd;

        public RandomPick(long seed) {
            rnd = new Random(seed);
        }

        public int pick(List<Integer> enabled, int last) {
            return enabled.get(rnd.nextInt(enabled.size()));
        }
    }

    /** Stateless DFS по дереву выборов: каждый прогон с нуля, повторяет префикс прошлого и берёт следующий вариант.
     *  bound — максимум вытеснений (этап 5); для dfs runner передаёт Integer.MAX_VALUE. */
    public static final class Dfs implements Strategy {
        private final List<List<Integer>> path = new ArrayList<>();
        private final int bound;
        private int currentStep;
        private int switchCount;

        public Dfs(int bound) {
            this.bound = bound;
        }

        /** Подготовиться к новому прогону (позиция в пути, счётчик вытеснений). */
        public void beginRun() {
            currentStep = 0;
            switchCount = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            boolean isPreviousThreadEnabled = enabled.contains(last);

            if (currentStep == path.size()) {
                path.add(new ArrayList<>(
                    isPreviousThreadEnabled && switchCount >= bound
                    ? List.of(last)
                    : enabled
                ));
            }

            int nextThread = path.get(currentStep).getFirst();
            if (isPreviousThreadEnabled && nextThread != last) {
                switchCount++;
            }

            currentStep++;
            return nextThread;
        }

        /** Выбрать следующий путь; false — дерево обойдено. */
        public boolean endRun() {
            while (!path.isEmpty()) {
                List<Integer> availableThreads = path.getLast();
                availableThreads.removeFirst();
                if (!availableThreads.isEmpty()) {
                    return true;
                }

                path.removeLast();
            }

            return false;
        }
    }
}
