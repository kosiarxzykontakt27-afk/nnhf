package pl.elevenclients.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import pl.elevenclients.config.ElevenConfig;

import java.util.Random;

/** Ekran ladowania gry (zamiast czerwonego Mojanga). Bez tekstu i tekstur - dziala zanim zaladuja sie zasoby. */
public final class ElevenSplash {
    private static final int STARS = 70;
    private static long start = -1;
    private static long last = -1;
    private static final float[] a = new float[STARS], r = new float[STARS], v = new float[STARS];
    private static boolean init;

    private ElevenSplash() {}

    private static void digit(DrawContext c, int x, int y, int g, int col) {
        c.fill(x + 4 * g, y, x + 7 * g, y + 16 * g, col);            // trzon
        c.fill(x + 2 * g, y + g, x + 4 * g, y + 2 * g, col);          // skosna flaga
        c.fill(x + g, y + 2 * g, x + 4 * g, y + 3 * g, col);
        c.fill(x, y + 3 * g, x + 4 * g, y + 4 * g, col);
        c.fill(x + g, y + 14 * g, x + 10 * g, y + 16 * g, col);       // podstawa
    }

    public static void draw(DrawContext c) {
        long now = Util.getMeasuringTimeMs();
        if (start < 0) start = now;
        float t = (now - start) / 1000f;
        float dt = last < 0 ? 0.016f : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        if (!init) {
            init = true;
            Random rnd = new Random(7);
            for (int i = 0; i < STARS; i++) {
                a[i] = rnd.nextFloat() * 6.2832f;
                r[i] = 2 + rnd.nextFloat() * 300;
                v[i] = 0.7f + rnd.nextFloat() * 1.3f;
            }
        }
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        float cx = w / 2f, cy = h * 0.46f;
        float k = Math.max(w / 680f, h / 383f);
        int acc = ElevenConfig.accent;

        c.fill(0, 0, w, h, 0xFF010104);

        // lot przez gwiazdy
        for (int i = 0; i < STARS; i++) {
            float step = v[i] * (1 + r[i] / 55f);
            r[i] += step * dt * 60f;
            if (r[i] > 440) { r[i] = 2; a[i] = (float) (Math.random() * 6.2832); }
            int z = 1 + (int) (r[i] / 180f);
            for (int j = 0; j < 3; j++) {
                float rr = r[i] - j * step * 1.4f;
                if (rr < 0) break;
                int x = Math.round(cx + (float) Math.cos(a[i]) * rr * k);
                int y = Math.round(cy + (float) Math.sin(a[i]) * rr * k);
                float al = Math.min(0.8f, r[i] / 200f) * (1 - j * 0.25f);
                c.fill(x, y, x + z, y + z, Draw.argb(al, 205, 222, 255));
            }
        }

        // rozszerzajace sie ramki
        for (int i = 0; i < 3; i++) {
            float p = (t * 0.35f + i / 3f) % 1f;
            int half = Math.round((30 + p * 320f) * k / 2f);
            int col = Draw.withAlpha(acc, 0.3f * (1 - p));
            c.fill((int) cx - half, (int) cy - half, (int) cx + half, (int) cy - half + 1, col);
            c.fill((int) cx - half, (int) cy + half - 1, (int) cx + half, (int) cy + half, col);
            c.fill((int) cx - half, (int) cy - half, (int) cx - half + 1, (int) cy + half, col);
            c.fill((int) cx + half - 1, (int) cy - half, (int) cx + half, (int) cy + half, col);
        }

        // logo 11 (prawa jedynka wyzej), pojawia sie plynnie
        float in = Math.max(0f, Math.min(1f, t / 0.9f));
        in = 1f - (1f - in) * (1f - in) * (1f - in);
        int g = Math.max(2, h / 45);
        int lw = 10 * g, gap = 3 * g;
        int x0 = (int) cx - (lw * 2 + gap) / 2;
        int y0 = (int) (cy - 8 * g) + Math.round((1 - in) * -20f);
        int col = Draw.fade(0xFF000000 | acc, in);
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3f);
        digit(c, x0, y0 + 2 * g, g, col);
        digit(c, x0 + lw + gap, y0 - 2 * g, g, col);
        // poswiata pod logo
        c.fill(x0, y0 + 19 * g, x0 + lw * 2 + gap, y0 + 19 * g + 1, Draw.withAlpha(acc, 0.35f * in * (0.6f + 0.4f * pulse)));

        // pasek postepu (animowany)
        int bw = Math.max(60, w / 4), bh = Math.max(2, h / 180);
        int bx = (int) cx - bw / 2, by = (int) (h * 0.8f);
        c.fill(bx, by, bx + bw, by + bh, 0x33FFFFFF);
        float seg = bw * 0.35f;
        float pos = ((t * 0.7f) % 1.35f) * bw - seg * 0.2f;
        int sx1 = Math.max(bx, bx + (int) pos), sx2 = Math.min(bx + bw, bx + (int) (pos + seg));
        if (sx2 > sx1) c.fill(sx1, by, sx2, by + bh, 0xFF000000 | acc);
    }
}
