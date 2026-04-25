# 3) Build output folders + copy resources
New-Item -ItemType Directory -Force -Path "target\classes" | Out-Null
Copy-Item -Recurse -Force "src\main\resources\*" "target\classes\"# OVERRIDE — *The Last Real Mind*

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
| Logic puzzle (3-way: solve / hint / Astra) | ✅ | `PuzzleScreen.java` |
| Stealth: avoid sentinel vision cone | ✅ | `StealthScreen.java` |
| Boss combat (turn-based) | ✅ | `CombatScreen.java` |
| Dependency Meter + Independent XP | ✅ | `GameState.java` |
| Hint system using coins | ✅ | inside `PuzzleScreen.java` |
| Save / Load (Properties file) | ✅ | `SaveService.java` |
| Chapter ending screen | ✅ | `EndingScreen.java` |
| HUD (HP / Dependency / Coins) | ✅ | `UIFactory.hud()` |
| Chapters 2 – 4 + Final Mission | 🟡 stubs | `ChapterMapScreen.java` |
| Spring Boot backend | 📋 specced | see "Backend hook-up" below |

> The build compiles **clean** against `javafx.controls 11+` with zero warnings. Tested against OpenJFX 11.0.11 and 21.0.2.

---

## Running the game

### Prerequisites

* **JDK 26** (project uses 26 as source/target)
* **Apache Maven 3.9+** — download the **Binary zip archive** from https://maven.apache.org/download.cgi

### Step 1 — Install JDK 26

Download and install JDK 26 from https://www.oracle.com/java/technologies/downloads/. Note the installation path (e.g. `E:\java`).

### Step 2 — Install Maven

1. Download `apache-maven-3.9.15-bin.zip` from https://maven.apache.org/download.cgi
2. Extract the zip to a folder (e.g. `E:\apache-maven-3.9.15-bin\`)
3. Add Maven's `bin` directory to your system PATH:

   **Windows (permanent) — run once in PowerShell:**
   ```powershell
   [Environment]::SetEnvironmentVariable("Path", $env:PATH + ";E:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin", "User")
   ```

   **Windows (temporary, current session only):**
   ```powershell
   $env:PATH += ";E:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin"
   ```

   > Adjust the path above to match where you extracted Maven.

4. **Open a new terminal** and verify:
   ```powershell
   mvn --version
   ```

### Step 3 — Run the game

```powershell
cd D:\override-game
mvn javafx:run
```

Maven will automatically download JavaFX dependencies on the first run (requires internet). After that, the game window should launch.

### Alternative — without Maven (Linux/macOS)

Install OpenJFX (`apt install openjfx` on Debian/Ubuntu, or download from https://openjfx.io), then:

```bash
cd override-game
JFX=/usr/share/openjfx/lib   # adjust path to your JavaFX install

# Compile
mkdir -p target/classes
cp -r src/main/resources/* target/classes/
javac --module-path "$JFX" --add-modules javafx.controls,javafx.fxml \
  -d target/classes \
  $(find src/main/java -name "*.java")

# Run
java --module-path "$JFX" --add-modules javafx.controls,javafx.fxml \
  -cp target/classes \
  com.override.Main
```

---

## Project structure

```
override-game/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/override/
    │   ├── Main.java                ← entry point + scene manager
    │   ├── model/
    │   │   ├── Player.java          ← stats + level + HP
    │   │   ├── GameCharacter.java   ← roster of selectable personas
    │   │   └── GameState.java       ← singleton: coins, dependency, progress
    │   ├── service/
    │   │   ├── SaveService.java     ← save/load to ~/.override/save.properties
    │   │   └── BkashMockService.java← simulated bKash payment dialog
    │   └── ui/
    │       ├── UIFactory.java       ← shared buttons, HUD, typewriter
    │       ├── MainMenuScreen.java
    │       ├── CharacterSelectScreen.java
    │       ├── ShopScreen.java
    │       ├── IntroStoryScreen.java
    │       ├── ChapterMapScreen.java
    │       ├── ChapterOneScreen.java← chapter 1 hub + room transitions
    │       ├── DialogueOverlay.java ← reusable typewriter dialogue
    │       ├── PuzzleScreen.java    ← Chapter 1 logic puzzle
    │       ├── StealthScreen.java   ← Chapter 1 stealth section
    │       ├── CombatScreen.java    ← turn-based boss fight
    │       └── EndingScreen.java
    └── resources/
        └── styles/main.css          ← entire dark cyber theme
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

## Adding the remaining chapters

`ChapterMapScreen.java` already routes Chapters 2–4 + the final mission to a "not implemented" dialog. To wire them up:

1. Create `ChapterTwoScreen.java`, `ChapterThreeScreen.java`, etc. modeled on `ChapterOneScreen.java`.
2. In `ChapterMapScreen.buildRow`, replace the stub `Alert` with `Main.switchScene(new ChapterTwoScreen().build())`.
3. Each chapter reuses the existing `DialogueOverlay`, `PuzzleScreen`, `StealthScreen`, `CombatScreen` — just feed them new content.

The hard part (engine pieces) is done. Chapters 2–4 are mostly content authoring + a couple of new puzzle types per chapter.

---

## Backend hook-up (Spring Boot)

The prototype saves locally to `~/.override/save.properties`. Swapping that for a Spring Boot REST backend is a 1-file change:

```java
// SaveService.java — replace save() body with:
String json = serializeState();
HttpRequest req = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/save"))
    .header("Authorization", "Bearer " + token)
    .POST(BodyPublishers.ofString(json))
    .build();
HttpClient.newHttpClient().send(req, BodyHandlers.discarding());
```

Suggested backend endpoints (matches the original spec):

```
POST  /api/auth/register
POST  /api/auth/login
GET   /api/player/me
POST  /api/save
GET   /api/save/{playerId}
GET   /api/leaderboard
GET   /api/achievements/{playerId}
POST  /api/payment/bkash/init    ← real bKash integration goes here
POST  /api/payment/bkash/execute
```

Suggested DB schema is in the original project brief and does not need to change.

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
