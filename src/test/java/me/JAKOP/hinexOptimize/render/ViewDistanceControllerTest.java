package me.JAKOP.hinexOptimize.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewDistanceControllerTest {
    private static final ViewDistanceController.Settings TARGET_60 = new ViewDistanceController.Settings(60, 6, 8);
    private static final int UNLIMITED = ViewDistanceController.UNLIMITED_FPS;

    /** Simulated game: feeds frames of a fixed duration and records every decision. */
    private static final class Game {
        final ViewDistanceController controller = new ViewDistanceController();
        final List<Long> decisionTimes = new ArrayList<>();
        final List<Integer> distances = new ArrayList<>();
        long now = 1;

        void run(double seconds, double frameMs, int setting, int limit, boolean mesherIdle, ViewDistanceController.Settings settings) {
            long end = now + (long) (seconds * 1e9);
            long frame = (long) (frameMs * 1e6);
            while (now < end) {
                ViewDistanceController.Decision decision = controller.onFrame(now, setting, limit, mesherIdle, settings);
                if (decision != null) {
                    decisionTimes.add(now);
                    distances.add(decision.distance());
                }
                now += frame;
            }
        }

        void run(double seconds, double frameMs) {
            run(seconds, frameMs, 24, UNLIMITED, true, TARGET_60);
        }

        int rendered(int setting) {
            return Math.min(setting, controller.cap());
        }
    }

    @Test
    void lowersOneChunkAtATimeDownToTheFloor() {
        Game game = new Game();
        game.run(200, 25); // 40 FPS against a 60 FPS target
        assertEquals(16, game.rendered(24), "floor is setting - maxReduction");
        assertEquals(List.of(23, 22, 21, 20, 19, 18, 17, 16), game.distances);
        for (int i = 1; i < game.decisionTimes.size(); i++) {
            long gap = game.decisionTimes.get(i) - game.decisionTimes.get(i - 1);
            assertTrue(gap >= ViewDistanceController.COOLDOWN_NANOS, "steps wait for the cooldown");
        }
    }

    @Test
    void neverGoesBelowMinDistance() {
        Game game = new Game();
        game.run(120, 25, 8, UNLIMITED, true, TARGET_60);
        assertEquals(6, game.rendered(8));
    }

    @Test
    void raisesBackToTheSettingWhenThereIsHeadroom() {
        Game game = new Game();
        game.run(40, 25);
        assertTrue(game.rendered(24) < 24);
        game.run(200, 5); // 200 FPS
        assertEquals(ViewDistanceController.NO_CAP, game.controller.cap());
        assertEquals(24, game.rendered(24));
    }

    @Test
    void framerateLimitBelowTargetIsNotASlowdown() {
        Game game = new Game();
        game.run(60, 1000.0 / 30, 24, 30, true, TARGET_60); // player limits the game to 30 FPS
        assertTrue(game.distances.isEmpty());
    }

    @Test
    void probesUpwardsWhenTheFrameTimeIsPinnedAtTheTarget() {
        Game game = new Game();
        game.run(20, 25);
        int lowered = game.rendered(24);
        assertTrue(lowered < 24);
        game.run(30, 1000.0 / 60); // VSync at 60 Hz: inside the band, neither slow nor fast
        assertTrue(game.rendered(24) > lowered, "a steady target frame rate lets it try one chunk more");
    }

    @Test
    void stepDownRightAfterStepUpBlocksRaisingForAMinute() {
        Game game = new Game();
        game.run(20, 25);
        game.run(30, 5); // fast: raises
        int raised = game.rendered(24);
        game.run(10, 25); // slow right after the raise: lowers again
        int afterBounce = game.rendered(24);
        assertTrue(afterBounce < raised);
        game.run(45, 5); // fast, but within the minute after the bounce
        int blocked = game.rendered(24);
        assertTrue(blocked <= afterBounce, "no raise while blocked");
        game.run(40, 5);
        assertTrue(game.rendered(24) > blocked, "raises again once the block expires");
    }

    @Test
    void changingTheSettingStartsOver() {
        Game game = new Game();
        game.run(20, 25);
        assertTrue(game.controller.cap() != ViewDistanceController.NO_CAP);
        game.run(1, 25, 20, UNLIMITED, true, TARGET_60);
        assertEquals(ViewDistanceController.NO_CAP, game.controller.cap());
    }

    @Test
    void waitsWhileTheMesherIsBusy() {
        Game game = new Game();
        game.run(60, 25, 24, UNLIMITED, false, TARGET_60);
        assertTrue(game.distances.isEmpty());
    }
}
