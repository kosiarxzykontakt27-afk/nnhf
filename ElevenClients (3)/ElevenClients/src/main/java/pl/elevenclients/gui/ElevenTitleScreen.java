package pl.elevenclients.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import pl.elevenclients.ElevenClients;
import pl.elevenclients.config.ElevenConfig;

import java.util.Random;

public class ElevenTitleScreen extends Screen {
    private static final int BW = 270, BH = 32;
    private static final int STARS = 90;

    private final float[] wa = new float[STARS], wr = new float[STARS], wv = new float[STARS];
    private final Random wRand = new Random(11);

    private final String[] labels = {
            "Tryb jednoosobowy", "Tryb wieloosobowy", "Mods", "Ustawienia...", "Wyjd\u017a z gry"
    };
    /** Pozycje Y przyciskow w lokalnym ukladzie (0 = srodek ekranu). */
    private final int[] ys = {-84, -46, -8, 42, 80};

    private float openT = -1f;
    private float lastT = -1f;
    private float curA = 1f;

    public ElevenTitleScreen() {
        super(Text.literal("ElevenClients"));
        for (int i = 0; i < STARS; i++) {
            wa[i] = wRand.nextFloat() * 6.2832f;
            wr[i] = 2 + wRand.nextFloat() * 360;
            wv[i] = 0.6f + wRand.nextFloat() * 1.2f;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private int accent() { return 0xFF000000 | ElevenConfig.accent; }
    private int f(int color) { return Draw.fade(color, curA); }
    private float s() { return ElevenConfig.guiScale; }
    private double lx(double mx, double ax) { return (mx - ax) / s(); }
    private double ly(double my, double ay) { return (my - ay) / s(); }
    private float menuAx() { return width / 2f; }
    private float menuAy() { return height * 0.53f; }

    private boolean over(double lx, double ly, int x1, int y1, int x2, int y2) {
        return lx >= x1 && lx < x2 && ly >= y1 && ly < y2;
    }

    /** Animacja wjazdu: 0 -> 1 (ease-out) po opoznieniu delay. */
    private float ease(float tt, float delay) {
        float p = Math.max(0f, Math.min(1f, (tt - delay) / 0.7f));
        float q = 1f - p;
        return 1f - q * q * q;
    }

    /* ---------- render ---------- */

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float now = Util.getMeasuringTimeMs() / 1000f;
        if (openT < 0) openT = now;
        float tt = now - openT;

        drawBackground(ctx, now, ease(tt, 0f));
        drawLogo(ctx, ease(tt, 0.10f));
        drawButtons(ctx, mx, my, tt);
        drawSkin(ctx, now, ease(tt, 0.35f));
        drawWindowButtons(ctx, mx, my, ease(tt, 0.20f));
        drawMultiK(ctx, mx, my, ease(tt, 0.30f));
        drawVersions(ctx, ease(tt, 0.80f));
    }

    private void drawBackground(DrawContext c, float t, float bgA) {
        float dt = lastT < 0 ? 0.016f : Math.min(0.1f, t - lastT);
        lastT = t;
        c.fill(0, 0, width, height, 0xFF010104);
        float cx = width / 2f, cy = height * 0.46f;
        float k = Math.max(width / 680f, height / 383f);

        for (int i = 0; i < STARS; i++) {
            float step = wv[i] * (1 + wr[i] / 55f);
            wr[i] += step * dt * 60f;
            if (wr[i] > 440) {
                wr[i] = 2 + wRand.nextFloat() * 10;
                wa[i] = wRand.nextFloat() * 6.2832f;
                wv[i] = 0.6f + wRand.nextFloat() * 1.2f;
            }
            int z = 1 + (int) (wr[i] / 180f);
            for (int j = 0; j < 4; j++) {
                float r = wr[i] - j * step * 1.4f;
                if (r < 0) break;
                int x = Math.round(cx + (float) Math.cos(wa[i]) * r * k);
                int y = Math.round(cy + (float) Math.sin(wa[i]) * r * k);
                float al = Math.min(0.8f, wr[i] / 200f) * (1 - j * 0.22f) * bgA;
                if (al > 0.02f) c.fill(x, y, x + z, y + z, Draw.argb(al, 205, 222, 255));
            }
        }

        MatrixStack m = c.getMatrices();
        for (int i = 0; i < 4; i++) {
            float p = (t * 0.25f + i / 4f) % 1f;
            int half = Math.round((40 + p * 360f) * k / 2f);
            int col = Draw.withAlpha(ElevenConfig.accent, 0.28f * (1 - p) * bgA);
            m.push();
            m.translate(cx, cy, 0);
            m.multiply(new Quaternionf().rotateZ(t * 0.15f + i * 0.4f));
            c.fill(-half, -half, half, -half + 1, col);
            c.fill(-half, half - 1, half, half, col);
            c.fill(-half, -half, -half + 1, half, col);
            c.fill(half - 1, -half, half, half, col);
            m.pop();
        }
        c.fillGradient(0, 0, width, height / 3, 0xCC000000, 0x00000000);
        c.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xCC000000);
    }

    private void drawLogo(DrawContext c, float p) {
        if (p < 0.05f) return;
        curA = p;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(width / 2f, 8 + (1 - p) * -26f, 0);
        m.scale(s(), s(), 1f);
        m.push();
        m.translate(-19, 10, 0);
        m.scale(5f, 5f, 1f);
        c.drawText(textRenderer, "1", 0, 0, f(accent()), false);
        m.pop();
        m.push();
        m.translate(3, 0, 0);
        m.scale(5f, 5f, 1f);
        c.drawText(textRenderer, "1", 0, 0, f(accent()), false);
        m.pop();
        m.push();
        m.translate(0, 58, 0);
        m.scale(0.8f, 0.8f, 1f);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("E L E V E N C L I E N T S"), 0, 0, f(0xFF8393B8));
        m.pop();
        m.pop();
    }

    private void drawButtons(DrawContext c, int mx, int my, float tt) {
        MatrixStack m = c.getMatrices();
        double lmx = lx(mx, menuAx()), lmy = ly(my, menuAy());
        m.push();
        m.translate(menuAx(), menuAy(), 0);
        m.scale(s(), s(), 1f);
        for (int i = 0; i < labels.length; i++) {
            float p = ease(tt, 0.25f + 0.09f * i);
            if (p < 0.05f) continue;
            curA = p;
            int y = ys[i];
            int off = Math.round((1 - p) * 16f);
            boolean hov = p > 0.9f && over(lmx, lmy, -BW / 2, y, BW / 2, y + BH);
            int lift = hov ? -1 : 0;
            Draw.roundBorder(c, -BW / 2, y + off + lift, BW / 2, y + BH + off + lift, 10,
                    f(hov ? 0xFF0F1A38 : 0xE6090E1C), f(hov ? accent() : 0xFF222F55));
            c.drawCenteredTextWithShadow(textRenderer, Text.literal(labels[i]), 0,
                    y + off + lift + (BH - 8) / 2, f(hov ? 0xFFFFFFFF : 0xFFD3DCF3));
        }
        m.pop();
    }

    private void drawSkin(DrawContext c, float t, float p) {
        if (p < 0.05f) return;
        curA = p;
        MinecraftClient mc = MinecraftClient.getInstance();
        float ax = 24 + 40 * s() - (1 - p) * 30f, ay = height * 0.53f;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(ax, ay, 0);
        m.scale(s(), s(), 1f);

        String nick = mc.getSession().getUsername();
        int tw = textRenderer.getWidth(nick) + 24;
        Draw.roundBorder(c, -tw / 2, -104, tw / 2, -86, 9, f(0xE6090E1C), f(0xFF222F55));
        c.fill(-tw / 2 + 7, -97, -tw / 2 + 10, -94, f(0xFF22C55E));
        c.drawText(textRenderer, nick, -tw / 2 + 14, -99, f(0xFFEEF3FF), false);

        Draw.round(c, -42, 102, 42, 110, 4, f(0x99000000));

        try {
            Identifier tex = SkinUtil.skin(nick);
            int oy = -72 + (int) Math.round(Math.sin(t * 2f) * 2);
            part(c, tex, -20, oy, 8, 8, 8, 8);
            part(c, tex, -20, oy + 40, 20, 20, 8, 12);
            part(c, tex, -40, oy + 40, 44, 20, 4, 12);
            part(c, tex, 20, oy + 40, 36, 52, 4, 12);
            part(c, tex, -20, oy + 100, 4, 20, 4, 12);
            part(c, tex, 0, oy + 100, 20, 52, 4, 12);
            part(c, tex, -20, oy, 40, 8, 8, 8);
            part(c, tex, -20, oy + 40, 20, 36, 8, 12);
            part(c, tex, -40, oy + 40, 44, 36, 4, 12);
            part(c, tex, 20, oy + 40, 52, 52, 4, 12);
            part(c, tex, -20, oy + 100, 4, 36, 4, 12);
            part(c, tex, 0, oy + 100, 4, 52, 4, 12);
        } catch (Throwable ignored) { }

        c.drawCenteredTextWithShadow(textRenderer, Text.literal("Zmie\u0144 skina"), 0, 116, f(0xFF8393B8));
        m.pop();
    }

    private void part(DrawContext c, Identifier tex, int x, int y, int u, int v, int w, int h) {
        final int k = 5;
        c.drawTexture(RenderLayer::getGuiTextured, tex, x, y, (float) u, (float) v, w * k, h * k, w, h, 64, 64);
    }

    private void drawWindowButtons(DrawContext c, int mx, int my, float p) {
        if (p < 0.05f) return;
        curA = p;
        MatrixStack m = c.getMatrices();
        float ax = width - 8, ay = 8 - (1 - p) * 20f;
        double lmx = lx(mx, width - 8), lmy = ly(my, 8);
        m.push();
        m.translate(ax, ay, 0);
        m.scale(s(), s(), 1f);
        String[] icons = {"\u2013", "\u25A1", "\u00D7"};
        for (int i = 0; i < 3; i++) {
            int x2 = -(2 - i) * 31, x1 = x2 - 26;
            boolean hov = over(lmx, lmy, x1, 0, x1 + 26, 22);
            int border = hov ? (i == 2 ? 0xFFEF4444 : accent()) : 0xFF222F55;
            Draw.roundBorder(c, x1, 0, x1 + 26, 22, 7, f(0xE6090E1C), f(border));
            c.drawCenteredTextWithShadow(textRenderer, Text.literal(icons[i]), x1 + 13, 7, f(hov ? 0xFFFFFFFF : 0xFF8393B8));
        }
        m.pop();
    }

    private int multiKWidth(String nick) {
        return 34 + Math.max(textRenderer.getWidth(nick), 32) + 10;
    }

    private void drawMultiK(DrawContext c, int mx, int my, float p) {
        if (p < 0.05f) return;
        curA = p;
        String nick = MinecraftClient.getInstance().getSession().getUsername();
        int pw = multiKWidth(nick);
        boolean hov = over(lx(mx, 8), ly(my, 8), 0, 0, pw, 28);
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(8 - (1 - p) * 30f, 8, 0);
        m.scale(s(), s(), 1f);
        Draw.roundBorder(c, 0, 0, pw, 28, 9, f(hov ? 0xFF0F1A38 : 0xE6090E1C), f(hov ? accent() : 0xFF222F55));
        Draw.head(c, SkinUtil.skin(nick), 5, 5, 18);
        Draw.text(c, textRenderer, "MultiK", 29, 4, f(accent()), 0.8f);
        c.drawText(textRenderer, nick, 29, 14, f(0xFFEEF3FF), false);
        m.pop();
    }

    private void drawVersions(DrawContext c, float p) {
        if (p < 0.05f) return;
        curA = p;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(8, height - 8, 0);
        m.scale(s(), s(), 1f);
        c.drawText(textRenderer, ElevenClients.NAME + " " + ElevenClients.VERSION, 0, -22, f(0xFFEEF3FF), true);
        c.drawText(textRenderer, "Fabric \u00B7 " + ElevenClients.modules.getAll().size() + " modu\u0142\u00F3w", 0, -11, f(0xFF6A7AA2), true);
        m.pop();
        m.push();
        m.translate(width - 8, height - 8, 0);
        m.scale(s(), s(), 1f);
        String v = "Minecraft 1.21.4 (Fabric)";
        c.drawText(textRenderer, v, -textRenderer.getWidth(v), -11, f(0xFF8090B6), true);
        m.pop();
    }

    /* ---------- klikniecia ---------- */

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        MinecraftClient mc = MinecraftClient.getInstance();

        String nick = mc.getSession().getUsername();
        if (over(lx(mx, 8), ly(my, 8), 0, 0, multiKWidth(nick), 28)) {
            mc.setScreen(new MultiKScreen(this));
            return true;
        }

        double lmx = lx(mx, menuAx()), lmy = ly(my, menuAy());
        for (int i = 0; i < labels.length; i++) {
            if (over(lmx, lmy, -BW / 2, ys[i], BW / 2, ys[i] + BH)) {
                switch (i) {
                    case 0 -> mc.setScreen(new SelectWorldScreen(this));
                    case 1 -> mc.setScreen(new MultiplayerScreen(this));
                    case 2 -> mc.setScreen(new ClickGuiScreen(this));
                    case 3 -> mc.setScreen(new OptionsScreen(this, mc.options));
                    case 4 -> mc.scheduleStop();
                }
                return true;
            }
        }

        double wx = lx(mx, width - 8), wy = ly(my, 8);
        for (int i = 0; i < 3; i++) {
            int x2 = -(2 - i) * 31, x1 = x2 - 26;
            if (over(wx, wy, x1, 0, x1 + 26, 22)) {
                long h = mc.getWindow().getHandle();
                if (i == 0) GLFW.glfwIconifyWindow(h);
                else if (i == 1) {
                    if (GLFW.glfwGetWindowAttrib(h, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE) GLFW.glfwRestoreWindow(h);
                    else GLFW.glfwMaximizeWindow(h);
                } else mc.scheduleStop();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
