package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.TabListModule;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ping em número na lista de jogadores. */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

	@Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
	private void vfx$numericPing(int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
		TabListModule module = VfxCore.module(TabListModule.class);
		if (module != null && module.renderPing(width, x, y, entry)) {
			ci.cancel();
		}
	}
}
