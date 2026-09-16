# Pixel-game title screen and classroom film

This presentation follows the supplied OVERRIDE slides: year 2556, KK,
dependence replacing independent thought, suppressed questions, a patrolled
classroom floor, and the professor's manual fragment. It deliberately does
not use the rejected generated background image.

## Player flow

- Main menu: New Game, Continue (requires a save), Personas / Shop, Display
  Options, Quit. Up/Down and Tab navigate; Enter activates the focused control.
- New Game still opens the chapter map and confirms before resetting a run
  when a save exists. It does not delete the save file.
- Only selecting **Chapter 1 / Curfew Protocol** opens the 40-second
  **Silent Classroom** story film. Both completing and skipping the film lead
  into the existing CurfewProtocolScreen. No progress or rewards are mutated.
- Chapter 2 keeps the latest Godot launcher routing.

## Art and animation

The set is authored in JavaFX Canvas on a crisp pixel grid: tiled floor,
wood-grain desks, monitors, books, chairs, rainy windows, a surveillance camera,
and restrained screen lighting. It uses the existing, unchanged Ayan idle and
walking PNGs; it does not generate or redraw the player character.

Five captioned story beats cover KK's promise, the crash, enforced silence,
the handwritten fragment, and the player's choice. Space pauses/resumes;
Enter or the visible button skips. Reduced Motion makes the room static while
retaining captions and controls. The setting is session-only and applies to
this presentation, not to gameplay effects. Animation pauses on focus loss
and stops when the screen is detached. There is no strobe or narration/audio.

The game plays the animation directly, so it needs no MP4 codec, network call,
or JavaFX Media dependency. The preview workflow also records the same renderer
to a silent 1280x720, 24fps MP4 for reviewing/sharing.

## Run and validate

With the project's JDK 26 / Maven setup:

```sh
git fetch origin
git switch presentation/ai-horror-opening
mvn javafx:run
```

This branch includes the latest master updates through `922add5`, including
the restored Godot launcher and the Chapter 1 gameplay/mesh updates. The
check compiles the full frontend and test sources, then runs the actual
presentation components:

```sh
# Linux with a display, or use xvfb-run -a in a headless environment
bash scripts/test-opening.sh --video
```

Requires ffmpeg for `--video`; omit the flag for controls/screenshots only.
Outputs are in `target/opening-preview/`. The GitHub PR workflow runs the same
check and uploads the real menu, display options, five story shots, and MP4.

The smoke test exercises save gating, menu callbacks, display settings,
pause/resume, exactly-once completion, and reduced-motion pixel stability.
It does not constitute a full gameplay playthrough or launch the external
Godot game.
