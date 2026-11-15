package pl.elevenclients.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import pl.elevenclients.ElevenClients;
import pl.elevenclients.config.ElevenConfig;
import pl.elevenclients.module.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Glowne GUI: Mods / Visual / Friends / Cosmetics / Settings. */
public class ClickGuiScreen extends Screen {
    private static final int WW = 680, WH = 383, NAV = 168;
    private static final String[] PAGES = {"Mods", "Visual", "Friends", "Cosmetics", "Settings"};
    private static final String[][] CHIPS = {
            {"Combat", "Player", "Movement", "World"},
            {"Mods", "Visual", "HUD"},
            {},
            {},
            {"Interfejs", "\u015awiat", "Sterowanie", "Og\u00f3lne"}
    };
    private static final int[] ACCENTS = {0x0EA5E9, 0x3B82F6, 0x8B5CF6, 0xEF4444, 0x22C55E};
    private static final String[] COS_NAMES = {"Eleven", "Midnight", "Violet", "Ice", "Ember", "Mint", "Stone", "Snow"};
    private static final int[] COS_COLORS = {0x0EA5E9, 0x0C1230, 0x8B5CF6, 0x22D3EE, 0xEF4444, 0x22C55E, 0x1B2745, 0xE8EEFF};

    private static class Hit {
        int x1, y1, x2, y2;
        Runnable click;
        Slider slider;
        int tx1, tx2;
    }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final int[] chip = new int[5];
    private int page = 0;
    private int cosSel = 0;
    private Module expanded = null;
    private final Map<Module.Setting, Slider> sliderCache = new HashMap<>();
    private Slider drag;
    private Hit dragHit;
    private Float pendingScale = null;
    private float s, wx, wy;

    private final Slider scaleSlider, fovSlider, brightSlider, distSlider, fpsSlider;

    public ClickGuiScreen(Screen parent) {
        super(Text.literal("ElevenClients"));
        this.parent = parent;
        MinecraftClient mc = MinecraftClient.getInstance();
        scaleSlider = new Slider("Skala GUI", 0.5f, 1.25f, 2,
                () -> pendingScale != null ? pendingScale : ElevenConfig.guiScale,
                v -> pendingScale = Math.round(v * 20f) / 20f, "\u00d7");
        fovSlider = new Slider("FOV", 30, 110, 0, () -> (float) mc.options.getFov().getValue(),
                v -> mc.options.getFov().setValue(Math.round(v)), "");
        brightSlider = new Slider("Jasno\u015b\u0107", 0, 100, 0, () -> (float) (mc.options.getGamma().getValue() * 100.0),
                v -> mc.options.getGamma().setValue((double) (v / 100f)), "%");
        distSlider = new Slider("Dystans renderowania", 2, 32, 0, () -> (float) mc.options.getViewDistance().getValue(),
                v -> mc.options.getViewDistance().setValue(Math.round(v)), "");
        fpsSlider = new Slider("Limit FPS", 10, 260, 0, () -> (float) mc.options.getMaxFps().getValue(),
                v -> mc.options.getMaxFps().setValue(Math.round(v)), "");
    }

    private int accent() { return 0xFF000000 | ElevenConfig.accent; }

    private void hit(int x1, int y1, int x2, int y2, Runnable r) {
        Hit h = new Hit(); h.x1 = x1; h.y1 = y1; h.x2 = x2; h.y2 = y2; h.click = r; hits.add(h);
    }

    private void layout() {
        s = Math.min(ElevenConfig.guiScale, Math.min((width - 16f) / WW, (height - 16f) / WH));
        wx = (width - WW * s) / 2f;
        wy = (height - WH * s) / 2f;
    }

    private boolean in(double x, double y, int x1, int y1, int x2, int y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    /* ================= render ================= */

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        layout();
        hits.clear();
        c.fill(0, 0, width, height, 0xAA000000);
        double lmx = (mx - wx) / s, lmy = (my - wy) / s;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(wx, wy, 0);
        m.scale(s, s, 1f);

        Draw.roundBorder(c, 0, 0, WW, WH, 14, 0xF2020409, 0xFF16233F);
        drawNav(c, lmx, lmy);
        if (CHIPS[page].length > 0) drawChips(c, lmx, lmy);
        switch (page) {
            case 0 -> drawModules(c, lmx, lmy, "mods");
            case 1 -> drawModules(c, lmx, lmy, "visual");
            case 2 -> drawFriends(c);
            case 3 -> drawCosmetics(c, lmx, lmy);
            default -> drawSettings(c, lmx, lmy);
        }
        m.pop();
    }

    private void drawNav(DrawContext c, double lmx, double lmy) {
        Draw.round(c, 1, 1, NAV, WH - 1, 13, 0xFF0A1020);
        c.fill(100, 1, NAV, WH - 1, 0xFF0A1020);
        c.fill(NAV, 1, NAV + 1, WH - 1, 0xFF16233F);
        Draw.round(c, 14, 14, 42, 42, 8, accent());
        Draw.text(c, textRenderer, "11", 28 - Draw.width(textRenderer, "11", 1.2f) / 2f, 24, 0xFFFFFFFF, 1.2f);
        Draw.text(c, textRenderer, "ElevenClients", 50, 22, 0xFFEEF3FF, 1.05f);
        for (int i = 0; i < PAGES.length; i++) {
            int y = 60 + i * 36;
            boolean act = i == page, hov = in(lmx, lmy, 10, y, NAV - 10, y + 32);
            if (act) Draw.roundBorder(c, 10, y, NAV - 10, y + 32, 10, 0xFF101A33, accent());
            else if (hov) Draw.round(c, 10, y, NAV - 10, y + 32, 10, 0xFF0D162B);
            Draw.text(c, textRenderer, PAGES[i], 26, y + 11, act ? 0xFFEEF3FF : (hov ? 0xFFB9C4E0 : 0xFF6B7A9C), 1.2f);
            final int idx = i;
            hit(10, y, NAV - 10, y + 32, () -> { page = idx; expanded = null; });
        }
        // konto
        String nick = MinecraftClient.getInstance().getSession().getUsername();
        Draw.roundBorder(c, 10, WH - 46, NAV - 10, WH - 12, 10, 0xFF101A33, 0xFF16233F);
        Draw.head(c, SkinUtil.skin(nick), 16, WH - 40, 22);
        Draw.text(c, textRenderer, nick, 44, WH - 40, 0xFFEEF3FF, 1.0f);
        Draw.text(c, textRenderer, "v" + ElevenClients.VERSION, 44, WH - 28, 0xFF6B7A9C, 0.8f);
    }

    private void drawChips(DrawContext c, double lmx, double lmy) {
        int x = NAV + 18;
        String[] names = CHIPS[page];
        for (int i = 0; i < names.length; i++) {
            int w = (int) (Draw.width(textRenderer, names[i], 1.1f) + 32);
            boolean act = chip[page] == i, hov = in(lmx, lmy, x, 16, x + w, 40);
            Draw.round(c, x, 16, x + w, 40, 12, act ? accent() : (hov ? 0xFF16233F : 0xFF101A33));
            Draw.text(c, textRenderer, names[i], x + 16, 24, act ? 0xFFFFFFFF : 0xFF6C86C8, 1.1f);
            final int idx = i;
            hit(x, 16, x + w, 40, () -> { chip[page] = idx; expanded = null; });
            x += w + 8;
        }
    }

    /* ---------- Mods / Visual ---------- */

    private Slider sliderFor(Module.Setting st) {
        return sliderCache.computeIfAbsent(st, k -> new Slider(k.label, k.min, k.max, k.max - k.min > 10 ? 0 : 1,
                () -> k.value, v -> k.value = v, k.unit));
    }

    private void drawModules(DrawContext c, double lmx, double lmy, String pageKey) {
        String group = CHIPS[page][chip[page]];
        List<Module> list = ElevenClients.modules.get(pageKey, group);
        int x0 = NAV + 18, colW = 234;
        int[] colY = {56, 56};
        for (int i = 0; i < list.size(); i++) {
            Module mod = list.get(i);
            int col = i % 2, x = x0 + col * (colW + 8), y = colY[col];
            boolean open = mod == expanded;
            int h = 40 + (open ? mod.settings.size() * 24 + 6 : 0);
            boolean on = mod.isEnabled();
            Draw.roundBorder(c, x, y, x + colW, y + h, 12, on ? 0xFF101A33 : 0xFF0A1020, on ? accent() : 0xFF0A1020);
            Draw.round(c, x + 10, y + 8, x + 34, y + 32, 7, 0xFF16233F);
            String letter = mod.name.substring(0, 1);
            Draw.text(c, textRenderer, letter, x + 22 - Draw.width(textRenderer, letter, 1.2f) / 2f, y + 16, on ? accent() : 0xFF6B7A9C, 1.2f);
            Draw.text(c, textRenderer, mod.name, x + 42, y + 8, on ? 0xFFEEF3FF : 0xFFB9C4E0, 1.1f);
            Draw.text(c, textRenderer, mod.desc, x + 42, y + 22, 0xFF6B7A9C, 0.8f);
            if (!mod.settings.isEmpty()) {
                boolean gh = in(lmx, lmy, x + colW - 86, y, x + colW - 52, y + 40);
                Draw.text(c, textRenderer, open ? "^" : "v", x + colW - 74, y + 14, gh || open ? accent() : 0xFF6B7A9C, 1.3f);
                hit(x + colW - 86, y, x + colW - 52, y + 40, () -> expanded = (expanded == mod ? null : mod));
            }
            // przelacznik
            int sx = x + colW - 46, sy = y + 10;
            Draw.round(c, sx, sy, sx + 36, sy + 20, 10, on ? accent() : 0xFF1B2745);
            Draw.round(c, sx + (on ? 19 : 3), sy + 3, sx + (on ? 33 : 17), sy + 17, 7, on ? 0xFFFFFFFF : 0xFF6B7A9C);
            hit(x + colW - 50, y, x + colW, y + 40, mod::toggle);
            if (open) {
                for (int k = 0; k < mod.settings.size(); k++) {
                    drawSlider(c, sliderFor(mod.settings.get(k)), x + 42, y + 44 + k * 24, x + 130, x + colW - 66, x + colW - 12);
                }
            }
            colY[col] += h + 6;
        }
        if (list.isEmpty()) Draw.text(c, textRenderer, "Brak modu\u0142\u00f3w.", x0, 60, 0xFF6B7A9C, 1.1f);
    }

    /** Rysuje suwak: etykieta z lewej, tor, wartosc z prawej. */
    private void drawSlider(DrawContext c, Slider sl, int labelX, int y, int tx1, int tx2, int valRight) {
        Draw.text(c, textRenderer, sl.label, labelX, y + 4, 0xFF8393B8, 0.95f);
        Draw.round(c, tx1, y + 7, tx2, y + 10, 1, 0xFF1B2745);
        int fx = tx1 + Math.round((tx2 - tx1) * sl.fraction());
        if (fx > tx1) Draw.round(c, tx1, y + 7, fx, y + 10, 1, accent());
        Draw.round(c, fx - 4, y + 4, fx + 4, y + 13, 4, 0xFFFFFFFF);
        String v = sl.text();
        Draw.text(c, textRenderer, v, valRight - Draw.width(textRenderer, v, 0.95f), y + 4, 0xFFEEF3FF, 0.95f);
        Hit h = new Hit();
        h.x1 = tx1 - 5; h.x2 = tx2 + 5; h.y1 = y; h.y2 = y + 17; h.slider = sl; h.tx1 = tx1; h.tx2 = tx2;
        hits.add(h);
    }

    /* ---------- Friends ---------- */

    private void drawFriends(DrawContext c) {
        int x = NAV + 18;
        Draw.text(c, textRenderer, "Friends", x, 18, 0xFFEEF3FF, 1.7f);
        Draw.roundBorder(c, x, 56, WW - 18, 150, 12, 0xFF0A1020, 0xFF16233F);
        Draw.text(c, textRenderer, "Lista znajomych i szybkie do\u0142\u0105czanie", x + 16, 72, 0xFFEEF3FF, 1.15f);
        Draw.text(c, textRenderer, "Ta zak\u0142adka potrzebuje serwera ElevenClients,", x + 16, 94, 0xFF6B7A9C, 0.95f);
        Draw.text(c, textRenderer, "wi\u0119c na razie jest pusta. Pojawi si\u0119 tu: lista online,", x + 16, 108, 0xFF6B7A9C, 0.95f);
        Draw.text(c, textRenderer, "zaproszenia i przycisk \u201eDo\u0142\u0105cz\u201d.", x + 16, 122, 0xFF6B7A9C, 0.95f);
    }

    /* ---------- Cosmetics ---------- */

    private void drawCosmetics(DrawContext c, double lmx, double lmy) {
        int x0 = NAV + 18;
        Draw.text(c, textRenderer, "Cosmetics", x0, 18, 0xFFEEF3FF, 1.7f);
        Draw.text(c, textRenderer, "Wygl\u0105d peleryny (na razie tylko lokalny podgl\u0105d)", x0, 40, 0xFF6B7A9C, 0.9f);
        for (int i = 0; i < COS_NAMES.length; i++) {
            int x = x0 + (i % 4) * 121, y = 64 + (i / 4) * 98;
            boolean sel = i == cosSel, hov = in(lmx, lmy, x, y, x + 113, y + 90);
            Draw.roundBorder(c, x, y, x + 113, y + 90, 12, 0xFF0A1020, sel ? accent() : (hov ? 0xFF222F55 : 0xFF16233F));
            Draw.round(c, x + 10, y + 10, x + 103, y + 62, 8, 0xFF000000 | COS_COLORS[i]);
            Draw.text(c, textRenderer, COS_NAMES[i], x + 56 - Draw.width(textRenderer, COS_NAMES[i], 1.0f) / 2f, y + 71, sel ? 0xFFEEF3FF : 0xFF8393B8, 1.0f);
            final int idx = i;
            hit(x, y, x + 113, y + 90, () -> cosSel = idx);
        }
    }

    /* ---------- Settings ---------- */

    private void drawSettings(DrawContext c, double lmx, double lmy) {
        int x0 = NAV + 18, w = WW - 18 - x0;
        switch (chip[4]) {
            case 0 -> {
                Draw.roundBorder(c, x0, 56, x0 + w, 96, 12, 0xFF0A1020, 0xFF16233F);
                Draw.text(c, textRenderer, "Kolor akcentu", x0 + 14, 70, 0xFFEEF3FF, 1.1f);
                for (int i = 0; i < ACCENTS.length; i++) {
                    int sx = x0 + w - 14 - (ACCENTS.length - i) * 28 + 6;
                    boolean sel = ACCENTS[i] == ElevenConfig.accent;
                    Draw.round(c, sx - 2, 66, sx + 22, 90 - 6 + 0, 7, sel ? 0xFFFFFFFF : 0x00000000);
                    Draw.round(c, sx, 68, sx + 20, 88 - 4 + 0, 6, 0xFF000000 | ACCENTS[i]);
                    final int col = ACCENTS[i];
                    hit(sx - 2, 62, sx + 22, 92, () -> { ElevenConfig.accent = col; ElevenConfig.save(); });
                }
                Draw.roundBorder(c, x0, 104, x0 + w, 146, 12, 0xFF0A1020, 0xFF16233F);
                drawSlider(c, scaleSlider, x0 + 14, 116, x0 + 150, x0 + w - 60, x0 + w - 14);
                Draw.text(c, textRenderer, "Skala zmienia si\u0119 po puszczeniu suwaka.", x0, 154, 0xFF6B7A9C, 0.85f);
            }
            case 1 -> {
                Draw.roundBorder(c, x0, 56, x0 + w, 56 + 4 * 30 + 16, 12, 0xFF0A1020, 0xFF16233F);
                Slider[] arr = {fovSlider, brightSlider, distSlider, fpsSlider};
                for (int i = 0; i < arr.length; i++) drawSlider(c, arr[i], x0 + 14, 66 + i * 30, x0 + 170, x0 + w - 64, x0 + w - 14);
                Draw.text(c, textRenderer, "To s\u0105 prawdziwe ustawienia gry (zapisuj\u0105 si\u0119 przy zamkni\u0119ciu).", x0, 202, 0xFF6B7A9C, 0.85f);
            }
            case 2 -> {
                String[][] keys = {{"Otw\u00f3rz menu w grze", "Right Shift"}, {"Zoom", "C"}, {"Freelook", "Left Alt"}, {"Toggle Sprint", "Brak"}};
                for (int i = 0; i < keys.length; i++) {
                    int y = 56 + i * 36;
                    Draw.roundBorder(c, x0, y, x0 + w, y + 30, 10, 0xFF0A1020, 0xFF16233F);
                    Draw.text(c, textRenderer, keys[i][0], x0 + 14, y + 10, 0xFFEEF3FF, 1.05f);
                    float kw = Draw.width(textRenderer, keys[i][1], 1.0f) + 20;
                    Draw.round(c, (int) (x0 + w - 12 - kw), y + 5, x0 + w - 12, y + 25, 7, 0xFF16233F);
                    Draw.text(c, textRenderer, keys[i][1], x0 + w - 12 - kw + 10, y + 10, 0xFFEEF3FF, 1.0f);
                }
            }
            default -> {
                boolean hov = in(lmx, lmy, x0, 56, x0 + 190, 88);
                Draw.roundBorder(c, x0, 56, x0 + 190, 88, 10, hov ? 0xFF0F1A38 : 0xFF0A1020, hov ? accent() : 0xFF222F55);
                Draw.text(c, textRenderer, "Przywr\u00f3\u0107 domy\u015blne", x0 + 95 - Draw.width(textRenderer, "Przywr\u00f3\u0107 domy\u015blne", 1.1f) / 2f, 68, 0xFFEEF3FF, 1.1f);
                hit(x0, 56, x0 + 190, 88, () -> { ElevenConfig.guiScale = 0.65f; ElevenConfig.accent = 0x0EA5E9; ElevenConfig.save(); });
                Draw.text(c, textRenderer, "Skala GUI 0,65\u00d7 i kolor b\u0142\u0119kitny.", x0, 98, 0xFF6B7A9C, 0.85f);
            }
        }
    }

    /* ================= wejscie ================= */

    private void updateDrag(double lmx) {
        if (drag == null || dragHit == null) return;
        float f = (float) Math.max(0, Math.min(1, (lmx - dragHit.tx1) / (double) (dragHit.tx2 - dragHit.tx1)));
        float v = drag.min + f * (drag.max - drag.min);
        if (drag.decimals == 0) v = Math.round(v);
        drag.set.accept(v);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            double lmx = (mx - wx) / s, lmy = (my - wy) / s;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (in(lmx, lmy, h.x1, h.y1, h.x2, h.y2)) {
                    if (h.slider != null) { drag = h.slider; dragHit = h; updateDrag(lmx); }
                    else if (h.click != null) h.click.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag != null) { updateDrag((mx - wx) / s); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drag != null) {
            drag = null;
            dragHit = null;
            if (pendingScale != null) {
                ElevenConfig.guiScale = Math.max(0.5f, Math.min(1.25f, pendingScale));
                pendingScale = null;
                ElevenConfig.save();
            }
            MinecraftClient.getInstance().options.write();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void close() {
        ElevenConfig.save();
        ElevenClients.modules.save();
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.write();
        mc.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
