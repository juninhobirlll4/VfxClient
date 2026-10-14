package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.TabListModule;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ping em número e cabeças na lista de jogadores. */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

	@Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
	private void vfx$numericPing(int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
		TabListModule module = VfxCore.module(TabListModule.class);
		if (module != null && module.renderPing(width, x, y, entry)) {
			ci.cancel();
		}
	}

	/** O jogo só desenha as cabeças em conexão criptografada (servidor original); aqui, sempre. */
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isEncrypted()Z"))
	private boolean vfx$alwaysHeads(ClientConnection connection) {
		TabListModule module = VfxCore.module(TabListModule.class);
		return (module != null && module.alwaysHeads()) || connection.isEncrypted();
	}
}