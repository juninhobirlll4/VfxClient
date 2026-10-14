package com.vfxclient.core.mixin;

import com.vfxclient.core.emote.EmoteState;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pose dos emotes no modelo do jogador, depois da pose normal (andar, olhar). */
@Mixin(PlayerEntityModel.class)
public abstract class EmoteModelMixin {

	@Inject(method = "setAngles", at = @At("TAIL"))
	private void vfx$emotePose(float handSwing, float handSwingAmount, float tickDelta, float age, float headPitch,
			float scale, Entity entity, CallbackInfo ci) {
		if (entity instanceof PlayerEntity) {
			EmoteState.applyPose((PlayerEntityModel) (Object) this, (PlayerEntity) entity);
		}
	}
}
