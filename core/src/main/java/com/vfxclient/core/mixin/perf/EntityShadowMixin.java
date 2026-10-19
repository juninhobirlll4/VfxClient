package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.EngineModule;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sombra de entidades desligável ({@link EngineModule}). */
@Mixin(EntityRenderer.class)
public abstract class EntityShadowMixin {

	@Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
	private void vfx$noShadow(Entity entity, double x, double y, double z, float f, float tickDelta, CallbackInfo ci) {
		EngineModule engine = VfxCore.module(EngineModule.class);
		if (engine != null && engine.noShadows()) {
			ci.cancel();
		}
	}
}
