package com.override.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The OVERRIDE relay: a plain socket server that pairs two players in a room
 * and forwards every line between them.
 *
 * <p>Both sides <em>dial out</em> to this server — the game and the Astra
 * console are both clients. That is what lets two players on different home
 * networks play together: neither router has to accept an incoming connection,
 * which is impossible behind the CGNAT most home ISPs use.
 *
 * <p>Run it anywhere both players can reach: a laptop on the same Wi-Fi, a
 * free cloud VM, or a Tailscale address.
 *
 * <pre>java -cp target/classes com.override.net.OverrideRelay [port]</pre>
 *
 * <p>Protocol: one line of text per message.
 * <ul>
 *   <li>{@code JOIN &lt;room&gt; &lt;role&gt; &lt;name&gt;} — first line a client sends</li>
 *   <li>{@code PEER &lt;role&gt; &lt;name&gt;} / {@code PEERGONE} — relay tells you who else is here</li>
 *   <li>everything else is forwarded verbatim to the other side of the room</li>
 * </ul>
 */
public final class OverrideRelay {

    public static final int DEFAULT_PORT = 5001;

    private final int port;
    private final Map<String, List<Client>> rooms = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public OverrideRelay(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new OverrideRelay(port).serve();
    }

    /** Blocks, accepting clients until {@link #stop()}. */
    public void serve() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("[relay] listening on port " + port);
        while (running) {
            try {
                // accept() blocks; every accepted client gets its own thread so
                // the main thread can go straight back to answering the door.
                Socket socket = serverSocket.accept();
                Thread worker = new Thread(() -> handle(socket), "relay-client");
                worker.setDaemon(true);
                worker.start();
            } catch (IOException e) {
                if (running) System.err.println("[relay] accept failed: " + e.getMessage());
            }
        }
    }

    /** The port actually in use — useful when the server was started on port 0. */
    public int port() {
        return serverSocket == null ? port : serverSocket.getLocalPort();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
            // shutting down anyway
        }
    }

    private void handle(Socket socket) {
        Client client = null;
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String first = in.readLine();
            if (first == null || !first.startsWith("JOIN ")) {
                out.println("ERROR expected JOIN <room> <role> <name>");
                return;
            }
            String[] parts = first.split(" ", 4);
            if (parts.length < 3) {
                out.println("ERROR malformed JOIN");
                return;
            }
            String room = parts[1];
            String role = parts[2];
            String name = parts.length > 3 ? parts[3] : role;

            client = new Client(role, name, out);
            List<Client> members = rooms.computeIfAbsent(room, key -> new ArrayList<>());
            synchronized (members) {
                for (Client other : members) {
                    other.send("PEER " + role + " " + name);
                    client.send("PEER " + other.role + " " + other.name);
                }
                members.add(client);
            }
            out.println("JOINED " + room + " " + role);
            System.out.println("[relay] " + name + " joined " + room + " as " + role);

            String line;
            while ((line = in.readLine()) != null) {
                forward(room, client, line);
            }
        } catch (IOException e) {
            // a player closing their window lands here; nothing to report
        } finally {
            if (client != null) leave(client);
        }
    }

    private void forward(String room, Client from, String line) {
        List<Client> members = rooms.get(room);
        if (members == null) return;
        synchronized (members) {
            for (Client other : members) {
                if (other != from) other.send(line);
            }
        }
    }

    private void leave(Client client) {
        for (Map.Entry<String, List<Client>> entry : rooms.entrySet()) {
            List<Client> members = entry.getValue();
            synchronized (members) {
                if (members.remove(client)) {
                    for (Client other : members) other.send("PEERGONE");
                    System.out.println("[relay] " + client.name + " left " + entry.getKey());
                }
            }
        }
    }

    private static final class Client {
        final String role;
        final String name;
        private final PrintWriter out;

        Client(String role, String name, PrintWriter out) {
            this.role = role;
            this.name = name;
            this.out = out;
        }

        void send(String line) {
            synchronized (out) {
                out.println(line);   // autoFlush is on, so this really goes out
            }
        }
    }
}
