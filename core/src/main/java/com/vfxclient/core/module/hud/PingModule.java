package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.TextHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;

/** Mostra a latência com o servidor (só em multiplayer); no modo avançado, também o endereço. */
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
		if (entry == null) {
			return null;
		}
		if (!advanced()) {
			return entry.getLatency() + " ms";
		}
		ServerInfo server = client.getCurrentServerEntry();
		String address = server == null ? "" : "  §7" + server.address;
		return "Ping " + entry.getLatency() + " ms" + address;
	}

	@Override
	public String getSampleText() {
		return advanced() ? "Ping 32 ms  §7mc.servidor.com" : "32 ms";
	}
}
