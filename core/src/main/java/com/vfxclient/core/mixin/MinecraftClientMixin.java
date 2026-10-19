package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.ScreenSwap;
import com.vfxclient.core.module.performance.EngineModule;
import com.vfxclient.core.module.performance.FpsLimiterModule;
import com.vfxclient.core.module.visual.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tick do cliente para o VfxClient, limite de FPS e troca das telas do jogo pelas nossas. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void vfx$onTick(CallbackInfo ci) {
		VfxCore core = VfxCore.get();
		if (core != null) {
			core.onClientTick((MinecraftClient) (Object) this);
		}
	}

	/** Com a tecla do zoom segurada, a rodinha do mouse muda o zoom em vez de trocar o item da barra. */
	@Redirect(method = "tick()V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(I)V"))
	private void vfx$zoomScroll(PlayerInventory inventory, int scroll) {
		ZoomModule zoom = VfxCore.module(ZoomModule.class);
		if (zoom == null || !zoom.consumeScroll(scroll)) {
			inventory.scrollInHotbar(scroll);
		}
	}

	/** GPU Fencing: depois de trocar o quadro. */
	@Inject(method = "updateDisplay()V", at = @At("TAIL"))
	private void vfx(CallbackInfo ci) {
		EngineModule engine = VfxCore.module(EngineModule.class);
		if (engine != null) {
			engine.afterFrame();
		}
	}

	@Inject(method = "getMaxFramerate()I", at = @At("RETURN"), cancellable = true)
	private void vfx$limitUnfocused(CallbackInfoReturnable<Integer> cir) {
		FpsLimiterModule module = VfxCore.module(FpsLimiterModule.class);
		if (module == null) {
			return;
		}

		int limited = module.adjust(cir.getReturnValue());
		if (limited != cir.getReturnValue()) {
			cir.setReturnValue(limited);
		}
	}

	/**
	 * Troca telas do jogo (inicial, pausa, servidores) pelas do VfxClient. Feito no começo do
	 * setScreen, para a tela nova passar pelo setScreen inteiro (init, limpar chat etc.).
	 */
	@Inject(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"), cancellable = true)
	private void vfx$swapScreen(Screen screen, CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		Screen replacement = ScreenSwap.replace(client, screen);
		if (replacement != null) {
			ci.cancel();
			client.setScreen(replacement);
		}
	}
}