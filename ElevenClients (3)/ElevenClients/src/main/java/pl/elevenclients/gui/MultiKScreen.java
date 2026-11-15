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

public class MultiKScreen extends Screen {
    private static final int WW = 380, WH = 344, ROWS = 5;

    private static class Hit { int x1, y1, x2, y2; Runnable run; }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private String input = "";
    private String msg = "";
    private int offset = 0;
    private float s, wx, wy;

    public MultiKScreen(Screen parent) {
        super(Text.literal("MultiK"));
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

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        layout();
        hits.clear();
        c.fill(0, 0, width, height, 0xAA000000);
        MinecraftClient mc = MinecraftClient.getInstance();
        double lmx = (mx - wx) / s, lmy = (my - wy) / s;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(wx, wy, 0);
        m.scale(s, s, 1f);
        Draw.roundBorder(c, 0, 0, WW, WH, 14, 0xF2020409, 0xFF16233F);
        Draw.text(c, textRenderer, "MultiK", 16, 12, accent(), 1.7f);
        Draw.text(c, textRenderer, "Konta premium (Microsoft) i offline. Kliknij konto, aby je w\u0142\u0105czy\u0107.", 16, 33, 0xFF6B7A9C, 0.8f);

        List<AccountStore.Entry> acc = AccountStore.all();
        String active = mc.getSession().getUsername();
        for (int i = 0; i < ROWS && offset + i < acc.size(); i++) {
            AccountStore.Entry e = acc.get(offset + i);
            int y = 52 + i * 40;
            boolean act = e.name.equals(active);
            boolean hov = lmx >= 14 && lmx < WW - 14 && lmy >= y && lmy < y + 34;
            Draw.roundBorder(c, 14, y, WW - 14, y + 34, 10, act || hov ? 0xFF101A33 : 0xFF0A1020, act ? accent() : (hov ? 0xFF222F55 : 0xFF16233F));
            Draw.head(c, SkinUtil.skin(e.name), 22, y + 5, 24);
            Draw.text(c, textRenderer, e.name, 56, y + 7, 0xFFEEF3FF, 1.15f);
            String type = e.original ? "g\u0142\u00f3wne konto" : (e.premium ? "premium" : "offline");
            Draw.text(c, textRenderer, act ? "aktywne \u00b7 " + type : type, 56, y + 21, act ? 0xFF22C55E : 0xFF6B7A9C, 0.8f);
            hit(14, y, WW - 44, y + 34, () -> {
                if (e.premium) PremiumAuth.switchTo(e.name, () -> { });
                else PremiumAuth.status = AccountStore.switchTo(e) ? "Aktywne konto: " + e.name : "Nie uda\u0142o si\u0119 zmieni\u0107 konta: " + AccountStore.lastError;
            });
            if (!e.original) {
                Draw.text(c, textRenderer, "x", WW - 34, y + 11, 0xFF8393B8, 1.3f);
                hit(WW - 44, y, WW - 14, y + 34, () -> AccountStore.remove(e));
            }
        }

        // dodawanie konta offline
        int fy = 52 + ROWS * 40 + 4;
        Draw.roundBorder(c, 14, fy, WW - 104, fy + 26, 8, 0xFF0A1020, 0xFF222F55);
        boolean blink = (Util.getMeasuringTimeMs() / 500) % 2 == 0;
        String shown = input.isEmpty() ? "Nick konta offline..." : input + (blink ? "|" : "");
        Draw.text(c, textRenderer, shown, 22, fy + 9, input.isEmpty() ? 0xFF4A5A80 : 0xFFEEF3FF, 1.05f);
        boolean bh = lmx >= WW - 96 && lmx < WW - 14 && lmy >= fy && lmy < fy + 26;
        Draw.roundBorder(c, WW - 96, fy, WW - 14, fy + 26, 8, bh ? 0xFF0F1A38 : 0xFF0A1020, bh ? accent() : 0xFF222F55);
        Draw.text(c, textRenderer, "Dodaj", WW - 55 - Draw.width(textRenderer, "Dodaj", 1.05f) / 2f, fy + 9, 0xFFEEF3FF, 1.05f);
        hit(WW - 96, fy, WW - 14, fy + 26, this::addOffline);

        // logowanie Microsoft
        int py = fy + 34;
        boolean ph = lmx >= 14 && lmx < WW - 14 && lmy >= py && lmy < py + 28;
        Draw.roundBorder(c, 14, py, WW - 14, py + 28, 9, ph ? 0xFF0F1A38 : 0xFF101A33, accent());
        String pl = PremiumAuth.busy ? "Logowanie w toku..." : "Dodaj konto premium (Microsoft)";
        Draw.text(c, textRenderer, pl, WW / 2f - Draw.width(textRenderer, pl, 1.1f) / 2f, py + 10, 0xFFFFFFFF, 1.1f);
        hit(14, py, WW - 14, py + 28, () -> {
            if (!PremiumAuth.busy) PremiumAuth.login(() -> { });
        });

        // status
        String st = !msg.isEmpty() ? msg : PremiumAuth.status;
        if (!st.isEmpty()) {
            Draw.text(c, textRenderer, st, 16, WH - 20, PremiumAuth.codeUrl != null ? accent() : 0xFF8393B8, 0.85f);
            if (PremiumAuth.codeUrl != null) hit(0, WH - 26, WW, WH - 4, PremiumAuth::openLink);
        }
        m.pop();
    }

    private void addOffline() {
        if (AccountStore.addOffline(input)) { input = ""; msg = ""; }
        else msg = "Nick: 3-16 znak\u00f3w (A-Z, 0-9, _), bez duplikat\u00f3w";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            msg = "";
            double lmx = (mx - wx) / s, lmy = (my - wy) / s;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (lmx >= h.x1 && lmx < h.x2 && lmy >= h.y1 && lmy < h.y2) { h.run.run(); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int max = Math.max(0, AccountStore.all().size() - ROWS);
        offset = Math.max(0, Math.min(max, offset - (int) Math.signum(v)));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (input.length() < 16 && (Character.isLetterOrDigit(chr) || chr == '_') && chr < 128) {
            input += chr;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 259 && !input.isEmpty()) { input = input.substring(0, input.length() - 1); return true; }
        if (keyCode == 257 || keyCode == 335) { addOffline(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() { MinecraftClient.getInstance().setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
