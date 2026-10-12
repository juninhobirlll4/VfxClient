package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.TextHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

/** Mostra a latência com o servidor (só em multiplayer). */
public class PingModule extends TextHudModule {

	public PingModule() {
		super("ping", "Ping", "Latência com o servidor", true, 4, 22);
	}

	@Override
	public String getText(MinecraftClient client) {
		if (client.player == null || client.getNetworkHandler() == null || client.isInSingleplayer()) {
			return null;
		}
		PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
		return entry == null ? null : entry.getLatency() + " ms";
	}

	@Override
	public String getSampleText() {
		return "32 ms";
	}
}
