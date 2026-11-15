package pl.elevenclients.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Male narzedzia do rysowania zaokraglonych prostokatow itp. */
public final class Draw {
    private Draw() {}

    public static int argb(float a, int r, int g, int b) {
        return ((int) (a * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    /** Mnozy alfe koloru ARGB przez a (0..1). */
    public static int fade(int argb, float a) {
        int al = (int) (((argb >>> 24) & 255) * a);
        if (al < 4) al = 0;
        return (al << 24) | (argb & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, float a) {
        return ((int) (a * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** Wypelniony zaokraglony prostokat. */
    public static void round(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        c.fill(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int d = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x1 + d, y1 + i, x2 - d, y1 + i + 1, color);
            c.fill(x1 + d, y2 - i - 1, x2 - d, y2 - i, color);
        }
    }

    /** Zaokraglony prostokat z obwodka 1 px. */
    public static void roundBorder(DrawContext c, int x1, int y1, int x2, int y2, int r, int fill, int border) {
        round(c, x1, y1, x2, y2, r, border);
        round(c, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(1, r - 1), fill);
    }

    /** Tekst w dowolnej skali (bez cienia). */
    public static void text(DrawContext c, TextRenderer tr, String s, float x, float y, int color, float scale) {
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        c.drawText(tr, s, 0, 0, color, false);
        m.pop();
    }

    public static float width(TextRenderer tr, String s, float scale) {
        return tr.getWidth(s) * scale;
    }

    /** Glowa gracza (twarz + czapka) z tekstury skina. */
    public static void head(DrawContext c, Identifier tex, int x, int y, int size) {
        try {
            c.drawTexture(RenderLayer::getGuiTextured, tex, x, y, 8f, 8f, size, size, 8, 8, 64, 64);
            c.drawTexture(RenderLayer::getGuiTextured, tex, x, y, 40f, 8f, size, size, 8, 8, 64, 64);
        } catch (Throwable ignored) { }
    }
}
