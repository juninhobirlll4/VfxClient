package com.vfxclient.core.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.font.Fonts;

/**
 * Peças visuais das telas do VfxClient (tela inicial, pausa, servidores, opções): botões
 * planos, barra de cima com a logo e ícones desenhados com formas simples.
 *
 * <p>Medidas em unidades de referência ({@link Canvas}), as mesmas das imagens de exemplo.</p>
 */
public final class Ui {

	/** Tipo de botão. */
	public enum Style {
		/** Escuro com contorno claro. */
		NORMAL,
		/** Gradiente roxo (ação principal). */
		PRIMARY,
		/** Contorno vermelho (sair, desconectar). */
		DANGER,
		/** Apagado (sem seleção, indisponível). */
		DISABLED
	}

	/** Ícones da barra de cima. */
	public enum Icon {
		SETTINGS, MENU, PACKS, CLOSE, LANGUAGE
	}

	public static final int PURPLE_DEEP = 0xFF5B16D0;
	public static final int PURPLE = 0xFF7B2FF7;
	public static final int PURPLE_LIGHT = 0xFFB48CFF;
	public static final int MUTED = 0xFFA99CC4;
	public static final int GREEN = 0xFF2FD07A;
	public static final int YELLOW = 0xFFF5C542;
	public static final int RED = 0xFFE5484D;

	private static final int BUTTON_FILL = 0xE6141218;
	private static final int BUTTON_FILL_HOVER = 0xF2262230;

	private Ui() {
	}

	/**
	 * Botão com texto em Montserrat, centralizado.
	 *
	 * @param label    texto (já em maiúsculas, se for o caso)
	 * @param x        esquerda
	 * @param y        topo
	 * @param w        largura
	 * @param h        altura
	 * @param textSize tamanho do texto
	 * @param style    tipo
	 * @param hover    mouse em cima
	 */
	public static void button(String label, float x, float y, float w, float h, float textSize, Style style, boolean hover) {
		buttonFrame(x, y, w, h, style, hover);
		buttonLabel(label, x, y, w, h, textSize, style);
	}

	/** Só o fundo e o contorno de um {@link #button}. */
	public static void buttonFrame(float x, float y, float w, float h, Style style, boolean hover) {
		switch (style) {
			case PRIMARY:
				drawPrimaryFill(x, y, w, h, hover);
				break;
			case DANGER:
				Render2D.outlinedRect(x, y, w, h, 8, hover ? RED : 0xCCE5484D, hover ? BUTTON_FILL_HOVER : BUTTON_FILL);
				break;
			case DISABLED:
				Render2D.outlinedRect(x, y, w, h, 8, 0x33FFFFFF, BUTTON_FILL);
				break;
			default:
				Render2D.outlinedRect(x, y, w, h, 8, hover ? 0xFFFFFFFF : 0x8CFFFFFF, hover ? BUTTON_FILL_HOVER : BUTTON_FILL);
				break;
		}
	}

	/** Só o texto de um {@link #button}, centralizado. */
	public static void buttonLabel(String label, float x, float y, float w, float h, float textSize, Style style) {
		int textColor;
		if (style == Style.DANGER) {
			textColor = 0xFFFFB3B5;
		} else if (style == Style.DISABLED) {
			textColor = 0xFF8F8F99;
		} else {
			textColor = 0xFFFFFFFF;
		}
		float textY = y + (h - Fonts.lineHeight(Fonts.BUTTON, textSize)) / 2F;
		Fonts.drawSpacedCentered(Fonts.BUTTON, label, x + w / 2F, textY, textSize, 0.06F, textColor);
	}

	/** Gradiente roxo com brilho embaixo, como o botão principal das imagens. */
	private static void drawPrimaryFill(float x, float y, float w, float h, boolean hover) {
		int light = hover ? 0xFFC9AAFF : PURPLE_LIGHT;
		float corner = 8;

		// Brilho roxo embaixo do botão.
		Render2D.roundedRect(x - 2, y + 6, w + 4, h, 10, hover ? 0x667B2FF7 : 0x447B2FF7);

		// Pontas arredondadas nas cores das extremidades e o gradiente no meio.
		Render2D.roundedRect(x, y, corner * 2, h, corner, PURPLE_DEEP);
		Render2D.roundedRect(x + w - corner * 2, y, corner * 2, h, corner, light);
		float middle = w - corner * 2;
		Render2D.gradientH(x + corner, y, middle * 0.55F, h, PURPLE_DEEP, PURPLE);
		Render2D.gradientH(x + corner + middle * 0.55F, y, middle * 0.45F, h, PURPLE, light);
		restore();
	}

	/**
	 * Logo e "VFXCLIENT" no canto de cima, como nas imagens.
	 *
	 * @param x esquerda
	 * @param y topo
	 */
	public static void brand(float x, float y) {
		Logo.draw((int) x, (int) y, 58);
		restore();
		Fonts.drawSpaced(Fonts.TITLE, "VFXCLIENT", x + 75, y + 11, 34, 0.04F, 0x66000000);
		Fonts.drawSpaced(Fonts.TITLE, "VFXCLIENT", x + 74, y + 9, 34, 0.04F, 0xFFFFFFFF);
	}

	/**
	 * Botão quadrado com ícone (barra de cima).
	 *
	 * @param icon  ícone
	 * @param x     esquerda
	 * @param y     topo
	 * @param hover mouse em cima
	 */
	public static void iconButton(Icon icon, float x, float y, boolean hover) {
		float size = 58;
		int border = hover ? (icon == Icon.CLOSE ? RED : 0xFFFFFFFF) : 0x59FFFFFF;
		Render2D.outlinedRect(x, y, size, size, 8, border, hover ? BUTTON_FILL_HOVER : 0xCC141218);

		int color = icon == Icon.CLOSE ? 0xFFFF8A8D : (hover ? 0xFFFFFFFF : 0xFFDDDDDD);
		float cx = x + size / 2F;
		float cy = y + size / 2F;
		switch (icon) {
			case SETTINGS:
				Render2D.gear(cx, cy, 10F, color);
				break;
			case MENU:
				Logo.draw((int) (cx - 15), (int) (cy - 15), 30);
				break;
			case PACKS:
				drawPacks(cx, cy, color);
				break;
			case CLOSE:
				drawCross(cx, cy, 11F, 2.5F, color);
				break;
			case LANGUAGE:
				drawGlobe(cx, cy, color);
				break;
			default:
				break;
		}

		restore();
	}

	private static void drawPacks(float cx, float cy, int color) {
		for (int row = -1; row <= 1; row++) {
			for (int col = -1; col <= 1; col++) {
				Render2D.rawRect(cx + col * 7 - 2.5F, cy + row * 7 - 2.5F, 5, 5, color);
			}
		}
	}

	private static void drawGlobe(float cx, float cy, int color) {
		Render2D.ring(cx, cy, 13F, 2F, color);
		Render2D.rawRect(cx - 13, cy - 1, 26, 2, color);
		Render2D.rawRect(cx - 1, cy - 13, 2, 26, color);
		GlStateManager.pushMatrix();
		GlStateManager.translate(cx, cy, 0F);
		GlStateManager.scale(0.45F, 1F, 1F);
		Render2D.ring(0, 0, 13F, 3F, color);
		GlStateManager.popMatrix();
	}

	/**
	 * "X" desenhado com dois traços.
	 *
	 * @param cx        centro X
	 * @param cy        centro Y
	 * @param radius    metade do comprimento do traço
	 * @param thickness espessura
	 * @param color     cor
	 */
	public static void drawCross(float cx, float cy, float radius, float thickness, int color) {
		for (int angle : new int[] {45, -45}) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(cx, cy, 0F);
			GlStateManager.rotate(angle, 0F, 0F, 1F);
			Render2D.rawRect(-radius, -thickness / 2F, radius * 2, thickness, color);
			GlStateManager.popMatrix();
		}
	}

	/** Etiqueta escura com texto (informações do servidor no menu de pausa). */
	public static void pill(String label, String value, int valueColor, float x, float y, float h) {
		float size = 17;
		float labelW = Fonts.width(Fonts.TEXT, label + " ", size);
		float valueW = Fonts.width(Fonts.TEXT_BOLD, value, size);
		float w = labelW + valueW + 40;
		Render2D.outlinedRect(x, y, w, h, 10, 0x1FFFFFFF, 0x99100A16);
		float textY = y + (h - Fonts.lineHeight(Fonts.TEXT, size)) / 2F;
		Fonts.draw(Fonts.TEXT, label, x + 20, textY, size, 0xFFD8D8DE);
		Fonts.draw(Fonts.TEXT_BOLD, value, x + 20 + labelW, textY, size, valueColor);
	}

	/**
	 * @return largura de uma etiqueta {@link #pill}
	 */
	public static float pillWidth(String label, String value) {
		return Fonts.width(Fonts.TEXT, label + " ", 17) + Fonts.width(Fonts.TEXT_BOLD, value, 17) + 40;
	}

	/** Deixa o estado do OpenGL pronto para o próximo desenho com textura. */
	public static void restore() {
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	/** @return se o ponto (mx, my) está dentro do retângulo */
	public static boolean inside(float mx, float my, float x, float y, float w, float h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
