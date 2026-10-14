package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.FarPlayersModule;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nome em cima dos jogadores só até a distância escolhida ({@link FarPlayersModule}). */
@Mixin(EntityRenderer.class)
public abstract class EntityLabelMixin {

	@Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
	private void vfx$nametagDistance(Entity entity, String text, double x, double y, double z, int maxDistance, CallbackInfo ci) {
		FarPlayersModule module = VfxCore.module(FarPlayersModule.class);
		if (module != null && module.hideNametag(entity)) {
			ci.cancel();
		}
	}
}
