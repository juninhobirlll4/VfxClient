package com.vfxclient.core.module.hud;

import java.util.Locale;

import com.vfxclient.core.gui.IconCard;
import com.vfxclient.core.gui.ServerIcons;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Identifier;

/**
 * Mostra o servidor atual num cartão: o ícone do servidor (o mesmo da lista de servidores), o
 * endereço (ou o nome salvo) e, embaixo, o ping e quantos jogadores há. No mundo local mostra
 * "Um jogador".
 */
public class ServerIpModule extends HudModule {

	private static final int SAMPLE_COLOR = 0xFF3C6E8F;
	private static final int BORDER = 0x66B48CFF;

	private final BooleanSetting showName = add(new BooleanSetting("showName", "Nome salvo em vez do endereço", false));
	private final BooleanSetting singleplayer = add(new BooleanSetting("singleplayer", "Mostrar no mundo local", false));
	private final BooleanSetting info = add(new BooleanSetting("info", "Mostrar ping e jogadores", true));

	public ServerIpModule() {
		super("serverIp", "Servidor", "Ícone, endereço, ping e jogadores do servidor atual", false, 4, 50);
		setDefaultAnchor(TOP_RIGHT);
	}

	/** O que o cartão mostra agora. */
	private static final class Card {
		final String title;
		final String subtitle;
		final Identifier icon;
		final String initials;
		final int color;

		Card(String title, String subtitle, Identifier icon, String initials, int color) {
			this.title = title;
			this.subtitle = subtitle;
			this.icon = icon;
			this.initials = initials;
			this.color = color;
		}
	}

	@Override
	protected boolean usesSharedBackground() {
		return false;
	}

	@Override
	public boolean isVisible(MinecraftClient client, boolean preview) {
		return card(client, preview) != null;
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		Card card = card(client, preview);
		if (card == null) {
			return 0;
		}
		int text = Math.max(client.textRenderer.getStringWidth(card.title), client.textRenderer.getStringWidth(card.subtitle));
		return IconCard.width(text);
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		return IconCard.HEIGHT;
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		Card card = card(client, preview);
		if (card == null) {
			return;
		}
		IconCard.Icon icon = c -> {
			if (card.icon == null) {
				return false;
			}
			c.getTextureManager().bindTexture(card.icon);
			return true;
		};
		IconCard.draw(client, icon, card.initials, card.color, card.title, card.subtitle, contentWidth(client, preview), BORDER, 1F);
	}

	private Card card(MinecraftClient client, boolean preview) {
		if (client.world == null) {
			return preview ? new Card("mc.servidor.com", info.get() ? "32 ms · 24 jogadores" : "", null, "MC", SAMPLE_COLOR) : null;
		}
		if (client.isInSingleplayer()) {
			return singleplayer.get() || preview ? new Card("Um jogador", info.get() ? "Mundo local" : "", null, "1J", SAMPLE_COLOR) : null;
		}

		ServerInfo server = client.getCurrentServerEntry();
		if (server == null) {
			return preview ? new Card("mc.servidor.com", "", null, "MC", SAMPLE_COLOR) : null;
		}
		boolean useName = showName.get() && server.name != null && !server.name.isEmpty();
		String title = useName ? server.name : server.address;
		int color = 0xFF000000 | (Math.abs(server.address.hashCode()) & 0x7F7F7F) | 0x303030;
		return new Card(title, info.get() ? details(client) : "", ServerIcons.textureFor(server), initials(title), color);
	}

	/** "32 ms · 24 jogadores". */
	private static String details(MinecraftClient client) {
		StringBuilder text = new StringBuilder();
		if (client.player != null && client.getNetworkHandler() != null) {
			PlayerListEntry self = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
			if (self != null && self.getLatency() > 0) {
				text.append(self.getLatency()).append(" ms");
			}
			int players = client.getNetworkHandler().getPlayerList().size();
			if (players > 0) {
				text.append(text.length() > 0 ? " · " : "").append(players).append(players == 1 ? " jogador" : " jogadores");
			}
		}
		return text.toString();
	}

	/** Duas letras do nome, para quando o servidor não manda ícone. */
	private static String initials(String name) {
		String clean = name.replaceAll("§.", "").replaceAll("[^\\p{L}\\p{N}]", "");
		return clean.isEmpty() ? "?" : clean.substring(0, Math.min(2, clean.length())).toUpperCase(Locale.ROOT);
	}
}
