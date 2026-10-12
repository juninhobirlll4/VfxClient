package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Congela as texturas animadas (água, lava, fogo, portal) quando desligadas. */
@Mixin(SpriteAtlasTexture.class)
public abstract class SpriteAtlasTextureMixin {

	@Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
	private void vfx$skipAnimations(CallbackInfo ci) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks != null && !tweaks.animatedTextures()) {
			ci.cancel();
		}
	}
}
