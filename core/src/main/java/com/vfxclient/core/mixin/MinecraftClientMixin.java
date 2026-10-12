package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.vfxclient.core.module.performance.FpsLimiterModule;

/** Repassa o tick do cliente (20 por segundo) para o VfxClient. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void vfx$onTick(CallbackInfo ci) {
		VfxCore core = VfxCore.get();
		if (core != null) {
			core.onClientTick((MinecraftClient) (Object) this);
		}
	}

	@Inject(method = "getMaxFramerate()I", at = @At("RETURN"), cancellable = true)
	private void vfx$limitUnfocused(CallbackInfoReturnable<Integer> cir) {
		FpsLimiterModule module = VfxCore.module(FpsLimiterModule.class);
		if (module != null) {
			int limited = module.adjust(cir.getReturnValue());
			if (limited != cir.getReturnValue()) {
				cir.setReturnValue(limited);
			}
		}
	}
}
