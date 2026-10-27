package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

/* Этап 1: планировщик, который ведёт настоящие потоки строго по одному (передача хода через семафоры).
 * Этап 2: обнаружение deadlock в главном цикле и корректное завершение застрявших потоков.
 * Внутреннее устройство (свои классы, семафоры, вспомогательные методы для Shared и SchedLock) придумываете сами. */
public final class Scheduler {
    private static final class DeadlockException extends RuntimeException {}

    private enum SchedulerThreadState {
        READY,
        RUNNING,
        WAITING,
        FINISHED
    }

    final class SchedulerThread extends Thread {
        final Scheduler scheduler = Scheduler.this;
        final int id;
        final Runnable body;
        final Semaphore threadSemaphore = new Semaphore(0);

        SchedulerThreadState state = SchedulerThreadState.READY;
        BooleanSupplier condition;

        SchedulerThread(int id, Runnable body) {
            super(String.valueOf((char) ('A' + id)));
            setDaemon(true);

            this.id = id;
            this.body = body;
        }

        @Override
        public void run() {
            try {
                body.run();
            } catch (DeadlockException e) {
            } finally {
                state = SchedulerThreadState.FINISHED;
                schedulerSemaphore.release();
            }
        }
    }

    private final List<SchedulerThread> threads = new ArrayList<>();
    final List<SchedLock> locks = new ArrayList<>();
    private final Semaphore schedulerSemaphore = new Semaphore(0);
    private final StringBuilder trace = new StringBuilder();

    /** Если true, каждая операция с общими данными добавляет строку в log. Выставляет runner. */
    boolean logging;
    /** Лог прогона: строки "<поток>  <операция>", например "A  lock mu", "A  balance -> 100". */
    final List<String> log = new ArrayList<>();
    /** null, если deadlock не было; иначе "held={a:A, b:B}" (этап 2). */
    String deadlock;

    public Scheduler() { }

    /** Зарегистрировать поток. Потоки получают буквы A, B, C… в порядке вызова spawn. */
    public void spawn(Runnable body) {
        threads.add(new SchedulerThread(threads.size(), body));
    }

    /** Запустить потоки и вести их по одному: на каждой паузе спросить strategy.pick(enabled, last). */
    public void run(Strategy strategy) {
        for (SchedulerThread thread : threads) {
            thread.start();
            schedulerSemaphore.acquireUninterruptibly();
        }

        int lastThread = -1;
        while (true) {
            List<Integer> enabledThreads = getEnabledThreads();
            if (enabledThreads.isEmpty()) {
                if (isDeadlock()) {
                    for (SchedulerThread thread : threads) {
                        if (thread.state != SchedulerThreadState.FINISHED) {
                            thread.threadSemaphore.release();
                        }
                    }
                }

                break;
            }

            lastThread = strategy.pick(enabledThreads, lastThread);
            SchedulerThread thread = threads.get(lastThread);
            trace.append(thread.getName());

            thread.threadSemaphore.release();
            schedulerSemaphore.acquireUninterruptibly();
        }

        for (SchedulerThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("scheduler interrupted", e);
            }
        }
    }

    /** Трасса прогона: буквы потоков, выбранных во всех решениях подряд, например "AAAABB". */
    public String traceString() {
        return trace.toString();
    }

    /** window() из тестовых программ: пауза перед следующей операцией с общими данными обязательна. */
    public static void point() {
        log("window");
    }

    /** Ждать, пока cond не станет true. До этого поток не попадает в enabled. */
    public static void await(BooleanSupplier cond) {
        SchedulerThread thread = (SchedulerThread) Thread.currentThread();

        thread.condition = cond;
        thread.scheduler.pause(thread);
        thread.condition = null;

        log("await done");
    }

    static void log(String string) {
        SchedulerThread thread = (SchedulerThread) Thread.currentThread();
        if (thread.scheduler.logging) {
            thread.scheduler.log.add(thread.getName() + "  " + string);
        }
    }

    void pause(SchedulerThread thread) {
        thread.state = thread.condition == null ? SchedulerThreadState.READY : SchedulerThreadState.WAITING;
        schedulerSemaphore.release();

        thread.threadSemaphore.acquireUninterruptibly();
        if (deadlock != null) {
            throw new DeadlockException();
        }

        thread.state = SchedulerThreadState.RUNNING;
    }

    private boolean isDeadlock() {
        for (SchedulerThread thread : threads) {
            if (thread.state != SchedulerThreadState.FINISHED) {
                List<String> held = new ArrayList<>();
                for (SchedLock lock : locks) {
                    if (lock.owner != null) {
                        held.add(lock.name + ":" + lock.owner.getName());
                    }
                }

                deadlock = "held={%s}".formatted(String.join(", ", held));
                return true;
            }
        }

        return false;
    }

    private List<Integer> getEnabledThreads() {
        List<Integer> enabled = new ArrayList<>();
        for (SchedulerThread thread : threads) {
            switch (thread.state) {
                case READY:
                    enabled.add(thread.id);
                    break;
                case WAITING:
                    if (thread.condition.getAsBoolean()) {
                        enabled.add(thread.id);
                    }
                    break;
                default:
                    break;
            }
        }

        return enabled;
    }
}
