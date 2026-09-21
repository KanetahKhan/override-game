# OVERRIDE — *The Last Real Mind*

## 📹 [**WATCH THE PROJECT PRESENTATION VIDEO**](https://drive.google.com/drive/folders/1hi_IMawelC7ngKrzKyyRECpzILzMedRh?usp=sharing)

> <https://drive.google.com/drive/folders/1hi_IMawelC7ngKrzKyyRECpzILzMedRh?usp=sharing>

The video presents the project in full: an overview and its objectives, a
demonstration of the working game and its promised features, the implementation
and technologies used, and a breakdown of each group member's contribution.

**Repository:** <https://github.com/KanetahKhan/override-game>

---

A story-based educational game about the dark future of AI dependence, built as a
final-year visual programming project in **JavaFX**. **SDG 4 — Quality Education**
is the primary alignment.

> *Year 2048. A mega-AI named KK has become the invisible backbone of civilization.
> You are REN, a final-year CSE student. Today, for the first time in years, you
> are about to think for yourself.*

---

## What the project does

| System | Status | Where |
|---|---|---|
| Name-only login, per-player saves | ✅ | `shared/ui/LoginScreen.java`, `shared/service/PlayerProfiles.java` |
| Scoreboard + top-five leaderboard | ✅ | `shared/ui/ScoreboardScreen.java`, `shared/service/ScoreArchive.java` |
| Main menu, coin shop, mock bKash payment | ✅ | `MainMenuScreen`, `ShopScreen`, `BkashMockService` |
| Cinematic intro (typewriter dialogue) | ✅ | `IntroStoryScreen.java` |
| Chapter map with progression | ✅ | `ChapterMapScreen.java` |
| **Chapter 1 — Curfew Protocol** (3D stealth) | ✅ playable | `chapter1/CurfewProtocolScreen.java`, `CurfewWorld.java` |
| **Chapter 2 — Harvest Protocol** (endless runner) | ✅ playable | `chapter2-godot/`, `chapter2/` |
| **Two-player online co-op** (KK vs REN) | ✅ | `net/`, `chapter1/KKConsoleScreen.java` |
| Mini-games (Kernel Panic, Syntax Snake, Circuit Breaker) | ✅ | `game/minigames/` |
| Procedural sound effects and music | ✅ | `game/minigames/ChiptuneSfx.java`, `ChiptuneAmbience.java` |
| Dependency Meter + Independent XP | ✅ | `shared/model/GameState.java` |
| Save / load per player profile | ✅ | `shared/service/SaveService.java` |
| Spring Boot backend (JWT, saves, leaderboard) | ✅ | `backend/` |

---

## Quick start — running the game

**Prerequisites**

| Tool | Version | Notes |
|---|---|---|
| JDK | **26** | <https://adoptium.net> |
| Maven | **3.9+** | <https://maven.apache.org/download.cgi> |

JavaFX is downloaded automatically by Maven — no separate SDK needed.

**Run it**

```bash
git clone https://github.com/KanetahKhan/override-game.git
cd override-game
mvn javafx:run
```

That is the whole setup. The game opens full-screen on the login screen; type any
name to create or resume a profile.

> **Full setup, troubleshooting and the Godot toolchain:** see [`setup.md`](setup.md).

### Useful flags

```bash
mvn javafx:run -Doverride.unlockAllChapters=true   # open the whole chapter map for testing
mvn javafx:run -Dprism.order=sw                    # software rendering, if the GPU misbehaves
mvn javafx:run -Doverride.mouseLock=false          # drag-to-look instead of captured mouse
```

> On **PowerShell**, quote any `-D` flag that contains `=`:
> `mvn javafx:run "-Doverride.unlockAllChapters=true"`

### Chapter 1 controls

| Key | Action |
|---|---|
| `W` `A` `S` `D` | Move |
| Mouse / arrow keys | Look around |
| `Shift` | Sprint (drains stamina) |
| `C` or `Ctrl` | Crouch — harder to spot, slower |
| `Space` | Jump |
| `E` | Use / interact / close a note |
| `F` | Enter or leave a hiding spot |
| `G` | Throw a book to make noise elsewhere |
| `Q` | KK Assist scan — costs dependency |
| `Esc` | Pause, and leave full-screen |

---

## Two-player online co-op

Chapter 1 can be played by **two people on different computers, in different
cities**. It is asymmetric:

- **REN** plays the normal Chapter 1 stealth run.
- **KK** gets a console showing REN's live position and spends power against her.

| KK's command | Effect | Cooldown |
|---|---|---|
| CUT THE POWER | Blacks out the floor for 20s | 35 |
| SWEEP HER ROOM | Sends the unit to REN's exact position | 20 |
| WAKE SECOND UNIT | Puts a second hunter on the floor | 45 |
| SEAL THE FLOOR | Lockdown — 60s to reach the exit | 60 |
| SPEAK | Sends a typed message to REN's screen | 5 |

KK can also **click the floor map** to sweep the unit anywhere, or **steer the
unit directly with `WASD`**. Releasing the keys hands it back to its own AI.
REN replies from her pause screen (`Esc` → type → `Enter`).

### How the networking works

Both players **dial out** to a small relay server. Neither router needs port
forwarding, which matters because most home ISPs use CGNAT where accepting an
incoming connection is impossible. One machine hosts the relay; both connect to it.

### Step 1 — start the relay (host only, one person)

In its own terminal, left open for the whole session:

```bash
cd override-game
java -cp target/classes com.override.net.OverrideRelay 5001
```

It prints `[relay] listening on port 5001` and then logs every join, so you can
see both players arrive and with which role.

> Run `mvn compile` first if `target/classes` does not exist yet.
> There is also a **RUN THE RELAY HERE** button in the game's co-op menu, but the
> standalone command above is better while testing: it survives game restarts and
> shows the join log.

### Step 2 — put both machines on the same network

On the same Wi-Fi, the host's LAN address is enough. **For different cities, use
[Tailscale](https://tailscale.com/download)** — a free private network:

1. Both players install Tailscale and **sign in** (`tailscale up` if the tray app
   does not prompt).
2. Either sign in to the **same account**, or share the host machine from
   <https://login.tailscale.com/admin/machines> → `...` → **Share**.
3. The host finds their address:

```powershell
& "C:\Program Files\Tailscale\tailscale.exe" ip -4
```

That prints a `100.x.x.x` address — this is the RELAY address both players type.

**Verify before launching the game.** On the *joining* player's machine:

```powershell
Test-NetConnection 100.x.x.x -Port 5001
```

| Result | Meaning |
|---|---|
| `TcpTestSucceeded : True` | Network is fine — go play |
| `TcpTestSucceeded : False`, `InterfaceAlias : Wi-Fi` | Not routing through Tailscale — sign in / accept the share |
| `Connection refused` | Network fine, but the relay is not running — do Step 1 |

A helper script does all of these checks at once, with no arguments to edit:

```powershell
cd override-game
.\scripts\coop-check.ps1
```

### Step 3 — connect in game

Both players: main menu → **06 KK CO-OP** → fill in the **same** three values:

| Field | Value |
|---|---|
| RELAY | the host's address (`100.x.x.x`, or `127.0.0.1` for the host themselves) |
| PORT | `5001` |
| ROOM | any shared word, e.g. `IUT` |

Then **one player picks `PLAY AS REN`, the other picks `PLAY AS KK`.** Never both
the same — two consoles with no player is the most common mistake, and it looks
exactly like a broken connection.

REN should start the run; the KK console stays at `UNIT OFFLINE` until REN is
actually moving on the floor.

---

## Spring Boot backend (optional)

The game is fully playable without it — profiles and scores are stored locally in
`~/.override/players/`. The backend adds accounts, cloud saves and a shared
leaderboard.

```bash
cd backend
mvn spring-boot:run      # starts http://localhost:8080
```

It uses an embedded H2 database with zero setup; the console is at
`http://localhost:8080/h2`.

### REST API

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register (username, email, password) |
| POST | `/api/auth/login` | No | Login, returns a JWT |
| GET | `/api/player/me` | Yes | Player profile and stats |
| PUT | `/api/player/update` | Yes | Update profile / stats |
| POST | `/api/save` | Yes | Save game state |
| GET | `/api/save` | Yes | Load saves for the current user |
| POST | `/api/chapter{1..5}/progress/update` | Yes | Per-chapter progress |
| GET | `/api/leaderboard` | No | Top 20 leaderboard |
| GET | `/api/highscore/{gameType}` | No | Best mini-game run |
| POST | `/api/highscore` | No | Submit a mini-game run |

> **Secrets:** the JWT key and DB credentials come from environment variables
> (`APP_JWT_SECRET`, `MYSQL_*`) with dev-only defaults for local runs. Copy
> `.env.example` to `.env` for any shared deployment.

---

## Running the tests

```bash
bash scripts/test-player-pages.sh
```

Compiles the project and runs three checks: player storage and profile isolation,
the login/scoreboard pages rendered as real screenshots, and the opening-menu
smoke test. Works from Git Bash on Windows and from a shell on Linux/macOS.

---

## Technologies used

| Area | Technology |
|---|---|
| Language | Java 26 |
| UI and 3D | JavaFX 26 (`Canvas`, `AnimationTimer`, JavaFX 3D `Group`/`PerspectiveCamera`) |
| Build | Maven 3.9 |
| Chapter 2 | Godot 4 (exported executable, launched from the JavaFX app) |
| Backend | Spring Boot 3.4, Spring Security, Spring Data JPA |
| Database | H2 (development), MySQL (production) |
| Auth | JWT (jjwt) |
| Networking | Plain TCP sockets (`java.net`), custom line protocol |
| Audio | `javax.sound.sampled` (procedurally synthesised), JavaFX Media |
| Persistence | `java.util.Properties`, per-profile directories |

---

## Team contributions

Derived from the repository's commit history (183 commits from Kanetah, 72 from Jeba).

### Kanetah Khan — [@KanetahKhan](https://github.com/KanetahKhan)

- **Chapter 1 — Curfew Protocol**: the entire 3D stealth chapter, ported to JavaFX
  3D — level geometry, the sentinel AI (patrol / search / chase, suspicion, hiding,
  line of sight), lockdown, blackout, EMP, grading and scoring.
- **Two-player co-op**: the relay server, the line protocol, the KK console, live
  telemetry, KK's commands, WASD steering and in-game chat.
- **Spring Boot backend**: JWT auth, saves, chapter progress, leaderboard, REST API.
- **Mini-games**: Kernel Panic, Syntax Snake, Circuit Breaker and the mini-game
  framework.
- **Audio**: the procedural chiptune sound engine, Chapter 1's footsteps, tension
  layer and room tone.

### Jeba Shajida — [@detectivepanda40307](https://github.com/KanetahKhan/override-game/commits?author=detectivepanda40307)

- **Chapter 2 — Harvest Protocol**: the whole Godot endless runner, its art, level
  and export, plus the JavaFX launcher, loading screen and result screen.
- **Player accounts and scores**: name-only login, per-player save isolation, the
  searchable scoreboard and the top-five leaderboard.
- **Shared UI**: the opening menu, chapter map, dialogue overlay, HUD, pixel scenes
  and the sci-fi control styling.
- **Campaign scoring**: combining Chapter 1's grade and Chapter 2's score into the
  final campaign verdict.

---

## Core mechanics

### The Dependency Meter

The central mechanic. At every help prompt the player chooses:

1. **Solve manually** → full reward, +XP, +Independent XP
2. **Buy a hint** → 20 coins, smaller reward, *no* dependency increase
3. **Ask KK** → free, smaller reward, **+10 dependency**

Endings tier off `dependency` and `independentXp`:

| Tier | Threshold | Lore |
|---|---|---|
| Full Override | dependency ≥ 70 | KK's quiet victory |
| Collapse | dependency 40–69 | mixed fall |
| Resistance | default | the human path |
| Symbiosis | dependency ≤ 15 + indepXP ≥ 50 | hardest, best ending |

### Campaign score

The final verdict is **50% Chapter 1 + 50% Chapter 2**. Chapter 1 contributes as a
grade (S=100, A=80, B=60, otherwise 40) and Chapter 2 as its run percentage. A
combined score of 50% or more ends in RESISTANCE, below it in OVERRIDDEN.

### Coins, stats and the shop

Coins are earned from chapters, puzzles and stealth, and spent on persona unlocks
and hints. Five stats — Logic, Awareness, Willpower, Combat, Empathy — are buffed
by solving things independently, clean stealth runs, refusing KK, boss fights and
dialogue choices. Coins can be topped up through a **simulated** bKash dialog.

> ⚠ The bKash flow is a **simulation only**. No real money moves.

---

## Project structure

```
override-game/
├── pom.xml                       ← JavaFX frontend build
├── setup.md                      ← full setup and troubleshooting
├── scripts/
│   ├── test-player-pages.sh      ← the test suite
│   └── coop-check.ps1            ← co-op network diagnostics
├── docs/                         ← project brief, player profiles, previews
├── src/main/java/com/override/
│   ├── Main.java                 ← entry point + scene manager
│   ├── shared/
│   │   ├── model/                ← Player, GameCharacter, GameState
│   │   ├── service/              ← PlayerProfiles, SaveService, ScoreArchive
│   │   └── ui/                   ← login, menus, scoreboard, dialogue, HUD
│   ├── chapter1/                 ← Curfew Protocol: 3D world, AI, KK console
│   ├── chapter2/                 ← Harvest Protocol launcher and results
│   ├── game/minigames/           ← mini-games, audio engine
│   └── net/                      ← relay server, link, co-op protocol
├── chapter2-godot/               ← Godot 4 endless runner
└── backend/                      ← Spring Boot backend
```

---

## What this game is trying to say

> The villain is not just KK. The real villain is unchecked dependence.

Every room, chapter and dialogue is built around one idea: **convenience can become
control, and a society that stops thinking eventually loses the ability to choose.**
The Dependency Meter exists so the player *feels* this in the gameplay loop rather
than reading it in a cutscene. That is the educational payload; everything else is
scaffolding.
