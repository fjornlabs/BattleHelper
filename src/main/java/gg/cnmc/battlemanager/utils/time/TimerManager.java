package gg.cnmc.battlemanager.utils.time;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TimerManager {

    private static final List<GameTimer> timers = new ArrayList<>();
    private static final List<GameTimer> pendingAdd = new ArrayList<>();
    private static final List<Runnable> pendingActions = new ArrayList<>();

    // -------------------------
    // SAFE API
    // -------------------------

    public static void addTimer(GameTimer timer) {
        pendingAdd.add(timer);
    }

    /**
     * Use this to safely run logic AFTER tick iteration
     */
    public static void runLater(Runnable action) {
        pendingActions.add(action);
    }

    public static void clearAll() {
        timers.clear();
        pendingAdd.clear();
        pendingActions.clear();
    }

    public static void removeTimer(GameTimer timer) {
        timers.remove(timer);
        pendingAdd.remove(timer);
    }

    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    public static int minutesToTicks(int minutes) {
        return minutes * 60 * 20;
    }

    // -------------------------
    // MAIN TICK (SAFE)
    // -------------------------

    public static void tick() {

        // 1. flush additions
        if (!pendingAdd.isEmpty()) {
            timers.addAll(pendingAdd);
            pendingAdd.clear();
        }

        List<GameTimer> finished = new ArrayList<>();

        // 2. tick only (NO CALLBACK EXECUTION HERE)
        Iterator<GameTimer> it = timers.iterator();

        while (it.hasNext()) {
            GameTimer timer = it.next();

            timer.tick();

            if (timer.isFinished()) {
                it.remove();
                finished.add(timer);
            }
        }

        // 3. execute callbacks AFTER iteration COMPLETELY DONE
        for (GameTimer timer : finished) {
            pendingActions.add(timer::run);
        }

        // 4. run queued actions safely
        if (!pendingActions.isEmpty()) {
            List<Runnable> copy = new ArrayList<>(pendingActions);
            pendingActions.clear();

            for (Runnable r : copy) {
                r.run();
            }
        }
    }
}