package pl.elevenclients.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pl.elevenclients.gui.ElevenTitleScreen;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen elevenclients$replaceTitle(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class) {
            return new ElevenTitleScreen();
        }
        return screen;
    }
}
