package pl.elevenclients.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    /** page: "mods" albo "visual"; group: zakladka (Combat, Player, HUD...). */
    public final String page, group, name, desc;
    private boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    public static class Setting {
        public final String label, unit;
        public final float min, max;
        public float value;

        public Setting(String label, float min, float max, float value, String unit) {
            this.label = label; this.min = min; this.max = max; this.value = value; this.unit = unit;
        }
    }

    public Module(String page, String group, String name, String desc) {
        this.page = page; this.group = group; this.name = name; this.desc = desc;
    }

    public Module on() { this.enabled = true; return this; }

    public Module slider(String label, float min, float max, float value, String unit) {
        settings.add(new Setting(label, min, max, value, unit));
        return this;
    }

    protected float val(int i) { return settings.size() > i ? settings.get(i).value : 0f; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (this.enabled == value) return;
        this.enabled = value;
        if (value) onEnable(); else onDisable();
    }

    public boolean isEnabled() { return enabled; }
    public String getName() { return name; }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender(DrawContext ctx) {}
}
