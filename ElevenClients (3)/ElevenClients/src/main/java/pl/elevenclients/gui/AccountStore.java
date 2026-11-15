package pl.elevenclients.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * MultiK - zapamietane konta: glowne, premium (Microsoft) i offline.
 * Pliki: config/elevenclients-accounts.txt (offline), config/elevenclients-premium.json (premium + tokeny - nie udostepniaj!).
 */
public final class AccountStore {
    public static final class Entry {
        public final String name;
        public final boolean premium, original;
        public Entry(String name, boolean premium, boolean original) {
            this.name = name; this.premium = premium; this.original = original;
        }
    }

    private static final List<String> offline = new ArrayList<>();
    /** nick -> {uuid, jsonSesji} */
    private static final Map<String, String[]> premium = new LinkedHashMap<>();
    private static String originalName;
    private static Session originalSession;
    private static boolean loaded;

    private AccountStore() {}

    private static Path offlineFile() { return FabricLoader.getInstance().getConfigDir().resolve("elevenclients-accounts.txt"); }
    private static Path premiumFile() { return FabricLoader.getInstance().getConfigDir().resolve("elevenclients-premium.json"); }

    private static synchronized void ensure() {
        if (loaded) return;
        loaded = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        originalSession = mc.getSession();
        originalName = originalSession.getUsername();
        try {
            Path p = offlineFile();
            if (Files.exists(p)) for (String l : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                l = l.trim();
                if (!l.isEmpty() && !offline.contains(l)) offline.add(l);
            }
        } catch (Exception ignored) { }
        try {
            Path p = premiumFile();
            if (Files.exists(p)) {
                JsonObject root = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String k : root.keySet()) {
                    JsonObject o = root.getAsJsonObject(k);
                    premium.put(k, new String[]{o.get("uuid").getAsString(), o.get("session").getAsString()});
                }
            }
        } catch (Exception ignored) { }
    }

    public static synchronized List<Entry> all() {
        ensure();
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(originalName, false, true));
        for (String n : premium.keySet()) if (!n.equals(originalName)) out.add(new Entry(n, true, false));
        for (String n : offline) if (!n.equals(originalName) && !premium.containsKey(n)) out.add(new Entry(n, false, false));
        return out;
    }

    public static synchronized boolean addOffline(String nick) {
        ensure();
        if (!nick.matches("[A-Za-z0-9_]{3,16}")) return false;
        for (Entry e : all()) if (e.name.equalsIgnoreCase(nick)) return false;
        offline.add(nick);
        saveOffline();
        return true;
    }

    public static synchronized void addPremium(String name, UUID uuid, String sessionJson) {
        ensure();
        premium.put(name, new String[]{uuid.toString(), sessionJson});
        offline.remove(name);
        savePremium();
        saveOffline();
    }

    public static synchronized String premiumSession(String name) {
        ensure();
        String[] v = premium.get(name);
        return v == null ? null : v[1];
    }

    public static synchronized UUID premiumUuid(String name) {
        ensure();
        String[] v = premium.get(name);
        return v == null ? null : UUID.fromString(v[0]);
    }

    public static synchronized void updatePremiumSession(String name, String sessionJson) {
        ensure();
        String[] v = premium.get(name);
        if (v != null) { v[1] = sessionJson; savePremium(); }
    }

    public static synchronized void remove(Entry e) {
        ensure();
        if (e.original) return;
        if (e.premium) { premium.remove(e.name); savePremium(); }
        else { offline.remove(e.name); saveOffline(); }
    }

    private static void saveOffline() {
        try { Files.write(offlineFile(), offline, StandardCharsets.UTF_8); } catch (Exception ignored) { }
    }

    private static void savePremium() {
        try {
            JsonObject root = new JsonObject();
            for (Map.Entry<String, String[]> e : premium.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", e.getValue()[0]);
                o.addProperty("session", e.getValue()[1]);
                root.add(e.getKey(), o);
            }
            Files.writeString(premiumFile(), new Gson().toJson(root), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    public static UUID offlineUuid(String nick) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8));
    }

    public static String lastError = "";

    /** Ustawia aktywna sesje klienta - pole szukane po TYPIE (dziala niezaleznie od mapowan). */
    public static boolean applySession(Session s) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            for (Field f : MinecraftClient.class.getDeclaredFields()) {
                if (f.getType() == Session.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    f.set(mc, s);
                    lastError = "";
                    resetProfileKeys(mc);
                    return true;
                }
            }
            lastError = "Nie znaleziono pola sesji";
        } catch (Throwable t) {
            lastError = t.toString();
        }
        return false;
    }

    /**
     * Po zmianie konta klucze czatu zostaja z poprzedniego konta i serwery potrafia wyrzucic
     * ("Invalid signature for profile public key"). Podmieniamy je na pusty wariant (jak dla kont offline).
     * Dzialaja serwery bez wymuszonego podpisu czatu (czyli prawie wszystkie).
     */
    private static void resetProfileKeys(MinecraftClient mc) {
        try {
            var r = FabricLoader.getInstance().getMappingResolver();
            Class<?> pk = Class.forName(r.mapClassName("named", "net.minecraft.client.session.ProfileKeys"),
                    false, MinecraftClient.class.getClassLoader());
            Object missing = null;
            for (Field f : pk.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == pk) {
                    f.setAccessible(true);
                    missing = f.get(null);
                    break;
                }
            }
            if (missing == null) return;
            for (Field f : MinecraftClient.class.getDeclaredFields()) {
                if (f.getType() == pk && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    f.set(mc, missing);
                }
            }
        } catch (Throwable t) {
            lastError = "klucze czatu: " + t;
        }
    }

    public static boolean applyPremium(String name, UUID uuid, String token) {
        return applySession(new Session(name, uuid, token, Optional.empty(), Optional.empty(), Session.AccountType.MSA));
    }

    /** Konto glowne (to z launchera) albo offline. Premium przelacza PremiumAuth. */
    public static boolean switchTo(Entry e) {
        ensure();
        if (e.original) return applySession(originalSession);
        return applySession(new Session(e.name, offlineUuid(e.name), "", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));
    }
}
