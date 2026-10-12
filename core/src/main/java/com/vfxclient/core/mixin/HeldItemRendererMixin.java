package com.vfxclient.core.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.visual.LowFireModule;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Abaixa o fogo que aparece na frente da câmera. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

	@Unique
	private boolean vfx$fireLowered;

	@Inject(method = "renderFireOverlay(F)V", at = @At("HEAD"))
	private void vfx$lowerFire(float tickDelta, CallbackInfo ci) {
		LowFireModule module = VfxCore.module(LowFireModule.class);
		vfx$fireLowered = module != null && module.isEnabled();
		if (vfx$fireLowered) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0F, -module.offset(), 0F);
		}
	}

	@Inject(method = "renderFireOverlay(F)V", at = @At("TAIL"))
	private void vfx$restoreFire(float tickDelta, CallbackInfo ci) {
		if (vfx$fireLowered) {
			GlStateManager.popMatrix();
			vfx$fireLowered = false;
		}
	}
}
