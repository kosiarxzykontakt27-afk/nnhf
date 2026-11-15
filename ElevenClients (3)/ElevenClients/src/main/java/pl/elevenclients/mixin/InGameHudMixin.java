package pl.elevenclients.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.elevenclients.ElevenClients;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void elevenclients$customCrosshair(CallbackInfo ci) {
        if (ElevenClients.modules != null && ElevenClients.modules.isOn("Crosshair")) ci.cancel();
    }
}
