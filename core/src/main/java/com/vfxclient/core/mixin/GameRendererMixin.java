package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.NoCloudsModule;
import com.vfxclient.core.module.visual.NoHurtCamModule;
import com.vfxclient.core.module.visual.ZoomModule;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom, câmera sem tremor ao levar dano e nuvens desligáveis. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Inject(method = "getFov(FZ)F", at = @At("RETURN"), cancellable = true)
	private void vfx$zoom(float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		ZoomModule zoom = VfxCore.module(ZoomModule.class);
		if (zoom != null && changingFov) {
			float multiplier = zoom.fovMultiplier();
			if (multiplier != 1F) {
				cir.setReturnValue(cir.getReturnValue() * multiplier);
			}
		}
	}

	@Inject(method = "bobViewWhenHurt(F)V", at = @At("HEAD"), cancellable = true)
	private void vfx$noHurtCam(float tickDelta, CallbackInfo ci) {
		NoHurtCamModule module = VfxCore.module(NoHurtCamModule.class);
		if (module != null && module.isEnabled()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void vfx$noClouds(CallbackInfo ci) {
		NoCloudsModule module = VfxCore.module(NoCloudsModule.class);
		if (module != null && module.isEnabled()) {
			ci.cancel();
		}
	}
}
