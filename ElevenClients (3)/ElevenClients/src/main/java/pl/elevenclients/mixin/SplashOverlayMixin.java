package pl.elevenclients.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.elevenclients.gui.ElevenSplash;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"), require = 0)
    private void elevenclients$splash(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ElevenSplash.draw(context);
    }
}
