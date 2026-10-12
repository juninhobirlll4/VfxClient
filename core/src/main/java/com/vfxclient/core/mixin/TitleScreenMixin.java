package com.vfxclient.core.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.Logo;
import com.vfxclient.core.module.general.TitleScreenModule;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tela inicial com a identidade do VfxClient.
 *
 * <p>Troca a logo do Minecraft pelo nosso logotipo e acrescenta a versão do client
 * no texto do canto inferior esquerdo. Tudo desligável pelo módulo "Tela inicial".</p>
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

	private static final int WORDMARK_W = 200;
	private static final int WORDMARK_H = 50;

	private static boolean enabled() {
		TitleScreenModule module = VfxCore.module(TitleScreenModule.class);
		return module != null && module.isEnabled();
	}

	/** Pula os pedaços da logo original (todas as chamadas de drawTexture do render são da logo). */
	@Redirect(method = "render(IIF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawTexture(IIIIII)V"))
	private void vfx$skipVanillaLogo(TitleScreen screen, int x, int y, int u, int v, int width, int height) {
		if (!enabled()) {
			screen.drawTexture(x, y, u, v, width, height);
		}
	}

	/** Desenha o nosso logotipo logo antes do texto amarelo (splash). */
	@Inject(method = "render(IIF)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;pushMatrix()V", ordinal = 0))
	private void vfx$drawWordmark(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		if (enabled()) {
			Logo.draw(Logo.WORDMARK, width / 2 - WORDMARK_W / 2, 22, WORDMARK_W, WORDMARK_H);
			GlStateManager.color(1F, 1F, 1F, 1F);
		}
	}

	/** "Minecraft 1.8.9" vira "VfxClient 0.x · Minecraft 1.8.9". */
	@ModifyArg(method = "render(IIF)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
					ordinal = 0),
			index = 1)
	private String vfx$versionText(String original) {
		VfxCore core = VfxCore.get();
		if (core == null || !enabled()) {
			return original;
		}
		return VfxCore.NAME + " " + core.getVersion() + " - " + original;
	}
}
