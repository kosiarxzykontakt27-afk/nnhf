package pl.elevenclients.gui;

import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.net.URI;

/** Logowanie kontem Microsoft (kod urzadzenia, jak w launcherach) i przelaczanie kont premium. */
public final class PremiumAuth {
    public static volatile String status = "";
    public static volatile String codeUrl = null;
    public static volatile boolean busy = false;

    private PremiumAuth() {}

    public static void openLink() {
        String u = codeUrl;
        if (u == null) return;
        try { Util.getOperatingSystem().open(URI.create(u)); } catch (Throwable ignored) { }
    }

    /** Dodaje nowe konto premium: pokazuje kod, uzytkownik loguje sie w przegladarce. */
    public static void login(Runnable onDone) {
        if (busy) return;
        busy = true;
        status = "\u0141\u0105czenie z Microsoft...";
        Thread t = new Thread(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            try {
                var http = MinecraftAuth.createHttpClient();
                var session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(http,
                        new StepMsaDeviceCode.MsaDeviceCodeCallback(code -> {
                            codeUrl = code.getDirectVerificationUri();
                            status = "Wpisz kod " + code.getUserCode() + " (skopiowany). Kliknij, aby otworzy\u0107 link.";
                            mc.execute(() -> mc.keyboard.setClipboard(code.getUserCode()));
                            openLink();
                        }));
                String name = session.getMcProfile().getName();
                AccountStore.addPremium(name, session.getMcProfile().getId(),
                        MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session).toString());
                status = "Dodano konto premium: " + name;
            } catch (Throwable e) {
                status = "B\u0142\u0105d logowania: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } finally {
                busy = false;
                codeUrl = null;
                mc.execute(onDone);
            }
        }, "ElevenClients-MSA");
        t.setDaemon(true);
        t.start();
    }

    /** Przelacza na zapisane konto premium (odswieza token). */
    public static void switchTo(String name, Runnable onDone) {
        if (busy) return;
        String json = AccountStore.premiumSession(name);
        if (json == null) { status = "Brak zapisanej sesji dla " + name; return; }
        busy = true;
        status = "Od\u015bwie\u017canie sesji " + name + "...";
        Thread t = new Thread(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            try {
                var http = MinecraftAuth.createHttpClient();
                var session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(JsonParser.parseString(json).getAsJsonObject());
                session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(http, session);
                AccountStore.updatePremiumSession(name, MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session).toString());
                boolean ok = AccountStore.applyPremium(session.getMcProfile().getName(), session.getMcProfile().getId(),
                        session.getMcProfile().getMcToken().getAccessToken());
                status = ok ? "Aktywne konto: " + name : "Nie uda\u0142o si\u0119 zmieni\u0107 sesji";
            } catch (Throwable e) {
                status = "B\u0142\u0105d: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } finally {
                busy = false;
                mc.execute(onDone);
            }
        }, "ElevenClients-MSA");
        t.setDaemon(true);
        t.start();
    }
}
