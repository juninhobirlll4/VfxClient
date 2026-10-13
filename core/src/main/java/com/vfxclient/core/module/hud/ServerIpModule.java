package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/** Mostra o servidor atual (útil em gravações); no mundo local mostra "Um jogador". */
public class ServerIpModule extends TextHudModule {

	private final BooleanSetting showName = add(new BooleanSetting("showName", "Nome salvo em vez do endereço", false));
	private final BooleanSetting singleplayer = add(new BooleanSetting("singleplayer", "Mostrar no mundo local", false));

	public ServerIpModule() {
		super("serverIp", "Servidor", "Endereço do servidor atual", false, 4, 4);
		setDefaultAnchor(TOP_RIGHT);
	}

	@Override
	protected boolean isSecondary() {
		return true;
	}

	private String format(String server) {
		return advanced() ? "Servidor " + VALUE + server : VALUE + server;
	}

	@Override
	public String getText(MinecraftClient client) {
		if (client.world == null) {
			return null;
		}
		if (client.isInSingleplayer()) {
			return singleplayer.get() ? format("Um jogador") : null;
		}
		ServerInfo server = client.getCurrentServerEntry();
		if (server == null) {
			return null;
		}
		return format(showName.get() && server.name != null && !server.name.isEmpty() ? server.name : server.address);
	}

	@Override
	public String getSampleText() {
		return format("mc.servidor.com");
	}
}
