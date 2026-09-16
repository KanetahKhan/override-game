package com.override.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs an {@link OverrideRelay} inside the game, so one player can host the
 * room from the title screen instead of launching a second program.
 *
 * <p>The server thread is a daemon: accept() blocks forever, and that must
 * never keep the game alive after the window closes.
 */
public final class CoopRelayHost {

    private static OverrideRelay relay;

    private CoopRelayHost() { }

    /** Starts the relay once and returns a line describing where to reach it. */
    public static synchronized String start(int port) {
        if (relay != null) {
            return "Relay already running on port " + relay.port() + addresses();
        }
        OverrideRelay server = new OverrideRelay(port);
        Thread thread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException e) {
                System.err.println("[relay] could not start: " + e.getMessage());
            }
        }, "override-relay");
        thread.setDaemon(true);
        thread.start();
        relay = server;
        return "Relay listening on port " + port + addresses();
    }

    public static synchronized boolean isRunning() {
        return relay != null;
    }

    public static synchronized void stop() {
        if (relay != null) {
            relay.stop();
            relay = null;
        }
    }

    /** Every address a partner could dial, including a Tailscale one if present. */
    private static String addresses() {
        List<String> found = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) continue;
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address) found.add(address.getHostAddress());
                }
            }
        } catch (Exception e) {
            return "";
        }
        return found.isEmpty() ? "" : "  —  reachable at " + String.join(" or ", found);
    }
}
