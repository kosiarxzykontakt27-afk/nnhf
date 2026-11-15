package pl.elevenclients.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.elevenclients.config.ElevenConfig;
import pl.elevenclients.gui.Draw;

/** Rysowanie linii HUD w lewym i prawym gornym rogu. */
public final class Hud {
    private static int yL = 6, yR = 6;

    private Hud() {}

    public static void begin() { yL = 6; yR = 6; }

    private static int accent() { return 0xFF000000 | ElevenConfig.accent; }

    public static void left(DrawContext c, String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.textRenderer.getWidth(text) + 12;
        Draw.round(c, 6, yL, 6 + w, yL + 14, 5, 0x99090E1C);
        c.fill(7, yL + 3, 9, yL + 11, accent());
        c.drawText(mc.textRenderer, text, 12, yL + 3, 0xFFEEF3FF, false);
        yL += 17;
    }

    public static void right(DrawContext c, String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.textRenderer.getWidth(text) + 12;
        int x2 = mc.getWindow().getScaledWidth() - 6;
        Draw.round(c, x2 - w, yR, x2, yR + 14, 5, 0x99090E1C);
        c.fill(x2 - 2, yR + 3, x2, yR + 11, accent());
        c.drawText(mc.textRenderer, text, x2 - w + 5, yR + 3, 0xFFEEF3FF, false);
        yR += 17;
    }
}
