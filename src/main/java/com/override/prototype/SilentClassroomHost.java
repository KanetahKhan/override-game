package com.override.prototype;

import com.override.game.minigames.MiniGameResult;

import java.util.function.Consumer;

/**
 * JavaFX host callbacks used by the embedded FXGL chapter.
 *
 * <p>The hallway owns real-time gameplay. The host owns modal UI, campaign
 * state, and scene changes so the FXGL engine never reaches into the global
 * JavaFX scene manager directly.</p>
 */
public interface SilentClassroomHost {

    /** Show the Professor introduction, then invoke {@code onBeginMission}. */
    void showTutorial(Runnable onBeginMission);

    /** Launch a modal mini-game and always report a result, including cancel. */
    void launchMiniGame(String gameId, Consumer<MiniGameResult> onResult);

    /** Apply assist costs and bank any first-clear rewards for chapter exit. */
    void onMiniGameResolved(
            String gameId,
            MiniGameResult result,
            boolean firstClear
    );

    /** Show the Room 306 patrol archive once, then resume the hallway. */
    void showPatrolClue(Runnable onClose);

    /** Show pause controls. Exactly one callback must eventually be invoked. */
    void showPauseMenu(Runnable onResume, Runnable onQuit);

    /** Commit the finished run, shut down FXGL, and leave Chapter 1. */
    void finishChapter(SilentClassroomResult result);
}
