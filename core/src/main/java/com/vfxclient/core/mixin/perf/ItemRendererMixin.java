package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Brilho de encantamento desligável em itens (mão, inventário, chão). */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

	@Inject(method = "renderGlint", at = @At("HEAD"), cancellable = true)
	private void vfx$noGlint(CallbackInfo ci) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks != null && !tweaks.glint()) {
			ci.cancel();
		}
	}
}
