package pl.elevenclients.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ElevenConfig {
    /** Skala calego GUI klienta (menu glowne, ekran modow itd.). 1.0 = normalna. */
    public static float guiScale = 0.65f;
    /** Kolor akcentu 0xRRGGBB. */
    public static int accent = 0x0EA5E9;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("elevenclients.properties");
    }

    public static void load() {
        try {
            Path p = file();
            if (!Files.exists(p)) { save(); return; }
            Properties pr = new Properties();
            try (InputStream in = Files.newInputStream(p)) { pr.load(in); }
            guiScale = Float.parseFloat(pr.getProperty("guiScale", "0.65"));
            accent = Integer.parseInt(pr.getProperty("accent", "0EA5E9"), 16);
        } catch (Exception ignored) { }
    }

    public static void save() {
        try {
            Properties pr = new Properties();
            pr.setProperty("guiScale", Float.toString(guiScale));
            pr.setProperty("accent", String.format("%06X", accent));
            try (OutputStream out = Files.newOutputStream(file())) { pr.store(out, "ElevenClients"); }
        } catch (Exception ignored) { }
    }
}
