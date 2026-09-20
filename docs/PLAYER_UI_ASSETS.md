# Player pages and supplied controls

`src/main/resources/ui/scifi-controls.jpg` is the unchanged JPEG supplied in
`game-buttons-frames-sci-fi-style-design-elements-menu-assets-user-interface-vector-ca.zip`
(`90Z_2109.w032.n003.95B.p1.95.jpg`). No asset ownership or new license is claimed.
Retain the source pack's original distribution terms when distributing the game.

`GameControls` selects the blank third button through a JavaFX ImageView viewport.
Its runtime color adjustment changes the bright blue frame to subdued dark metal
and cyan, clips the corners, and adds hover/focus/pressed states. All captions
remain live JavaFX text with the game's existing monospaced typography.

The jungle pack's vines and fantasy stone do not match this classroom's hardware.
The neon stone pack was reviewed as a secondary reference; it is not embedded.
The UnityMenuSystem archive contains C# scripts, not visual assets. The relevant
play/quit, pause/resume, volume and fullscreen functions are provided by the
existing JavaFX menu and game screens; Unity scripts are not executable in this
Java project. The new login, switch-player, scoreboard, search and history pages
use the same themed controls.
