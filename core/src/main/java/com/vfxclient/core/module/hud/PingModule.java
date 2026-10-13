package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

/**
 * Mostra a latência com o servidor (só em multiplayer). Com "cor por qualidade": verde até
 * 80 ms, amarelo até 150 ms, laranja até 250 ms e vermelho acima disso.
 */
public class PingModule extends TextHudModule {

	private final BooleanSetting qualityColor = add(new BooleanSetting("qualityColor", "Cor por qualidade da conexão", true));

	public PingModule() {
		super("ping", "Ping", "Latência com o servidor", true, 4, 22);
	}

	/**
	 * @param ms latência
	 * @return código de cor do Minecraft para a qualidade
	 */
	public static String qualityCode(int ms) {
		return ms <= 80 ? "§a" : ms <= 150 ? "§e" : ms <= 250 ? "§6" : "§c";
	}

	private String format(int ms) {
		String value = qualityColor.get() ? qualityCode(ms) + ms + "§r" : VALUE + String.valueOf(ms) + TEXT;
		return advanced() ? "Ping " + value + " ms" : value + " ms";
	}

	@Override
	public String getText(MinecraftClient client) {
		if (client.player == null || client.getNetworkHandler() == null || client.isInSingleplayer()) {
			return null;
		}
		PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
		return entry == null ? null : format(entry.getLatency());
	}

	@Override
	public String getSampleText() {
		return format(32);
	}
}
