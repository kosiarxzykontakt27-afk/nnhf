package pl.elevenclients.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.elevenclients.ElevenClients;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void elevenclients$noHurtCam(CallbackInfo ci) {
        if (ElevenClients.modules != null && ElevenClients.modules.isOn("Hurt Cam")) ci.cancel();
    }
}
