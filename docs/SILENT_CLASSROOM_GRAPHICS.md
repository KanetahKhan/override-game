# Silent Classroom graphics refresh

The active Chapter 1 entry point is `CurfewProtocolScreen`, launched by
`ChapterMapScreen`. It uses the real JavaFX 3D scene in `CurfewWorld` and the
three minigames in `CurfewNodeGames`. The older FXGL classroom prototype is
not the campaign entry point and is not changed by this refresh.

## Visual and interaction changes

- Warmer, shared wood-grain material, two-tone walls with physical trim, and
  individually shaded floor tiles. Increased ambient light makes furniture
  readable while retaining the curfew atmosphere.
- Classroom chairs have supporting legs; desks have physical notebooks.
  Terminals have raised keyboards, stands, ventilation slots and power lights.
  Server racks have individual blades instead of a solid glowing rectangle.
- Teacher desks have hollow pedestals and sliding drawer trays. Credits vanish
  when collected and are still awarded only once per drawer.
- Three lab workstations can be switched on/off with E or left click. Clearing
  Kernel Panic changes powered displays to KERNEL RESTORED. A powered-off
  display stays off until the player switches it on again.
- The crosshair turns amber over usable objects. Walls now occlude interaction
  rays, preventing selection of furniture through a room divider.
- All three minigames share a readable terminal frame. Kernel Panic has marked
  data lanes, a patch progress strip, and a patch band matching its hit window.
  Falling tokens no longer intercept lane clicks. Circuit Breaker uses copper
  traces with live connected paths shown in mint. Silent Code has line numbers,
  syntax colours, a selection indicator, and a verified completion state.

These are procedural 3D models with depth, perspective, lighting, picking and
existing gameplay interactions. This change does not replace them with
artist-authored models or introduce a physics engine.

## Run

Requires the project's existing JDK 26 / Maven setup. The three missing Chapter 2
classes listed below must also be restored before the full game can compile:

```sh
git fetch origin
git switch graphics/silent-classroom-upgrade
mvn javafx:run
```

Open Chapter 1 from the chapter map. For drag-to-look instead of pointer capture,
set the JVM property `-Doverride.mouseLock=false` in the launcher/IDE.

## Validation

Local validation: Java compiler parser accepted all three changed Java files;
source-extracted circuit logic passed 1,000 seeded generated-board checks for
solvability, a powered receiver, and no power from a disconnected source;
`git diff --check` passed. These checks are not a full frontend compilation.

The editing environment does not have Maven/JavaFX/JDK 26, and dependency
endpoints timed out. The JDK 26 PR build ran on GitHub and failed on existing
missing classes referenced by the unchanged `ChapterMapScreen`:

- `com.override.game.minigames.GodotGameLauncher`
- `com.override.chapter2.ChapterTwoResultScreen`
- `com.override.chapter2.ChapterTwoTutorialScreen`

Build log: https://github.com/KanetahKhan/override-game/actions/runs/35069924951

These files are absent from the base commit, too. The compiler reported six
errors, all in the unchanged chapter map. This is not a successful full build.
Restore the actual Chapter 2 sources, then rerun the build. In-game visual QA
and gameplay checks still need a machine with JavaFX 3D rendering support:

1. Inspect all six rooms and the three node panels at 1280 × 720.
2. Sit/stand, open/close both almirah doors, hide, and collect books.
3. Open each drawer twice: credits disappear and are awarded once.
4. Toggle a lab display off, clear Kernel Panic, then power it on: restored text
   appears; other powered displays update immediately.
5. Check that the crosshair selects a terminal through its doorway, but not
   through the adjacent wall.
6. Complete each minigame and disconnect/reopen it; verify rewards, controls,
   win conditions, pause handling, and the exit still work.
7. Check frame rate on the target laptop; added geometry uses the existing
   lighting system without per-prop lights or per-frame texture generation.
