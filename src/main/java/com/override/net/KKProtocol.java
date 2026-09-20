package com.override.net;

/**
 * The line format the game and the KK console speak over {@link RelayLink}.
 *
 * <p>The game machine simulates everything and is the authority; the console
 * only receives positions and sends intents. Nothing waits for a round trip,
 * so a player in another city on a 200 ms link still plays fine — her blackout
 * simply lands a moment after she clicks it.
 */
public final class KKProtocol {

    public static final String ROLE_GAME = "GAME";
    public static final String ROLE_KK = "KK";

    /** game to console, ~12x a second. */
    public static final String TICK = "TICK";
    /** game to console, one-off moments worth reacting to. */
    public static final String EVENT = "EVENT";
    /** console to game: an intent KK is spending power on. */
    public static final String CMD = "CMD";
    /** game to console: REN answering back. */
    public static final String SAY = "SAY";

    /** Longest chat line either side will send; the rest is dropped. */
    public static final int CHAT_LIMIT = 120;

    // commands
    public static final String CMD_BLACKOUT = "BLACKOUT";
    public static final String CMD_SWEEP = "SWEEP";      // + x z : send the unit to a spot
    public static final String CMD_WAKE = "WAKE";        // put the second unit on the floor
    public static final String CMD_LOCKDOWN = "LOCKDOWN";
    public static final String CMD_TAUNT = "TAUNT";      // + text : KK speaks to the player
    public static final String CMD_DRIVE = "DRIVE";      // + dx dz : steer the unit; 0 0 lets go

    private KKProtocol() { }

    /**
     * Flattens a typed message into something safe to put on the wire.
     *
     * <p>Every message here is one line, and the reader splits on newlines, so a
     * pasted line break would arrive as two messages and the second would be
     * parsed as a command. Control characters are folded to spaces and the
     * result is capped, which also stops one player flooding the other's log.</p>
     *
     * @return the cleaned text, or {@code null} if nothing printable was left
     */
    public static String chat(String text) {
        if (text == null) return null;
        StringBuilder out = new StringBuilder(Math.min(text.length(), CHAT_LIMIT));
        for (int i = 0; i < text.length() && out.length() < CHAT_LIMIT; i++) {
            char c = text.charAt(i);
            out.append(c < ' ' || c == '\u007f' ? ' ' : c);
        }
        String cleaned = out.toString().strip();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /** One frame of the floor: positions, alert state and the run's vitals. */
    public static String tick(double px, double pz, double sx, double sz, double ex, double ez,
                              String state, boolean twoUnits, boolean hidden,
                              int hp, int credits, int tokens, int secs) {
        return TICK + " " + r(px) + " " + r(pz) + " " + r(sx) + " " + r(sz) + " " + r(ex) + " " + r(ez)
            + " " + state + " " + (twoUnits ? 1 : 0) + " " + (hidden ? 1 : 0)
            + " " + hp + " " + credits + " " + tokens + " " + secs;
    }

    /** Parsed form of a TICK line, or null when the line is malformed. */
    public static Tick parseTick(String line) {
        String[] p = line.split(" ");
        if (p.length < 14 || !TICK.equals(p[0])) return null;
        try {
            return new Tick(d(p[1]), d(p[2]), d(p[3]), d(p[4]), d(p[5]), d(p[6]), p[7],
                "1".equals(p[8]), "1".equals(p[9]),
                Integer.parseInt(p[10]), Integer.parseInt(p[11]),
                Integer.parseInt(p[12]), Integer.parseInt(p[13]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record Tick(double px, double pz, double sx, double sz, double ex, double ez,
                       String state, boolean twoUnits, boolean hidden,
                       int hp, int credits, int tokens, int secs) {}

    private static String r(double v) {
        return String.format("%.2f", v);
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }
}
