package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.FarPlayersModule;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pula capa, armadura, item na mão e flechas presas dos jogadores longe ({@link FarPlayersModule}). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityFeaturesMixin {

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Redirect(method = "renderFeatures", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/entity/LivingEntity;FFFFFFF)V"))
	private void vfx$farFeatures(FeatureRenderer feature, LivingEntity entity, float handSwing, float handSwingAmount,
			float tickDelta, float age, float headYaw, float headPitch, float scale) {
		FarPlayersModule module = VfxCore.module(FarPlayersModule.class);
		if (module == null || !module.skipFeature(feature, entity)) {
			feature.render(entity, handSwing, handSwingAmount, tickDelta, age, headYaw, headPitch, scale);
		}
	}
}
