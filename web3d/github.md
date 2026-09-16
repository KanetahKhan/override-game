repo: KanetahKhan/override-game
branch: master

## Last sync
date: 2026-08-02T08:49:53Z

### Updated in this project
- Rebuilt Chapter 1 "The Silent Classroom" as a 3D slice: top-down corridor + first-person rooms.
- Palette, HUD and typography lifted from src/main/resources/styles/main.css and shared/ui/UIFactory.java.
- Enforcer-unit patrol/vision-cone behaviour ported from chapter4/ServerStealthScreen.java (cone length + half-angle, reset-to-entrance on detection, dependency penalty).
- Seven rooms, three hidden glitch tokens, mission clock, hide-under-furniture.
- Imported the real glitch-token sprite sheet for the HUD slots and the recovery modal.

## Screen map
| Screen | Built from |
| --- | --- |
| OVERRIDE 3D — Chapter One.dc.html | chapter1/ChapterOneScreen.java (room hub, lore), shared/ui/UIFactory.java (HUD), shared/model/GameState.java (dependency/coins), styles/main.css (palette) |
| override3d.js | chapter4/ServerStealthScreen.java (patrol + cone detection maths) |
| art/glitch-token.png | src/main/resources/individual_glitch_token-removebg-preview.png |

## Sync history
- 2026-08-02 — earlier turn read Derek-Scheuerman/Escape (Unreal FP template) as a 3D reference only; its inventory thumbnails live in items/.
