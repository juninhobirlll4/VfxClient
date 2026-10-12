package com.vfxclient.core.mixin;

import com.vfxclient.core.hud.HudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Desenha a HUD do VfxClient depois da HUD do jogo. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

	@Inject(method = "render(F)V", at = @At("TAIL"))
	private void vfx$renderHud(float tickDelta, CallbackInfo ci) {
		HudRenderer.render(MinecraftClient.getInstance());
	}
}
