package com.vfxclient.core.mixin;

import com.vfxclient.core.gui.VanillaStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fundo das telas do jogo no visual do VfxClient: escuro no lugar da terra (ver {@link VanillaStyle}). */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

	@Shadow
	protected MinecraftClient client;
	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "renderBackground(I)V", at = @At("HEAD"), cancellable = true)
	private void vfx$background(int alpha, CallbackInfo ci) {
		if (VanillaStyle.enabled() && client.world != null) {
			VanillaStyle.drawWorldOverlay(width, height);
			ci.cancel();
		}
	}

	@Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
	private void vfx$dirtBackground(int alpha, CallbackInfo ci) {
		if (VanillaStyle.enabled()) {
			VanillaStyle.drawBackground(width, height);
			ci.cancel();
		}
	}
}
