package com.override.net;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * One end of a relay conversation, used by both the game and the Astra console.
 *
 * <p>Connecting and reading both block, so they happen on a background thread;
 * every line that arrives is handed back to the JavaFX Application Thread with
 * {@link Platform#runLater} before any handler touches the game or the UI.
 * Touching a scene from the socket thread is the classic crash in this pattern.
 */
public final class RelayLink {

    /** What happened on the link; always delivered on the FX thread. */
    public interface Listener {
        void onLine(String line);
        void onStatus(String status, boolean connected);
    }

    private final String host;
    private final int port;
    private final String room;
    private final String role;
    private final String name;
    private final Listener listener;

    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile boolean closed;

    public RelayLink(String host, int port, String room, String role, String name, Listener listener) {
        this.host = host;
        this.port = port;
        this.room = room;
        this.role = role;
        this.name = name;
        this.listener = listener;
    }

    /** Dials the relay on a background thread and keeps reading until {@link #close()}. */
    public void connect() {
        Thread worker = new Thread(this::run, "relay-link");
        worker.setDaemon(true);
        worker.start();
    }

    private void run() {
        status("Connecting to " + host + ":" + port + " ...", false);
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 8000);
            socket = s;
            PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
            out = writer;
            writer.println("JOIN " + room + " " + role + " " + name);
            status("Connected to room " + room, true);

            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                String received = line;
                // Re-checked inside the lambda too: close() can land between the
                // post and the FX thread running it.
                if (closed) break;
                Platform.runLater(() -> { if (!closed) listener.onLine(received); });
            }
            status("The relay closed the connection.", false);
        } catch (IOException e) {
            status(closed ? "Disconnected." : "Could not reach the relay: " + e.getMessage(), false);
        } finally {
            out = null;
            socket = null;
        }
    }

    /** Sends one line; safe to call from the FX thread (writing does not block meaningfully). */
    public void send(String line) {
        PrintWriter writer = out;
        if (writer == null) return;
        synchronized (writer) {
            writer.println(line);
        }
    }

    public boolean isConnected() {
        return out != null;
    }

    public void close() {
        closed = true;
        Socket s = socket;
        try {
            if (s != null) s.close();
        } catch (IOException ignored) {
            // closing anyway
        }
    }

    /**
     * Report a link change to the owner, unless {@link #close()} has already run.
     *
     * <p>Closing the socket unblocks the reader, which then reports the drop — but
     * by then the screen that owns this link has torn itself down, so delivering it
     * would push a toast into a dead scene graph and restart the very timers that
     * dispose() just stopped. A deliberate close needs no announcement.</p>
     */
    private void status(String message, boolean connected) {
        if (closed) return;
        Platform.runLater(() -> { if (!closed) listener.onStatus(message, connected); });
    }
}
