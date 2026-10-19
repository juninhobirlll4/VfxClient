package com.vfxclient.core.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;

/**
 * Cartão com um ícone à esquerda e duas linhas de texto (servidor e pacote de textura no HUD).
 * O fundo traz o próprio ícone esticado e esmaecido, num degradê que escurece para a direita:
 * um detalhe de cor que combina com a imagem.
 */
public final class IconCard {

	/** De onde vem o ícone. */
	public interface Icon {

		/**
		 * Deixa a textura do ícone pronta para desenhar.
		 *
		 * @return {@code false} se não há imagem (usa as iniciais)
		 */
		boolean bind(MinecraftClient client);
	}

	public static final int ICON = 32;
	public static final int PAD = 4;
	public static final int HEIGHT = ICON + PAD * 2;
	private static final int BASE = 0xE60E0C14;
	private static final int TEXT_GAP = 7;

	private IconCard() {
	}

	/**
	 * @param textWidth largura do maior texto
	 * @return largura do cartão
	 */
	public static int width(int textWidth) {
		return PAD + ICON + TEXT_GAP + textWidth + 10;
	}

	/**
	 * Desenha o cartão com o canto de cima à esquerda em (0, 0) da matriz atual.
	 *
	 * @param icon          de onde vem o ícone
	 * @param initials      letras quando não há imagem
	 * @param fallbackColor cor do quadrado das iniciais
	 * @param title         linha de cima (aceita códigos de cor do Minecraft)
	 * @param subtitle      linha de baixo (pode ser vazia)
	 * @param width         largura do cartão
	 * @param border        cor do contorno
	 * @param alpha         transparência geral (0 a 1)
	 */
	public static void draw(MinecraftClient client, Icon icon, String initials, int fallbackColor, String title, String subtitle,
			int width, int border, float alpha) {
		Render2D.roundedRect(0, 0, width, HEIGHT, 5, withAlpha(BASE, alpha));

		boolean hasImage = drawBackdrop(client, icon, width, alpha);
		Render2D.gradientH(1, 1, width - 2, HEIGHT - 2, withAlpha(0x40101018, alpha), withAlpha(0xF2101018, alpha));
		Render2D.roundedOutline(0, 0, width, HEIGHT, 5, 1F, withAlpha(border, alpha));

		drawTile(client, icon, hasImage, initials, fallbackColor, alpha);

		TextRenderer text = client.textRenderer;
		int x = PAD + ICON + TEXT_GAP;
		text.draw(title, x, PAD + 5, withAlpha(0xFFFFFFFF, alpha), true);
		if (!subtitle.isEmpty()) {
			text.draw(subtitle, x, PAD + 5 + 11, withAlpha(0xFFB9B3CB, alpha), true);
		}
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	/** Ícone esticado por trás de tudo, bem fraco. */
	private static boolean drawBackdrop(MinecraftClient client, Icon icon, int width, float alpha) {
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		if (!icon.bind(client)) {
			return false;
		}
		GlStateManager.color(1F, 1F, 1F, 0.45F * alpha);
		DrawableHelper.drawTexture(1, 1, 0F, 0F, 1, 1, width - 2, HEIGHT - 2, 1F, 1F);
		GlStateManager.color(1F, 1F, 1F, 1F);
		return true;
	}

	private static void drawTile(MinecraftClient client, Icon icon, boolean hasImage, String initials, int fallbackColor, float alpha) {
		if (!hasImage) {
			Render2D.roundedRect(PAD, PAD, ICON, ICON, 4, withAlpha(fallbackColor, alpha));
			TextRenderer text = client.textRenderer;
			text.draw(initials, PAD + (ICON - text.getStringWidth(initials)) / 2, PAD + (ICON - 8) / 2, withAlpha(0xFFFFFFFF, alpha), true);
			return;
		}

		Render2D.roundedRect(PAD - 1, PAD - 1, ICON + 2, ICON + 2, 4, withAlpha(0xFF000000, alpha));
		icon.bind(client);
		GlStateManager.enableBlend();
		GlStateManager.color(1F, 1F, 1F, alpha);
		DrawableHelper.drawTexture(PAD, PAD, 0F, 0F, 1, 1, ICON, ICON, 1F, 1F);
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private static int withAlpha(int argb, float alpha) {
		int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, alpha)));
		return (a << 24) | (argb & 0xFFFFFF);
	}
}
