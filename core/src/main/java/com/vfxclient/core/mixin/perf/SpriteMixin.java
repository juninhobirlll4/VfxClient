package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Liga/desliga a animação de cada tipo de textura (água, lava, fogo, portal). */
@Mixin(Sprite.class)
public abstract class SpriteMixin {

	@Shadow
	public abstract String getName();

	@Inject(method = "update()V", at = @At("HEAD"), cancellable = true)
	private void vfx$animationByType(CallbackInfo ci) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks != null && !tweaks.animates(getName())) {
			ci.cancel();
		}
	}
}
