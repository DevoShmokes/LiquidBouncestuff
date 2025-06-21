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

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FogRenderer.class)
public abstract class MixinBackgroundRenderer {

    @Shadow
    private static boolean fogEnabled;

    @ModifyVariable(method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;"))
    private static FogData injectFog(FogData original, Camera camera, int viewDistance, boolean thick, RenderTickCounter some, float f1, ClientWorld world) {
        if (!ModuleAntiBlind.INSTANCE.getRunning() || !fogEnabled) {
            return ModuleCustomAmbience.FogConfigurable.INSTANCE.modifyFog(camera, viewDistance, original);
        }

        CameraSubmersionType type = camera.getSubmersionType();

        if (!ModuleAntiBlind.canRender(DoRender.POWDER_SNOW_FOG) && type == CameraSubmersionType.POWDER_SNOW) {
            original.renderDistanceStart = -8.0F;
            original.renderDistanceEnd = viewDistance * 0.5F;
        }

        if (!ModuleAntiBlind.canRender(DoRender.LIQUIDS_FOG)) {
            // Renders fog same as spectator.
            switch (type) {
                case LAVA -> {
                    original.renderDistanceStart = -8.0F;
                    original.renderDistanceEnd = viewDistance * 0.5F;
                }
                case WATER -> {
                    original.renderDistanceStart = -8.0F;
                    original.renderDistanceEnd = viewDistance;
                }
            }
        }

        return original;
    }

    @ModifyVariable(method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;"))
    private static Vector4f injectFogColor(Vector4f original, Camera camera, int viewDistance, boolean thick, RenderTickCounter some, float f1, ClientWorld world) {
        if (!ModuleAntiBlind.INSTANCE.getRunning() || !fogEnabled) {
            return ModuleCustomAmbience.FogConfigurable.INSTANCE.getFogColor();
        }

        return original;
    }

}
