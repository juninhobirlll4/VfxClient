package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.vfxclient.core.module.performance.FpsLimiterModule;

/** Repassa o tick do cliente (20 por segundo) para o VfxClient. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void vfx$onTick(CallbackInfo ci) {
		VfxCore core = VfxCore.get();
		if (core != null) {
			core.onClientTick((MinecraftClient) (Object) this);
		}
	}

	@Inject(method = "getMaxFramerate()I", at = @At("RETURN"), cancellable = true)
	private void vfx$limitUnfocused(CallbackInfoReturnable<Integer> cir) {
		FpsLimiterModule module = VfxCore.module(FpsLimiterModule.class);
		if (module != null) {
			int limited = module.adjust(cir.getReturnValue());
			if (limited != cir.getReturnValue()) {
				cir.setReturnValue(limited);
			}
		}
	}

	/**
	 * Troca a tela inicial do Minecraft pela do VfxClient (módulo "Tela inicial"). Feito no começo
	 * do setScreen: a tela nova passa pelo setScreen inteiro (init, limpar chat etc.).
	 */
	@Inject(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"), cancellable = true)
	private void vfx$titleScreen(net.minecraft.client.gui.screen.Screen screen, CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		boolean vanillaTitle = screen != null && screen.getClass() == net.minecraft.client.gui.screen.TitleScreen.class;
		boolean impliedTitle = screen == null && client.world == null;
		if (!vanillaTitle && !impliedTitle) {
			return;
		}
		com.vfxclient.core.module.general.TitleScreenModule module = VfxCore.module(com.vfxclient.core.module.general.TitleScreenModule.class);
		if (module != null && module.isEnabled()) {
			ci.cancel();
			client.setScreen(new com.vfxclient.core.gui.VfxTitleScreen());
		}
	}
}