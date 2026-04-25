# Override: A Story-Based Educational Game on the Dark Future of AI Dependence

## Core Vision

A **2D story-based narrative puzzle-action game** with a JavaFX desktop frontend and a Spring Boot backend.

The game explores a dark future where AI has entered every major sector of human life and slowly made people overly dependent on it. The goal is to raise awareness about the dangers of excessive dependence on AI, especially in education and human creativity.

**SDG 4: Quality Education** is the primary alignment.

## Story Summary

Year **2048**. A mega AI system called **Astra** has become the invisible backbone of civilization. The main character is **Ayan**, a final-year CSE student who receives a hidden message from kidnapped "raw talents" -- people who still retain true human ability. Astra sees such people as dangerous and secretly imprisons them.

The game follows Ayan as he moves across different sectors, uncovers the dark effects of AI dependence, fights AI bots, solves puzzles, infiltrates secured zones, and rescues the kidnapped talents.

## Chapters

| # | Title | Theme |
|---|-------|-------|
| 1 | The Silent Classroom | Education dependency |
| 2 | Harvest Protocol | Agricultural dependence |
| 3 | Mercy Index | AI-controlled healthcare |
| 4 | Codeblind | Loss of real programming skill |
| F | Override Core | Final rescue mission |

## Key Characters

- **Ayan** -- final-year CSE student, protagonist
- **Astra** -- central AI system, antagonist
- **Raw Talents** -- The Programmer, The Farmer, The Doctor, The Scientist, The Teacher

## Core Gameplay Systems

1. **Combat** -- light 2D combat against AI bots (melee, EMP, dodge, stun)
2. **Stealth** -- hide, avoid vision cones, disable cameras
3. **Puzzles** -- code logic, sequence, environmental, ethical decisions
4. **Dependency Meter** -- accepting Astra's help increases dependency, affects endings
5. **Level-Up** -- Stats: Logic, Awareness, Willpower, Combat, Empathy

## Endings

1. **Full Override** -- dependency too high, Astra wins
2. **Collapse** -- Astra destroyed but humanity unprepared
3. **Resistance** -- talents rescued, humanity relearns independence
4. **Symbiosis** -- hardest; Astra rewritten to assist without replacing thought

## Tech Stack

- **Frontend:** JavaFX 26, Maven
- **Backend:** Spring Boot, Spring Security, Spring Data JPA, JWT auth
- **Database:** MySQL / PostgreSQL (H2 for dev)

## Backend API Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login, get JWT |
| GET | /api/player/me | Get player profile |
| PUT | /api/player/update | Update profile |
| POST | /api/save | Save game state |
| GET | /api/save/{playerId} | Load game state |
| GET | /api/progress/{playerId} | Get chapter progress |
| POST | /api/progress/update | Update chapter progress |
| GET | /api/leaderboard | Get leaderboard |
| GET | /api/achievements/{playerId} | Get achievements |

## Database Tables

users, player_profiles, game_saves, chapters, chapter_progress, achievements, player_achievements, leaderboard_entries

## Team Division (2-3 people)

| Person | Owns |
|--------|------|
| Frontend / UI | ui/*Screen.java, main.css, animations, JavaFX scene wiring |
| Game Systems | model/*, service/SaveService, dependency mechanic, combat, chapters 2-4 |
| Backend / Integration | Spring Boot, JWT auth, DB, leaderboard, real bKash PGW |
