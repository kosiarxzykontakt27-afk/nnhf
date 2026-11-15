package pl.elevenclients.gui;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Slider {
    public final String label, unit;
    public final float min, max;
    public final int decimals;
    public final Supplier<Float> get;
    public final Consumer<Float> set;

    public Slider(String label, float min, float max, int decimals, Supplier<Float> get, Consumer<Float> set, String unit) {
        this.label = label; this.min = min; this.max = max; this.decimals = decimals;
        this.get = get; this.set = set; this.unit = unit;
    }

    public float fraction() {
        return Math.max(0f, Math.min(1f, (get.get() - min) / (max - min)));
    }

    public String text() {
        float v = get.get();
        if (decimals == 0) return Math.round(v) + unit;
        return String.format(Locale.ROOT, "%." + decimals + "f%s", v, unit);
    }
}
