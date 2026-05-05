package gg.cnmc.battlemanager.utils.time;

public class GameTimer {

    private int ticks;
    private final Runnable onFinish;
    private boolean finished = false;

    public GameTimer(int ticks, Runnable onFinish) {
        this.ticks = ticks;
        this.onFinish = onFinish;
    }

    public void tick() {
        if (finished) return;

        if (--ticks <= 0) {
            finished = true;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void run() {
        // ONLY execute logic, NEVER modify TimerManager directly here
        onFinish.run();
    }
}