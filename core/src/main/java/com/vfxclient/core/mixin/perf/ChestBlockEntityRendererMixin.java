package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.BlockDetailsModule;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tampa dos baús parada (fechada) quando a opção está ligada ({@link BlockDetailsModule}). */
@Mixin(ChestBlockEntityRenderer.class)
public abstract class ChestBlockEntityRendererMixin {

	@Inject(method = "render(Lnet/minecraft/block/entity/ChestBlockEntity;DDDFI)V", at = @At("HEAD"))
	private void vfx$freezeLid(ChestBlockEntity chest, double x, double y, double z, float tickDelta, int destroyProgress,
			CallbackInfo ci) {
		BlockDetailsModule module = VfxCore.module(BlockDetailsModule.class);
		if (module != null && module.freezeChests()) {
			chest.animationAngle = 0F;
			chest.animationAnglePrev = 0F;
		}
	}
}
