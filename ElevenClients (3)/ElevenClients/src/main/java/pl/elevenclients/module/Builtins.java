package pl.elevenclients.module;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import pl.elevenclients.ElevenClients;
import pl.elevenclients.config.ElevenConfig;
import pl.elevenclients.gui.Draw;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Locale;

/** Wszystkie wbudowane moduly. */
public final class Builtins {
    private Builtins() {}

    private static int accent() { return 0xFF000000 | ElevenConfig.accent; }

    /* ===================== HUD ===================== */

    public static class Watermark extends Module {
        public Watermark() { super("visual", "HUD", "Watermark", "Nazwa klienta"); }
        @Override public void onRender(DrawContext c) { Hud.left(c, ElevenClients.NAME + " " + ElevenClients.VERSION); }
    }

    public static class Fps extends Module {
        public Fps() { super("visual", "HUD", "FPS", "Licznik klatek"); }
        @Override public void onRender(DrawContext c) { Hud.left(c, "FPS: " + mc.getCurrentFps()); }
    }

    public static class Coordinates extends Module {
        public Coordinates() { super("mods", "World", "Coordinates", "Koordynaty gracza"); }
        @Override public void onRender(DrawContext c) {
            if (mc.player == null) return;
            Hud.left(c, String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
    }

    public static class Direction extends Module {
        private static final String[] N = {"Po\u0142udnie", "Zach\u00f3d", "P\u00f3\u0142noc", "Wsch\u00f3d"};
        public Direction() { super("mods", "World", "Direction", "Kierunek patrzenia"); }
        @Override public void onRender(DrawContext c) {
            if (mc.player == null) return;
            float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
            int idx = ((int) Math.floor(yaw / 90f + 0.5f)) & 3;
            Hud.left(c, "Kierunek: " + N[idx] + " (" + Math.round(yaw) + "\u00b0)");
        }
    }

    public static class Clock extends Module {
        public Clock() { super("mods", "World", "Clock", "Aktualna godzina"); }
        @Override public void onRender(DrawContext c) { Hud.left(c, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))); }
    }

    public static class Ping extends Module {
        public Ping() { super("mods", "Combat", "Ping", "Op\u00f3\u017anienie serwera"); }
        @Override public void onRender(DrawContext c) {
            if (mc.player == null) return;
            String t = "--";
            if (mc.getNetworkHandler() != null) {
                var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) t = e.getLatency() + " ms";
            }
            Hud.left(c, "Ping: " + t);
        }
    }

    public static class Cps extends Module {
        private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
        private boolean lp, rp;
        public Cps() { super("mods", "Combat", "CPS Counter", "Klikni\u0119cia na sekund\u0119"); }
        @Override public void onRender(DrawContext c) {
            long now = Util.getMeasuringTimeMs();
... (Pozostałe wiersze: 523)
