package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.FarPlayersModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Esconde as camadas da skin dos jogadores longe ({@link FarPlayersModule}). */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

	@Inject(method = "setModelPose", at = @At("TAIL"))
	private void vfx$farSkinLayers(AbstractClientPlayerEntity player, CallbackInfo ci) {
		FarPlayersModule module = VfxCore.module(FarPlayersModule.class);
		if (module == null || !module.hideSkinLayers(player)) {
			return;
		}

		PlayerEntityModel model = ((PlayerEntityRenderer) (Object) this).getModel();
		model.hat.visible = false;
		model.jacket.visible = false;
		model.leftSleeve.visible = false;
		model.rightSleeve.visible = false;
		model.leftPants.visible = false;
		model.rightPants.visible = false;
	}
}
