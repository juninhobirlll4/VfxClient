package com.vfxclient.core.module.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.hud.PingModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

/**
 * Lista de jogadores (Tab): ping em número, colorido pela qualidade, no lugar das barrinhas, e a
 * cabeça de cada jogador também em servidores offline (o jogo só mostra em servidores originais).
 */
public class TabListModule extends Module {

	private final BooleanSetting numericPing = add(new BooleanSetting("numericPing", "Ping em número", true));
	private final BooleanSetting heads = add(new BooleanSetting("heads", "Cabeça dos jogadores sempre", true));

	public TabListModule() {
		super("tabList", "Lista de jogadores", "Ping em número no Tab", Category.UTILITY, true);
	}

	/** @return se a cabeça dos jogadores deve aparecer mesmo em servidor offline */
	public boolean alwaysHeads() {
		return isEnabled() && heads.get();
	}

	/**
	 * Desenha o ping em número no lugar do ícone.
	 *
	 * @param width largura da coluna do jogador
	 * @param x     X da coluna
	 * @param y     Y da linha
	 * @param entry jogador
	 * @return {@code true} se desenhou (o ícone original não deve aparecer)
	 */
	public boolean renderPing(int width, int x, int y, PlayerListEntry entry) {
		if (!isEnabled() || !numericPing.get()) {
			return false;
		}
		int ms = entry.getLatency();
		String text = ms < 0 ? "§7?" : PingModule.qualityCode(ms) + ms;
		MinecraftClient client = MinecraftClient.getInstance();
		float scale = 0.75F;
		int textWidth = client.textRenderer.getStringWidth(text);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + width - 1 - textWidth * scale, y + 1.5F, 0F);
		GlStateManager.scale(scale, scale, 1F);
		client.textRenderer.drawWithShadow(text, 0, 0, 0xFFFFFFFF);
		GlStateManager.popMatrix();
		return true;
	}
}
