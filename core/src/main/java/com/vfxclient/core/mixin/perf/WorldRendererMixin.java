package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Céu desligável e limite de tempo para enviar chunks à placa de vídeo. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

	@Inject(method = "renderSky(FI)V", at = @At("HEAD"), cancellable = true)
	private void vfx$noSky(float tickDelta, int anaglyphFilter, CallbackInfo ci) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks != null && !tweaks.sky()) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "updateChunks(J)V", at = @At("HEAD"), argsOnly = true)
	private long vfx$chunkBudget(long limitTime) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks == null) {
			return limitTime;
		}
		long budget = tweaks.chunkBudgetNanos();
		return budget > 0 ? Math.min(limitTime, System.nanoTime() + budget) : limitTime;
	}
}
