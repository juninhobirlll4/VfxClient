package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.hud.HudRenderer;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import com.vfxclient.core.module.util.CrosshairModule;
import com.vfxclient.core.module.util.ScoreboardModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD do VfxClient, mira personalizada, placar e vinheta. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

	@Inject(method = "render(F)V", at = @At("TAIL"))
	private void vfx$renderHud(float tickDelta, CallbackInfo ci) {
		HudRenderer.render(MinecraftClient.getInstance());
	}

	/** Com a mira personalizada ligada, desenha a nossa e diz ao jogo para não desenhar a dele. */
	@Redirect(method = "render(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;showCrosshair()Z"))
	private boolean vfx$customCrosshair(InGameHud hud) {
		boolean vanilla = ((InGameHudAccessor) hud).vfx$showCrosshair();
		CrosshairModule module = VfxCore.module(CrosshairModule.class);
		if (!vanilla || module == null || !module.isEnabled()) {
			return vanilla;
		}
		Window window = new Window(MinecraftClient.getInstance());
		module.render(window.getWidth(), window.getHeight());
		return false;
	}

	@Inject(method = "renderScoreboardObjective", at = @At("HEAD"), cancellable = true)
	private void vfx$hideScoreboard(CallbackInfo ci) {
		ScoreboardModule module = VfxCore.module(ScoreboardModule.class);
		if (module != null && module.hideAll()) {
			ci.cancel();
		}
	}

	/** Segunda chamada de draw no placar = o número vermelho de cada linha. */
	@Redirect(method = "renderScoreboardObjective",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)I", ordinal = 1))
	private int vfx$scoreNumbers(TextRenderer renderer, String text, int x, int y, int color) {
		ScoreboardModule module = VfxCore.module(ScoreboardModule.class);
		if (module != null && module.hideNumbers()) {
			return 0;
		}
		return renderer.draw(text, x, y, color);
	}

	@Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
	private void vfx$noVignette(CallbackInfo ci) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks != null && !tweaks.vignette()) {
			ci.cancel();
		}
	}
}
