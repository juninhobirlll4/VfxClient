package com.vfxclient.core.mixin;

import com.vfxclient.core.emote.EmoteState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** O braço em primeira pessoa usa o mesmo modelo: durante o desenho dele, o emote não vale. */
@Mixin(PlayerEntityRenderer.class)
public abstract class EmoteArmMixin {

	@Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("HEAD"))
	private void vfx$armBegin(AbstractClientPlayerEntity player, CallbackInfo ci) {
		EmoteState.setDrawingFirstPersonArm(true);
	}

	@Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("RETURN"))
	private void vfx$armEnd(AbstractClientPlayerEntity player, CallbackInfo ci) {
		EmoteState.setDrawingFirstPersonArm(false);
	}
}
