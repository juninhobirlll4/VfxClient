package com.vfxclient.core.mixin.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.Logo;
import com.vfxclient.core.module.util.TabListModule;
import com.vfxclient.core.net.VfxUsers;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ping em número, cabeças e a logo do VfxClient na lista de jogadores. */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

	@Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
	private void vfx$numericPing(int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
		TabListModule module = VfxCore.module(TabListModule.class);
		if (module != null && module.renderPing(width, x, y, entry)) {
			ci.cancel();
		}
	}

	/** Quem usa o VfxClient ganha dois espaços antes do nome, onde a logo é desenhada. */
	@Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
	private void vfx$reserveBadge(PlayerListEntry entry, CallbackInfoReturnable<String> cir) {
		if (VfxUsers.showsBadge(entry.getProfile().getName())) {
			cir.setReturnValue(VfxUsers.MARK + "  " + cir.getReturnValue());
		}
	}

	/** Desenha a logo no lugar reservado (a marca zero largura mostra qual nome tem logo). */
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;FFI)I"))
	private int vfx$drawBadge(TextRenderer textRenderer, String text, float x, float y, int color) {
		if (text.contains(VfxUsers.MARK)) {
			Logo.drawSmall((int) x, (int) y, 8);
			// A logo desliga a mistura de cores; a lista precisa dela ligada para o texto.
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		}
		return textRenderer.drawWithShadow(text, x, y, color);
	}

	/** O jogo só desenha as cabeças em conexão criptografada (servidor original); aqui, sempre. */
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isEncrypted()Z"))
	private boolean vfx$alwaysHeads(ClientConnection connection) {
		TabListModule module = VfxCore.module(TabListModule.class);
		return (module != null && module.alwaysHeads()) || connection.isEncrypted();
	}
}