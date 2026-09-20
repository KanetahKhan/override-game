package com.override.net;

/**
 * Where the relay lives and which room to join, chosen on the title screen and
 * read when Chapter 1 starts. Session-scoped: co-op is opt-in per launch.
 */
public final class CoopConfig {

    private static String host = "127.0.0.1";
    private static int port = OverrideRelay.DEFAULT_PORT;
    private static String room = "IUT";
    private static boolean linked;

    private CoopConfig() { }

    public static void set(String relayHost, int relayPort, String roomCode) {
        host = relayHost == null || relayHost.isBlank() ? "127.0.0.1" : relayHost.trim();
        port = relayPort > 0 ? relayPort : OverrideRelay.DEFAULT_PORT;
        room = roomCode == null || roomCode.isBlank() ? "IUT" : roomCode.trim();
    }

    /** True when the next Chapter 1 run should stream itself to an KK console. */
    public static boolean isLinked() { return linked; }

    public static void setLinked(boolean value) { linked = value; }

    public static String host() { return host; }

    public static int port() { return port; }

    public static String room() { return room; }
}
