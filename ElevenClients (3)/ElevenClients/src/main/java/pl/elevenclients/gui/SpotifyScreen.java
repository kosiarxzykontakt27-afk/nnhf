package pl.elevenclients.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pl.elevenclients.config.ElevenConfig;

import java.util.ArrayList;
import java.util.List;

public class SpotifyScreen extends Screen {
    private static final int WW = 380, WH = 236;
    private static final int GREEN = 0xFF1DB954;

    private static class Hit { int x1, y1, x2, y2; Runnable run; }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private String input = "";
    private float s, wx, wy;

    public SpotifyScreen(Screen parent) {
        super(Text.literal("Spotify"));
        this.parent = parent;
    }

    private int accent() { return 0xFF000000 | ElevenConfig.accent; }

    private void hit(int x1, int y1, int x2, int y2, Runnable r) {
        Hit h = new Hit(); h.x1 = x1; h.y1 = y1; h.x2 = x2; h.y2 = y2; h.run = r; hits.add(h);
    }

    private void layout() {
        s = Math.min(ElevenConfig.guiScale * 1.15f, Math.min((width - 20f) / WW, (height - 20f) / WH));
        wx = (width - WW * s) / 2f;
        wy = (height - WH * s) / 2f;
    }

    private void button(DrawContext c, double lmx, double lmy, int x1, int y1, int x2, int y2, String label, Runnable r) {
        boolean hov = lmx >= x1 && lmx < x2 && lmy >= y1 && lmy < y2;
        Draw.roundBorder(c, x1, y1, x2, y2, 10, hov ? 0xFF0F1A38 : 0xFF0A1020, hov ? GREEN : 0xFF222F55);
        Draw.text(c, textRenderer, label, (x1 + x2) / 2f - Draw.width(textRenderer, label, 1.15f) / 2f, y1 + (y2 - y1 - 9) / 2f, 0xFFEEF3FF, 1.15f);
        hit(x1, y1, x2, y2, r);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        layout();
        hits.clear();
        SpotifyControl.poll();
        c.fill(0, 0, width, height, 0xAA000000);
        double lmx = (mx - wx) / s, lmy = (my - wy) / s;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(wx, wy, 0);
        m.scale(s, s, 1f);
        Draw.roundBorder(c, 0, 0, WW, WH, 14, 0xF2020409, 0xFF16233F);
        Draw.text(c, textRenderer, "Spotify", 16, 12, GREEN, 1.7f);
        Draw.text(c, textRenderer, "Sterowanie aplikacj\u0105 Spotify na komputerze (Windows).", 16, 33, 0xFF6B7A9C, 0.8f);

        // co gra
        Draw.roundBorder(c, 14, 52, WW - 14, 90, 10, 0xFF0A1020, 0xFF16233F);
        String np = SpotifyControl.nowPlaying.isEmpty() ? "Nic nie gra (lub Spotify jest wy\u0142\u0105czony)" : SpotifyControl.nowPlaying;
        if (np.length() > 52) np = np.substring(0, 51) + "...";
        Draw.text(c, textRenderer, "Teraz gra", 24, 58, 0xFF6B7A9C, 0.8f);
        Draw.text(c, textRenderer, np, 24, 71, 0xFFEEF3FF, 1.1f);

        // sterowanie
        button(c, lmx, lmy, 14, 100, 14 + 100, 128, "<<  Poprzedni", SpotifyControl::previous);
        button(c, lmx, lmy, 14 + 108, 100, 14 + 108 + 130, 128, "Play / Pauza", SpotifyControl::playPause);
        button(c, lmx, lmy, 14 + 246, 100, WW - 14, 128, "Nast\u0119pny  >>", SpotifyControl::next);

        // link
        Draw.text(c, textRenderer, "Wklej link do utworu / playlisty / albumu:", 16, 142, 0xFF8393B8, 0.9f);
        Draw.roundBorder(c, 14, 156, WW - 104, 182, 8, 0xFF0A1020, 0xFF222F55);
        boolean blink = (Util.getMeasuringTimeMs() / 500) % 2 == 0;
        String shown = input.isEmpty() ? "https://open.spotify.com/..." : input;
        float maxW = WW - 104 - 14 - 16;
        while (Draw.width(textRenderer, shown, 1.0f) > maxW && shown.length() > 3) shown = shown.substring(1);
        if (!input.isEmpty() && blink) shown += "|";
        Draw.text(c, textRenderer, shown, 22, 165, input.isEmpty() ? 0xFF4A5A80 : 0xFFEEF3FF, 1.0f);
        button(c, lmx, lmy, WW - 96, 156, WW - 14, 182, "Odtw\u00f3rz", this::playInput);

        if (!SpotifyControl.status.isEmpty()) Draw.text(c, textRenderer, SpotifyControl.status, 16, 194, 0xFF8393B8, 0.85f);
        Draw.text(c, textRenderer, "Klawisze: Num5 = play/pauza, Num6 = nast\u0119pny, Num4 = poprzedni, O = to okno", 16, 212, 0xFF4A5A80, 0.8f);
        m.pop();
    }

    private void playInput() {
        SpotifyControl.playLink(input);
        if (SpotifyControl.status.startsWith("Wysy")) input = "";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            SpotifyControl.status = "";
            double lmx = (mx - wx) / s, lmy = (my - wy) / s;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (lmx >= h.x1 && lmx < h.x2 && lmy >= h.y1 && lmy < h.y2) { h.run.run(); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (input.length() < 200 && chr >= 32 && chr < 127) { input += chr; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 259 && !input.isEmpty()) { input = input.substring(0, input.length() - 1); return true; }
        if (keyCode == 257 || keyCode == 335) { playInput(); return true; }
        if (keyCode == 86 && (modifiers & 2) != 0) { // Ctrl+V
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null) input = clip.trim().replaceAll("[^\\x20-\\x7E]", "");
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() { MinecraftClient.getInstance().setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
