package pl.elevenclients.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.elevenclients.ElevenClients;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void elevenclients$noFire(CallbackInfo ci) {
        if (ElevenClients.modules != null && ElevenClients.modules.isOn("Fire Overlay")) ci.cancel();
    }
}
