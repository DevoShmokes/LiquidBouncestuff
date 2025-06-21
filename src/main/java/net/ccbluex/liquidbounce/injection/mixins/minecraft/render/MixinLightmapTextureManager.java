/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.interfaces.LightmapTextureManagerAddition;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class MixinLightmapTextureManager implements LightmapTextureManagerAddition {

    @Shadow
    @Final
    private GpuTextureView glTextureView;
    @Unique
    private boolean liquid_bounce$customLightMap = false;

    @ModifyExpressionValue(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 1))
    private Object injectXRayFullBright(Object original) {
        // If fullBright is enabled, we need to return our own gamma value
        if (ModuleFullBright.FullBrightGamma.INSTANCE.getRunning()) {
            return ModuleFullBright.FullBrightGamma.INSTANCE.getGamma();
        }

        // Xray fullbright
        final ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning() || !module.getFullBright()) {
            return original;
        }

        // They use .floatValue() afterward on the return value,
        // so we need to return a value which is not bigger than Float.MAX_VALUE
        return (double) Float.MAX_VALUE;
    }

    @Inject(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V"))
    private void hookBlendTextureColors(float delta, CallbackInfo ci) {
        var lightColor = ModuleCustomAmbience.CustomLightColor.INSTANCE;
        if (lightColor.getRunning()) {
            lightColor.update();
        }
    }

    @Inject(method = "update(F)V", at = @At(value = "HEAD"))
    private void hookResetIndex(float delta, CallbackInfo ci) {
        var customLightColor = ModuleCustomAmbience.CustomLightColor.INSTANCE;
        if (customLightColor.getRunning()) {
            liquid_bounce$customLightMap = true;
            if (RenderSystem.getShaderTexture(2) == this.glTextureView) {
                RenderSystem.setShaderTexture(2, customLightColor.getTexture());
            }
        }
    }

    @Inject(method = "enable", at = @At("HEAD"), cancellable = true)
    private void hookSpoof(CallbackInfo ci) {
        if (liquid_bounce$customLightMap) {
            RenderSystem.setShaderTexture(2, ModuleCustomAmbience.CustomLightColor.INSTANCE.getTexture());
            ci.cancel();
        }
    }

    @Unique
    @Override
    public void liquid_bounce$restoreLightMap() {
        if (RenderSystem.getShaderTexture(2) != null) {
            RenderSystem.setShaderTexture(2, this.glTextureView);
        }
        liquid_bounce$customLightMap = false;
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void hookClose(CallbackInfo ci) {
        ModuleCustomAmbience.CustomLightColor.INSTANCE.close();
    }

    // Turns off blinking when the darkness effect is active.
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"))
    private float injectAntiDarkness(float original) {
        if (!ModuleAntiBlind.canRender(DoRender.DARKNESS)) {
            return 0.0F;
        }

        return original;
    }

}
