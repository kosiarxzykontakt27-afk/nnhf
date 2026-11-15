package pl.elevenclients.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.elevenclients.ElevenClients;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTargetingMargin()F", at = @At("HEAD"), cancellable = true, require = 0)
    private void elevenclients$hitboxes(CallbackInfoReturnable<Float> cir) {
        if (ElevenClients.modules == null || !ElevenClients.modules.isOn("Hitboxes")) return;
        Object self = this;
        if (self instanceof PlayerEntity && self != MinecraftClient.getInstance().player) {
            cir.setReturnValue(ElevenClients.modules.value("Hitboxes", 0));
        }
    }
}
