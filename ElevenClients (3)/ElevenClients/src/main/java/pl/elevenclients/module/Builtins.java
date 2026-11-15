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
import net.minecraft.util.math.Direction;
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
            long h = mc.getWindow().getHandle();
            boolean l = mc.currentScreen == null && GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean r = mc.currentScreen == null && GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (l && !lp) left.add(now);
            if (r && !rp) right.add(now);
            lp = l; rp = r;
            while (!left.isEmpty() && now - left.peekFirst() > 1000) left.pollFirst();
            while (!right.isEmpty() && now - right.peekFirst() > 1000) right.pollFirst();
            Hud.left(c, "CPS: " + left.size() + " | " + right.size());
        }
    }

    public static class Speed extends Module {
        public Speed() { super("mods", "World", "Speed HUD", "Pr\u0119dko\u015b\u0107 w blokach na sekund\u0119"); }
        @Override public void onRender(DrawContext c) {
            PlayerEntity p = mc.player;
            if (p == null) return;
            double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
            Hud.left(c, String.format(Locale.ROOT, "Pr\u0119dko\u015b\u0107: %.1f b/s", Math.sqrt(dx * dx + dz * dz) * 20));
        }
    }

    public static class Keystrokes extends Module {
        public Keystrokes() { super("mods", "Combat", "Keystrokes", "Wci\u015bni\u0119te klawisze (WASD, spacja)"); }
        private void key(DrawContext c, int x, int y, int w, String label, boolean down) {
            Draw.round(c, x, y, x + w, y + 18, 5, down ? 0xCC000000 | ElevenConfig.accent : 0x99090E1C);
            c.drawText(mc.textRenderer, label, x + w / 2 - mc.textRenderer.getWidth(label) / 2, y + 5, 0xFFFFFFFF, false);
        }
        @Override public void onRender(DrawContext c) {
            int x0 = 6, y0 = mc.getWindow().getScaledHeight() - 66;
            key(c, x0 + 20, y0, 18, "W", mc.options.forwardKey.isPressed());
            key(c, x0, y0 + 20, 18, "A", mc.options.leftKey.isPressed());
            key(c, x0 + 20, y0 + 20, 18, "S", mc.options.backKey.isPressed());
            key(c, x0 + 40, y0 + 20, 18, "D", mc.options.rightKey.isPressed());
            key(c, x0, y0 + 40, 58, "SPACE", mc.options.jumpKey.isPressed());
        }
    }

    public static class ArmorStatus extends Module {
        public ArmorStatus() { super("mods", "Player", "Armor Status", "Pancerz i wytrzyma\u0142o\u015b\u0107"); }
        @Override public void onRender(DrawContext c) {
            if (mc.player == null) return;
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int x = mc.getWindow().getScaledWidth() - 26, y = mc.getWindow().getScaledHeight() / 2 - 36;
            for (int i = 0; i < slots.length; i++) {
                ItemStack st = mc.player.getEquippedStack(slots[i]);
                if (st.isEmpty()) continue;
                Draw.round(c, x - 3, y + i * 20 - 2, x + 19, y + i * 20 + 18, 5, 0x99090E1C);
                c.drawItem(st, x, y + i * 20);
                c.drawStackOverlay(mc.textRenderer, st, x, y + i * 20);
            }
        }
    }

    public static class PotionStatus extends Module {
        public PotionStatus() { super("mods", "Player", "Potion Status", "Aktywne efekty"); }
        @Override public void onRender(DrawContext c) {
            if (mc.player == null) return;
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                int sec = e.getDuration() / 20;
                String name = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
                Hud.right(c, name + "  " + (sec / 60) + ":" + String.format("%02d", sec % 60));
            }
        }
    }

    public static class TotemCounter extends Module {
        public TotemCounter() { super("mods", "Player", "Totem Counter", "Ile totem\u00f3w zosta\u0142o"); }
        @Override public void onRender(DrawContext c) {
            if (mc.player == null) return;
            int n = 0;
            var inv = mc.player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (st.isOf(Items.TOTEM_OF_UNDYING)) n += st.getCount();
            }
            Hud.right(c, "Totemy: " + n);
        }
    }

    public static class Crosshair extends Module {
        public Crosshair() { super("visual", "Visual", "Crosshair", "W\u0142asny celownik"); }
        @Override public void onRender(DrawContext c) {
            if (mc.currentScreen != null || mc.player == null) return;
            int cx = mc.getWindow().getScaledWidth() / 2, cy = mc.getWindow().getScaledHeight() / 2;
            int size = Math.round(val(0)), gap = Math.round(val(1)), col = accent();
            c.fill(cx - gap - size, cy - 1, cx - gap, cy + 1, col);
            c.fill(cx + gap, cy - 1, cx + gap + size, cy + 1, col);
            c.fill(cx - 1, cy - gap - size, cx + 1, cy - gap, col);
            c.fill(cx - 1, cy + gap, cx + 1, cy + gap + size, col);
        }
    }

    /* ===================== ruch ===================== */

    public static class ToggleSprint extends Module {
        public ToggleSprint() { super("mods", "Movement", "Toggle Sprint", "Sprint bez trzymania"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null) return;
            if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                    && !p.isUsingItem() && p.getHungerManager().getFoodLevel() > 6) {
                p.setSprinting(true);
            }
        }
    }

    public static class ToggleSneak extends Module {
        public ToggleSneak() { super("mods", "Movement", "Toggle Sneak", "Skradanie bez trzymania"); }
        @Override public void onTick() { if (mc.player != null) mc.options.sneakKey.setPressed(true); }
        @Override public void onDisable() { mc.options.sneakKey.setPressed(false); }
    }

    public static class Zoom extends Module {
        private final KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elevenclients.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.elevenclients"));
        private boolean zoomed;
        private int orig;

        public Zoom() { super("mods", "Movement", "Zoom", "Przybli\u017cenie (trzymaj C, FOV min. 30)"); }

        private void restore() {
            if (zoomed) { mc.options.getFov().setValue(orig); zoomed = false; }
        }

        @Override public void onTick() {
            if (mc.player == null) return;
            boolean down = key.isPressed() && mc.currentScreen == null;
            if (down && !zoomed) { orig = mc.options.getFov().getValue(); zoomed = true; }
            if (down) {
                int f = Math.round(orig / Math.max(1f, val(0)));
                mc.options.getFov().setValue(Math.max(30, Math.min(110, f)));
            } else restore();
        }

        @Override public void onDisable() { restore(); }
    }

    /* ===================== swiat / wyglad ===================== */

    public static class Fullbright extends Module {
        public Fullbright() { super("visual", "Visual", "Fullbright", "Pe\u0142na jasno\u015b\u0107"); }
        @Override public void onTick() {
            if (mc.player == null) return;
            StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 600) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, false, false));
            }
        }
        @Override public void onDisable() {
            if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    /** Znaczniki - dzialanie robia mixiny (HurtCam / FireOverlay / Crosshair). */
    public static class HurtCam extends Module {
        public HurtCam() { super("visual", "Visual", "Hurt Cam", "Bez trz\u0119sienia kamery po obra\u017ceniach"); }
    }

    public static class FireOverlay extends Module {
        public FireOverlay() { super("visual", "Visual", "Fire Overlay", "Bez ognia na ekranie"); }
    }

    public static class TimeChanger extends Module {
        public TimeChanger() { super("visual", "Mods", "Time Changer", "W\u0142asna pora dnia"); }
        @Override public void onTick() {
            if (mc.world == null) return;
            long ticks = ((Math.round(val(0)) - 6 + 24) % 24) * 1000L;
            mc.world.setTime(mc.world.getTime(), ticks, false);
        }
    }

    public static class WeatherChanger extends Module {
        public WeatherChanger() { super("visual", "Mods", "Weather Changer", "Bez deszczu i burz"); }
        @Override public void onTick() {
            if (mc.world == null) return;
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }
    }

    /* ===================== NOWE MODULY ===================== */

    /** Kierunek ruchu z klawiszy WASD (wektor jednostkowy w poziomie) albo zero. */
    private static Vec3d moveDir(ClientPlayerEntity p) {
        double f = (mc().options.forwardKey.isPressed() ? 1 : 0) - (mc().options.backKey.isPressed() ? 1 : 0);
        double sd = (mc().options.leftKey.isPressed() ? 1 : 0) - (mc().options.rightKey.isPressed() ? 1 : 0);
        if (f == 0 && sd == 0) return Vec3d.ZERO;
        double yaw = Math.toRadians(p.getYaw());
        double dx = -Math.sin(yaw) * f + Math.cos(yaw) * sd;
        double dz = Math.cos(yaw) * f + Math.sin(yaw) * sd;
        double len = Math.sqrt(dx * dx + dz * dz);
        return new Vec3d(dx / len, 0, dz / len);
    }

    private static net.minecraft.client.MinecraftClient mc() { return net.minecraft.client.MinecraftClient.getInstance(); }

    /* ---------- Combat ---------- */

    /** Powiekszone hitboxy graczy - dziala przez mixin (EntityMixin). */
    public static class Hitboxes extends Module {
        public Hitboxes() { super("mods", "Combat", "Hitboxes", "Powi\u0119kszone hitboxy graczy"); }
    }

    public static class TriggerBot extends Module {
        public TriggerBot() { super("mods", "Combat", "TriggerBot", "Auto-atak, gdy celownik jest na celu"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;
            if (mc.crosshairTarget instanceof EntityHitResult ehr) {
                Entity e = ehr.getEntity();
                if (e instanceof LivingEntity && p.getAttackCooldownProgress(0.5f) >= 0.95f) {
                    mc.interactionManager.attackEntity(p, e);
                    p.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }

    public static class KillAura extends Module {
        public KillAura() { super("mods", "Combat", "KillAura", "Auto-atak najbli\u017cszych graczy i mob\u00f3w"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
            if (p.getAttackCooldownProgress(0.5f) < 0.95f) return;
            double range = val(0);
            Entity best = null;
            double bd = range + 1;
            for (Entity e : mc.world.getEntities()) {
                if (e == p || !(e instanceof PlayerEntity || e instanceof HostileEntity)) continue;
                if (!((LivingEntity) e).isAlive()) continue;
                double d = p.distanceTo(e);
                if (d <= range && d < bd) { best = e; bd = d; }
            }
            if (best != null) {
                mc.interactionManager.attackEntity(p, best);
                p.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    public static class Velocity extends Module {
        public Velocity() { super("mods", "Combat", "Velocity", "Mniejszy odrzut po trafieniu"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null) return;
            if (p.hurtTime == 9) {
                Vec3d v = p.getVelocity();
                float f = val(0) / 100f;
                p.setVelocity(v.x * f, v.y, v.z * f);
            }
        }
    }

    /* ---------- Player ---------- */

    public static class AutoRespawn extends Module {
        public AutoRespawn() { super("mods", "Player", "AutoRespawn", "Automatyczne odradzanie po \u015bmierci"); }
        @Override public void onTick() {
            if (mc.player != null && mc.currentScreen instanceof DeathScreen) mc.player.requestRespawn();
        }
    }

    public static class AutoTotem extends Module {
        private int cd;
        public AutoTotem() { super("mods", "Player", "AutoTotem", "Totem do drugiej r\u0119ki"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;
            if (cd > 0) { cd--; return; }
            if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
            var inv = p.getInventory();
            for (int i = 0; i < 36; i++) {
                if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                    int slot = i < 9 ? 36 + i : i;
                    int id = p.playerScreenHandler.syncId;
                    mc.interactionManager.clickSlot(id, slot, 0, SlotActionType.PICKUP, p);
                    mc.interactionManager.clickSlot(id, 45, 0, SlotActionType.PICKUP, p);
                    mc.interactionManager.clickSlot(id, slot, 0, SlotActionType.PICKUP, p);
                    cd = 8;
                    return;
                }
            }
        }
    }

    public static class Scaffold extends Module {
        public Scaffold() { super("mods", "Player", "Scaffold", "Stawia bloki pod nogami"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.world == null || mc.interactionManager == null) return;
            if (!(p.getMainHandStack().getItem() instanceof BlockItem)) return;
            BlockPos below = p.getBlockPos().down();
            if (!mc.world.getBlockState(below).isAir()) return;
            for (Direction d : Direction.values()) {
                BlockPos n = below.offset(d);
                if (mc.world.getBlockState(n).isAir()) continue;
                Direction face = d.getOpposite();
                Vec3d pos = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(pos, face, n, false));
                p.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }

    public static class Nuker extends Module {
        public Nuker() { super("mods", "Player", "Nuker", "Automatycznie niszczy pobliskie bloki"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
            int r = Math.max(1, Math.round(val(0)));
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            Vec3d eye = p.getEyePos();
            for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
                BlockPos pos = p.getBlockPos().add(dx, dy, dz);
                var st = mc.world.getBlockState(pos);
                if (st.isAir() || st.getHardness(mc.world, pos) < 0) continue;
                double d = pos.toCenterPos().squaredDistanceTo(eye);
                if (d < bd) { bd = d; best = pos; }
            }
            if (best != null) {
                mc.interactionManager.updateBlockBreakingProgress(best, Direction.UP);
                p.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    /* ---------- Movement ---------- */

    public static class Fly extends Module {
        public Fly() { super("mods", "Movement", "Fly", "Latanie (jak w trybie kreatywnym)"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null) return;
            p.getAbilities().allowFlying = true;
            p.getAbilities().flying = true;
            p.getAbilities().setFlySpeed(val(0) / 20f);
        }
        @Override public void onDisable() {
            var p = mc.player;
            if (p == null || p.isCreative() || p.isSpectator()) return;
            p.getAbilities().allowFlying = false;
            p.getAbilities().flying = false;
            p.getAbilities().setFlySpeed(0.05f);
        }
    }

    public static class SpeedBoost extends Module {
        public SpeedBoost() { super("mods", "Movement", "SpeedBoost", "Szybsze bieganie"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null || p.isTouchingWater()) return;
            Vec3d d = moveDir(p);
            if (d == Vec3d.ZERO) return;
            double sp = val(0) * 0.1;
            Vec3d v = p.getVelocity();
            p.setVelocity(d.x * sp, v.y, d.z * sp);
        }
    }

    public static class HighJump extends Module {
        public HighJump() { super("mods", "Movement", "HighJump", "Wy\u017cszy skok"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null) return;
            if (p.isOnGround() && mc.options.jumpKey.isPressed()) {
                Vec3d v = p.getVelocity();
                p.setVelocity(v.x, 0.42 * val(0), v.z);
            }
        }
    }

    public static class Jesus extends Module {
        public Jesus() { super("mods", "Movement", "Jesus", "Chodzenie po wodzie"); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null) return;
            if (p.isTouchingWater() && !p.isSneaking() && !mc.options.jumpKey.isPressed()) {
                Vec3d v = p.getVelocity();
                p.setVelocity(v.x, 0.1, v.z);
            }
        }
    }

    public static class AutoWalk extends Module {
        public AutoWalk() { super("mods", "Movement", "AutoWalk", "Ci\u0105g\u0142e chodzenie do przodu"); }
        @Override public void onTick() { if (mc.player != null) mc.options.forwardKey.setPressed(true); }
        @Override public void onDisable() { mc.options.forwardKey.setPressed(false); }
    }

    public static class AntiAfk extends Module {
        private int c;
        public AntiAfk() { super("mods", "Movement", "AntiAFK", "Skacze co jaki\u015b czas, \u017ceby nie wyrzuci\u0142o"); }
        @Override public void onTick() {
            if (mc.player == null) return;
            c++;
            if (c % 400 == 0) mc.options.jumpKey.setPressed(true);
            if (c % 400 == 3) mc.options.jumpKey.setPressed(false);
            if (c % 600 == 0) mc.player.setYaw(mc.player.getYaw() + 7f);
        }
    }

    public static class InvMove extends Module {
        public InvMove() { super("mods", "Movement", "InvMove", "Chodzenie z otwartym ekwipunkiem"); }
        @Override public void onTick() {
            if (mc.player == null || !(mc.currentScreen instanceof HandledScreen<?>)) return;
            long h = mc.getWindow().getHandle();
            KeyBinding[] keys = {mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey};
            for (KeyBinding k : keys) {
                int code = KeyBindingHelper.getBoundKeyOf(k).getCode();
                k.setPressed(InputUtil.isKeyPressed(h, code));
            }
        }
    }

    public static class AutoJump extends Module {
        public AutoJump() { super("mods", "Movement", "AutoJump", "Automatyczny skok na blok"); }
        @Override public void onTick() { mc.options.getAutoJump().setValue(true); }
        @Override public void onDisable() { mc.options.getAutoJump().setValue(false); }
    }

    /* ---------- World / HUD ---------- */

    public static class BiomeHud extends Module {
        public BiomeHud() { super("mods", "World", "Biome", "Aktualny biom"); }
        @Override public void onRender(DrawContext c) {
            if (mc.player == null || mc.world == null) return;
            String n = mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("?");
            Hud.left(c, "Biom: " + n);
        }
    }

    public static class DayCounter extends Module {
        public DayCounter() { super("visual", "HUD", "Day Counter", "Kt\u00f3ry dzie\u0144 w \u015bwiecie"); }
        @Override public void onRender(DrawContext c) {
            if (mc.world == null) return;
            Hud.left(c, "Dzie\u0144: " + (mc.world.getTimeOfDay() / 24000L));
        }
    }

    public static class MemoryHud extends Module {
        public MemoryHud() { super("visual", "HUD", "Memory", "U\u017cycie pami\u0119ci RAM"); }
        @Override public void onRender(DrawContext c) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            Hud.left(c, "RAM: " + used + " / " + (rt.maxMemory() / 1048576L) + " MB");
        }
    }

    /* ---------- Visual ---------- */

    public static class Esp extends Module {
        public Esp() { super("visual", "Visual", "ESP", "Pod\u015bwietla graczy przez \u015bciany"); }
        @Override public void onTick() {
            if (mc.world == null) return;
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof PlayerEntity && e != mc.player) e.setGlowing(true);
            }
        }
        @Override public void onDisable() {
            if (mc.world == null) return;
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof PlayerEntity && e != mc.player) e.setGlowing(false);
            }
        }
    }

    public static class ItemEsp extends Module {
        public ItemEsp() { super("visual", "Visual", "Item ESP", "Pod\u015bwietla przedmioty na ziemi"); }
        @Override public void onTick() {
            if (mc.world == null) return;
            for (Entity e : mc.world.getEntities()) if (e instanceof ItemEntity) e.setGlowing(true);
        }
        @Override public void onDisable() {
            if (mc.world == null) return;
            for (Entity e : mc.world.getEntities()) if (e instanceof ItemEntity) e.setGlowing(false);
        }
    }

    public static class HitboxRender extends Module {
        public HitboxRender() { super("visual", "Visual", "Hitbox Render", "Pokazuje hitboxy (jak F3+B)"); }
        @Override public void onTick() { mc.getEntityRenderDispatcher().setRenderHitboxes(true); }
        @Override public void onDisable() { mc.getEntityRenderDispatcher().setRenderHitboxes(false); }
    }

    public static class NoBob extends Module {
        public NoBob() { super("visual", "Visual", "No Bob", "Wy\u0142\u0105cza ko\u0142ysanie kamery przy chodzeniu"); }
        @Override public void onTick() { mc.options.getBobView().setValue(false); }
        @Override public void onDisable() { mc.options.getBobView().setValue(true); }
    }

    /* ---------- Spotify ---------- */

    public static class Spotify extends Module {
        private final KeyBinding playK = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elevenclients.spotify.play", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_5, "category.elevenclients"));
        private final KeyBinding nextK = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elevenclients.spotify.next", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_6, "category.elevenclients"));
        private final KeyBinding prevK = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elevenclients.spotify.prev", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_4, "category.elevenclients"));
        private final KeyBinding openK = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elevenclients.spotify.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.elevenclients"));

        public Spotify() { super("visual", "HUD", "Spotify", "Co gra + sterowanie (klawisz O)"); }

        @Override public void onTick() {
            while (playK.wasPressed()) pl.elevenclients.gui.SpotifyControl.playPause();
            while (nextK.wasPressed()) pl.elevenclients.gui.SpotifyControl.next();
            while (prevK.wasPressed()) pl.elevenclients.gui.SpotifyControl.previous();
            while (openK.wasPressed()) mc.setScreen(new pl.elevenclients.gui.SpotifyScreen(mc.currentScreen));
        }

        @Override public void onRender(DrawContext c) {
            pl.elevenclients.gui.SpotifyControl.poll();
            String t = pl.elevenclients.gui.SpotifyControl.nowPlaying;
            if (t.isEmpty()) return;
            if (t.length() > 44) t = t.substring(0, 43) + "...";
            Hud.left(c, "Spotify: " + t);
        }
    }
}
