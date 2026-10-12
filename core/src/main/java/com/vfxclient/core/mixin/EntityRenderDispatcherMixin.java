package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.EntityDistanceModule;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pula o desenho de entidades além da distância configurada. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void vfx$entityDistance(Entity entity, CameraView cameraView, double x, double y, double z,
			CallbackInfoReturnable<Boolean> cir) {
		EntityDistanceModule module = VfxCore.module(EntityDistanceModule.class);
		if (module != null && module.shouldSkip(entity)) {
			cir.setReturnValue(false);
		}
	}
}
