package lab2;

/* Этап 2: лок, о котором знает планировщик. Поток, ждущий занятый лок, не попадает в enabled;
 * обычный synchronized / ReentrantLock здесь нельзя — поток уснёт с ходом в руках и всё зависнет.
 * Лог: "lock <имя>", "unlock <имя>". Планировщик должен знать все локи, чтобы печатать held={a:A, b:B}. */
public final class SchedLock {
    final Scheduler scheduler;
    final String name;
    Scheduler.SchedulerThread owner;

    /** Создаётся в setup() тестовой программы; зарегистрируйте лок в планировщике s. */
    public SchedLock(Scheduler s, String name) {
        this.scheduler = s;
        this.name = name;
        s.locks.add(this);
    }

    /** Пауза; если лок занят — ждать, не участвуя в выборе; затем стать владельцем. */
    public void lock() {
        Scheduler.SchedulerThread thread = (Scheduler.SchedulerThread) Thread.currentThread();

        thread.condition = () -> owner == null;
        scheduler.pause(thread);
        thread.condition = null;

        owner = thread;

        Scheduler.log("lock " + name);
    }

    /** Пауза; освободить лок, ждавшие потоки снова могут быть выбраны. */
    public void unlock() {
        Scheduler.SchedulerThread thread = (Scheduler.SchedulerThread) Thread.currentThread();

        scheduler.pause(thread);
        owner = null;

        Scheduler.log("unlock " + name);
    }
}
