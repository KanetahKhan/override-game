package com.override.chapter1;

/** Curfew Protocol difficulty: curfew length, how fast and sharp the unit is, how tight the hacks are. */
enum CurfewDifficulty {
    EASY("EASY", 11 * 60, 0.85, 0.75, 0.8, 1.3,
        "Eleven minutes, a slower unit that is slow to notice you, looser hacks."),
    NORMAL("NORMAL", 9 * 60, 1.0, 1.0, 1.0, 1.0,
        "Nine minutes. The curfew as it was written."),
    HARD("HARD", 7 * 60, 1.2, 1.35, 1.25, 0.8,
        "Seven minutes, a faster and sharper unit, tighter hacks."),
    NIGHTMARE("NIGHTMARE", 6 * 60, 1.35, 1.6, 1.4, 0.7, true,
        "Six minutes. Two units walk this floor, and both of them remember you.");

    final String label;
    final int seconds;          // curfew length
    final double speed;         // sentinel speed multiplier
    final double awareness;     // how fast its suspicion fills
    final double kernelSpeed;   // Kernel Panic token fall speed
    final double hackTime;      // Circuit Breaker / Syntax Snake time-limit multiplier
    final boolean secondUnit;   // a second hunter walks the floor
    final String blurb;

    CurfewDifficulty(String label, int seconds, double speed, double awareness,
                     double kernelSpeed, double hackTime, String blurb) {
        this(label, seconds, speed, awareness, kernelSpeed, hackTime, false, blurb);
    }

    CurfewDifficulty(String label, int seconds, double speed, double awareness,
                     double kernelSpeed, double hackTime, boolean secondUnit, String blurb) {
        this.label = label;
        this.seconds = seconds;
        this.speed = speed;
        this.awareness = awareness;
        this.kernelSpeed = kernelSpeed;
        this.hackTime = hackTime;
        this.secondUnit = secondUnit;
        this.blurb = blurb;
    }

    /** Key for this difficulty's best run in HighScoreClient and the backend's /api/highscore. */
    String gameType() {
        return "curfew-protocol-" + name().toLowerCase();
    }
}
