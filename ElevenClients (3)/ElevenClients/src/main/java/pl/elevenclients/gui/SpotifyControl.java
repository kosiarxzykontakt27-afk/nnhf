package pl.elevenclients.gui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sterowanie aplikacja Spotify (Windows) bez kluczy API:
 * - co gra: odczyt tytulu okna Spotify ("Artysta - Utwor"),
 * - play/pauza/nastepny/poprzedni: klawisze multimedialne systemu,
 * - odtworzenie linku: schemat spotify: (otwiera i odpala w aplikacji).
 */
public final class SpotifyControl {
    public static volatile String nowPlaying = "";
    public static volatile String status = "";
    private static volatile boolean polling;
    private static long last;
    private static final boolean WIN = System.getProperty("os.name", "").toLowerCase().contains("win");

    private SpotifyControl() {}

    public static boolean supported() { return WIN; }

    /** Odswieza "co gra" (maks. co 3 s, w tle). */
    public static void poll() {
        long now = System.currentTimeMillis();
        if (!WIN || polling || now - last < 3000) return;
        last = now;
        polling = true;
        Thread t = new Thread(() -> {
            try {
                String cmd = "[Console]::OutputEncoding=[Text.Encoding]::UTF8; "
                        + "Get-Process Spotify -ErrorAction SilentlyContinue | Where-Object {$_.MainWindowTitle} | ForEach-Object { $_.MainWindowTitle }";
                Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command", cmd).redirectErrorStream(true).start();
                String found = "";
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.contains(" - ") && !line.startsWith("Spotify")) { found = line; break; }
                    }
                }
                nowPlaying = found;
            } catch (Throwable ignored) {
                nowPlaying = "";
            } finally {
                polling = false;
            }
        }, "ElevenClients-Spotify");
        t.setDaemon(true);
        t.start();
    }

    private static void run(String... cmd) {
        Thread t = new Thread(() -> {
            try { new ProcessBuilder(cmd).start(); } catch (Throwable ignored) { }
        }, "ElevenClients-Spotify-cmd");
        t.setDaemon(true);
        t.start();
    }

    private static void mediaKey(int vk) {
        if (!WIN) { status = "Sterowanie dzia\u0142a na Windows"; return; }
        run("powershell", "-NoProfile", "-Command", "(New-Object -ComObject WScript.Shell).SendKeys([char]" + vk + ")");
    }

    public static void playPause() { mediaKey(179); }
    public static void next() { mediaKey(176); }
    public static void previous() { mediaKey(177); }

    /** Odtwarza link ze Spotify (utwor, album, playlista...). */
    public static void playLink(String raw) {
        String s = raw == null ? "" : raw.trim();
        String uri = null;
        Matcher m = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]+/)?(track|album|playlist|artist|episode|show)/([A-Za-z0-9]+)").matcher(s);
        if (m.find()) uri = "spotify:" + m.group(1) + ":" + m.group(2);
        else if (s.startsWith("spotify:")) uri = s;
        if (uri == null) { status = "To nie jest link ze Spotify"; return; }
        if (!uri.endsWith(":play")) uri += ":play";
        String os = System.getProperty("os.name", "").toLowerCase();
        if (WIN) run("cmd", "/c", "start", "", uri);
        else if (os.contains("mac")) run("open", uri);
        else run("xdg-open", uri);
        status = "Wysy\u0142am do Spotify...";
    }
}
