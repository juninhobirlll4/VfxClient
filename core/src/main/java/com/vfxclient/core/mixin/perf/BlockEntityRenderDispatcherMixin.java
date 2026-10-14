package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.BlockDetailsModule;
import com.vfxclient.core.module.performance.BlockEntityDistanceModule;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pula baús, placas, cabeças e banners além da distância configurada (geral ou por tipo). */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

	@Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;FI)V", at = @At("HEAD"), cancellable = true)
	private void vfx$blockEntityDistance(BlockEntity blockEntity, float tickDelta, int destroyProgress, CallbackInfo ci) {
		BlockEntityDistanceModule module = VfxCore.module(BlockEntityDistanceModule.class);
		if (module != null && module.shouldSkip(blockEntity)) {
			ci.cancel();
			return;
		}

		BlockDetailsModule details = VfxCore.module(BlockDetailsModule.class);
		if (details != null && details.shouldSkip(blockEntity)) {
			ci.cancel();
		}
	}
}
