package com.vfxclient.core.module.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.NumberSetting;

/** Barra de itens no visual plano: slots escuros arredondados e o slot escolhido com borda colorida. */
public class HotbarModule extends Module {

	private final ColorSetting slotColor = add(new ColorSetting("slotColor", "Cor dos slots", 0x8C1A1A20));
	private final ColorSetting selectedColor = add(new ColorSetting("selectedColor", "Borda do slot escolhido", 0xFFE0A526));
	private final NumberSetting radius = add(new NumberSetting("radius", "Arredondamento", 3, 0, 6, 1, " px"));

	public HotbarModule() {
		super("hotbar", "Barra de itens", "Slots planos e arredondados", Category.UTILITY, false);
	}

	/**
	 * Fundo da barra (no lugar da textura do jogo).
	 *
	 * @param x X da barra (182 de largura)
	 * @param y Y da barra (22 de altura)
	 */
	public void drawBar(int x, int y) {
		Render2D.roundedRect(x - 1, y - 1, 184, 24, radius.getInt() + 1, 0x66000000);
		for (int i = 0; i < 9; i++) {
			Render2D.roundedRect(x + 1 + i * 20, y + 1, 20, 20, radius.getInt(), slotColor.argb());
		}
		restore();
	}

	/**
	 * Destaque do slot escolhido.
	 *
	 * @param x X do destaque do jogo (24 de largura)
	 * @param y Y do destaque do jogo
	 */
	public void drawSelected(int x, int y) {
		Render2D.outlinedRect(x + 1, y + 1, 22, 22, radius.getInt() + 1, selectedColor.argb(), 0x22FFFFFF);
		Render2D.roundedRect(x + 3, y + 3, 18, 18, Math.max(0, radius.getInt() - 1), 0x40000000);
		restore();
	}

	private static void restore() {
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}
}