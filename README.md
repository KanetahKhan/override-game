# OVERRIDE — *The Last Real Mind*

A JavaFX prototype for a story-based educational game on the dark future of AI dependence. Built as a final-year visual programming project. **SDG 4 — Quality Education** is the primary alignment.

> *Year 2048. A mega-AI named Astra has become the invisible backbone of civilization. You are Ayan, a final-year CSE student. Today, for the first time in years, you are about to think for yourself.*

---

## What's in the prototype

This v0.1 build ships a **playable end-to-end vertical slice** of Chapter 1 plus all the menu / metagame systems wired up:

| System | Status | File(s) |
|---|---|---|
| Main menu (New / Continue / Shop / Quit) | ✅ | `MainMenuScreen.java` |
| Character select with locked tiles | ✅ | `CharacterSelectScreen.java` |
| Coin shop | ✅ | `ShopScreen.java` |
| **Mock bKash payment flow** | ✅ | `BkashMockService.java` |
| Cinematic intro (typewriter dialogue) | ✅ | `IntroStoryScreen.java` |
| Chapter map with progression | ✅ | `ChapterMapScreen.java` |
| **Chapter 1 — The Silent Classroom** | ✅ playable | `ChapterOneScreen.java` |
| Reusable dialogue overlay with choices | ✅ | `DialogueOverlay.java` |
| Chapter 1 mini-games (rebuilding on the new framework) | 🟡 in progress | Kernel Panic done; more to follow |
| Dependency Meter + Independent XP | ✅ | `GameState.java` |
| Astra Assist (EMP) with a dependency cost | ✅ | inside `KernelPanicGame.java` |
| Save / Load (Properties file) | ✅ | `SaveService.java` |
| Chapter ending screen | ✅ | `EndingScreen.java` |
| HUD (HP / Dependency / Coins) | ✅ | `UIFactory.hud()` |
| Chapters 2 – 5 + Final Mission | ✅ wired up | `chapter2/` … `chapter5/` |
| **Kernel Panic** reflex mini-game (Chapter 1) | ✅ playable | `game/minigames/KernelPanicGame.java` |
| Mini-game framework (Canvas + AnimationTimer) | ✅ | `game/minigames/MiniGame.java` |
| Persisted mini-game high scores | ✅ | `HighScore` entity + `/api/highscore` |
| Spring Boot backend | ✅ | `backend/` directory |

> The build compiles **clean** against JavaFX 26.0.1 on JDK 26.

---

## Running the game

> **Full setup lives in [`setup.md`](setup.md)** — prerequisites, install, run,
> tests, and troubleshooting. Quick version below.

**Prerequisites:** JDK 26 and Maven 3.9+ (JavaFX is downloaded by Maven; no
separate SDK needed).

```bash
git clone https://github.com/KanetahKhan/override-game.git
cd override-game
mvn javafx:run          # launches the game
```

The Spring Boot backend (optional — accounts, cloud saves, leaderboard) runs in
a separate terminal:

```bash
cd backend
mvn spring-boot:run     # starts http://localhost:8080
```

---

## Project structure

```
override-game/
├── pom.xml                          ← JavaFX frontend build
├── README.md
├── docs/
│   └── PROJECT_BRIEF.md             ← full project specification
├── src/main/                        ← JavaFX frontend
│   ├── java/com/override/
│   │   ├── Main.java                ← entry point + scene manager
│   │   ├── shared/
│   │   │   ├── model/               ← Player, GameCharacter, GameState
│   │   │   ├── service/             ← SaveService, BkashMockService
│   │   │   └── ui/                  ← menus, dialogue overlay, HUD, shared screens
│   │   ├── chapter1/                ← ChapterOne + Puzzle / Stealth / Combat screens
│   │   ├── chapter2/                ← ChapterTwo + Crop / Drone / AgroBoss screens
│   │   ├── chapter3/                ← ChapterThree + Triage / Hospital / MedBoss screens
│   │   ├── chapter4/                ← ChapterFour + Code / Server / CodeBoss screens
│   │   └── chapter5/                ← FinalMission, AstraBoss, FinalEnding
│   └── resources/
│       └── styles/main.css          ← entire dark cyber theme
└── backend/                         ← Spring Boot backend
    ├── pom.xml
    └── src/main/
        ├── java/com/override/backend/
        │   ├── OverrideBackendApplication.java
        │   ├── config/               ← SecurityConfig, GlobalExceptionHandler
        │   ├── security/             ← JwtUtils, JwtAuthFilter, AppUserDetailsService
        │   ├── dto/                  ← request/response objects
        │   ├── entity/               ← User, PlayerProfile, GameSave, ChapterProgress, Achievement, LeaderboardEntry
        │   ├── repository/           ← Spring Data JPA repositories
        │   ├── shared/
        │   │   ├── controller/       ← Auth, Player, Save, Leaderboard controllers
        │   │   └── service/          ← Auth, Player, Save, Leaderboard services
        │   └── chapter1..5/          ← Chapter{N}ProgressController + Chapter{N}ProgressService
        └── resources/
            ├── application.properties
            └── db/schema.sql
```

---

## Core mechanics

### The Dependency Meter

The single most important mechanic. At every help-prompt the player chooses one of:

1. **Solve manually** → full reward, +XP, +Independent XP, sometimes +stat
2. **Buy a hint** → 20 coins, smaller reward, *no* dependency increase
3. **Ask Astra** → free, smaller reward, **+10 dependency**

Endings tier off `dependency` and `independentXp`:

| Tier | Threshold | Lore |
|---|---|---|
| Full Override | dependency ≥ 70 | Astra's quiet victory |
| Collapse | dependency 40–69 | mixed fall |
| Resistance | default | the human path |
| Symbiosis | dependency ≤ 15 + indepXP ≥ 50 | hardest, best ending |

### Coins & character unlock

* Earned from chapter completions, puzzles, stealth, combat
* Spent on: persona unlock, hints, future cosmetics
* Topped up via the **mock bKash dialog** (3-step phone → OTP → PIN flow)

> ⚠ The bKash flow is a **simulation only**. No real money moves. Real integration requires the bKash PGW merchant credentials and a server-side webhook — see "Backend hook-up" below.

### Player stats (Logic / Awareness / Willpower / Combat / Empathy)

Mapped 1-to-1 with the design doc. Buffed by:

* completing puzzles independently (Logic)
* clean stealth runs (Awareness)
* refusing Astra (Willpower)
* boss fights (Combat)
* dialogue choices (Empathy)

---

## Chapter structure

All five chapters are implemented and wired up — `ChapterMapScreen.java` routes
each tile straight to its chapter screen (`ChapterOneScreen` … `FinalMissionScreen`).
Every chapter lives in its own package (`chapter1/` … `chapter5/`) and reuses the
shared engine pieces in `shared/ui` (`DialogueOverlay`, HUD, typewriter) plus its
own puzzle / stealth / boss screens.

To add a new chapter or a new puzzle type, model it on an existing
`chapter*/` package and add a route in `ChapterMapScreen`.

---

## Spring Boot Backend

The backend lives in the `backend/` directory. It provides user authentication (JWT), game save/load, chapter progress tracking, and a leaderboard.

### Running the backend

Open a **separate terminal** from the game (full details in [`setup.md`](setup.md#6-run)):

```bash
cd backend
mvn spring-boot:run
```

The backend starts on `http://localhost:8080` with an embedded H2 database (zero setup needed). The H2 console is available at `http://localhost:8080/h2`.

To switch to MySQL for production, edit `backend/src/main/resources/application.properties` and uncomment the MySQL section.

> **Secrets:** the JWT signing key and DB credentials are read from environment
> variables (`APP_JWT_SECRET`, `MYSQL_*`) with dev-only defaults baked in for
> local runs. For any shared/production deployment, copy `.env.example` to `.env`
> and set real values — see [`setup.md` § Environment](setup.md#3-environment).

### REST API

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register (username, email, password) |
| POST | `/api/auth/login` | No | Login, returns JWT token |
| GET | `/api/player/me` | Yes | Get player profile + stats |
| PUT | `/api/player/update` | Yes | Update profile / stats |
| POST | `/api/save` | Yes | Save game state |
| GET | `/api/save` | Yes | Load all saves for current user |
| POST | `/api/chapter{1..5}/progress/update` | Yes | Update per-chapter progress |
| GET | `/api/leaderboard` | No | Top 20 leaderboard |
| GET | `/api/highscore/{gameType}` | No | Best mini-game run (e.g. `kernel-panic`) |
| POST | `/api/highscore` | No | Submit a mini-game run (raises stored best) |

### Database

Uses H2 (file-based) for development, MySQL for production. Schema auto-created by JPA. Full SQL schema available at `backend/src/main/resources/db/schema.sql`.

**Tables:** users, player_profiles, game_saves, chapter_progress, achievements, leaderboard_entries

### Connecting the game to the backend

The JavaFX frontend currently saves locally to `~/.override/save.properties`. To connect it to the backend, update `SaveService.java` to make HTTP calls:

```java
// Example: save game to backend
String json = serializeState();
HttpRequest req = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/save"))
    .header("Authorization", "Bearer " + token)
    .header("Content-Type", "application/json")
    .POST(BodyPublishers.ofString(json))
    .build();
HttpClient.newHttpClient().send(req, BodyHandlers.discarding());
```

---

## Splitting the work

For a 2–3 person team:

| Person | Owns |
|---|---|
| **Frontend / UI** | `ui/*Screen.java`, `main.css`, animations, JavaFX scene wiring |
| **Game systems** | `model/*`, `service/SaveService`, dependency mechanic, combat balance, content for chapters 2–4 |
| **Backend / Integration** | Spring Boot project, JWT auth, real bKash PGW (sandbox), DB, leaderboard |

---

## What this game is trying to say

> The villain is not just Astra. The real villain is unchecked dependence.

Each room, each chapter, each dialogue is built around one idea: **convenience can become control, and a society that stops thinking eventually loses the ability to choose.** The Dependency Meter exists so the player *feels* this in the gameplay loop, not just reads it in a cutscene.

That's the educational payload. Everything else is scaffolding.
