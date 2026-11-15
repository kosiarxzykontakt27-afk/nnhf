package pl.elevenclients.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public final class SkinUtil {
    private SkinUtil() {}

    /** Skin aktywnego konta (prawdziwy) albo domyslny skin dla pozostalych nickow. */
    public static Identifier skin(String nick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (nick.equals(mc.getSession().getUsername())) {
                return mc.getSkinProvider().getSkinTextures(mc.getGameProfile()).texture();
            }
        } catch (Throwable ignored) { }
        return DefaultSkinHelper.getSkinTextures(AccountStore.offlineUuid(nick)).texture();
    }
}
