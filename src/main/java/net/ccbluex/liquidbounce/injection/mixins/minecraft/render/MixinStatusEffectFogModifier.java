package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectFogModifier.class)
public abstract class MixinStatusEffectFogModifier {

    @Shadow
    public abstract RegistryEntry<StatusEffect> getStatusEffect();

    @Inject(method = "shouldApply", at = @At("HEAD"), cancellable = true)
    private void injectAntiBlind(CameraSubmersionType submersionType, Entity cameraEntity, CallbackInfoReturnable<Boolean> cir) {
        boolean cancel = false;

        if (this.getStatusEffect() == StatusEffects.BLINDNESS || ModuleAntiBlind.canRender(DoRender.BLINDING)) {
            cancel = true;
        } else if (this.getStatusEffect() == StatusEffects.DARKNESS || ModuleAntiBlind.canRender(DoRender.DARKNESS)) {
            cancel = true;
        }

        if (cancel) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}
