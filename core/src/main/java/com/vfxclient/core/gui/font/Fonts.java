package com.vfxclient.core.gui.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * Fontes do VfxClient (Montserrat e Inter, licença SIL OFL, em {@code assets/vfx/fonts}).
 *
 * <p>Os tamanhos são passados nas unidades da tela que está desenhando. Antes de desenhar, a
 * tela informa quantos pixels reais cabem em uma unidade ({@link #setPixelsPerUnit}); assim
 * as letras são geradas no tamanho exato e ficam nítidas.</p>
 */
public final class Fonts {

	/** Títulos e o nome do client (Montserrat Black). */
	public static final FontFamily TITLE = new FontFamily("/assets/vfx/fonts/montserrat-900.ttf");
	/** Botões (Montserrat ExtraBold). */
	public static final FontFamily BUTTON = new FontFamily("/assets/vfx/fonts/montserrat-800.ttf");
	/** Textos (Inter Medium). */
	public static final FontFamily TEXT = new FontFamily("/assets/vfx/fonts/inter-500.ttf");
	/** Textos em destaque (Inter Bold). */
	public static final FontFamily TEXT_BOLD = new FontFamily("/assets/vfx/fonts/inter-700.ttf");

	private static float pixelsPerUnit = 1F;

	private Fonts() {
	}

	/**
	 * @param pixels quantos pixels reais da janela cabem em uma unidade de desenho
	 */
	public static void setPixelsPerUnit(float pixels) {
		pixelsPerUnit = Math.max(0.1F, pixels);
	}

	/** Usa a escala da interface do Minecraft (telas comuns, em unidades da GUI). */
	public static void useGuiScale() {
		setPixelsPerUnit(new Window(MinecraftClient.getInstance()).getScaleFactor());
	}

	/**
	 * Desenha um texto.
	 *
	 * @param family fonte
	 * @param text   texto
	 * @param x      esquerda
	 * @param y      topo
	 * @param size   altura da letra, em unidades
	 * @param argb   cor
	 */
	public static void draw(FontFamily family, String text, float x, float y, float size, int argb) {
		drawSpaced(family, text, x, y, size, 0F, argb);
	}

	/**
	 * Desenha um texto com espaço extra entre as letras (títulos e botões em maiúsculas).
	 *
	 * @param tracking espaço extra, em proporção do tamanho (ex.: 0.12 = 12% do tamanho)
	 */
	public static void drawSpaced(FontFamily family, String text, float x, float y, float size, float tracking, int argb) {
		GlyphAtlas atlas = family.atlas(Math.round(size * pixelsPerUnit));
		if (atlas == null) {
			MinecraftClient.getInstance().textRenderer.draw(text, x, y, argb, false);
			return;
		}
		atlas.draw(text, x, y, 1F / pixelsPerUnit, tracking * size, argb);
	}

	/** Igual a {@link #drawSpaced}, centralizado em {@code centerX}. */
	public static void drawSpacedCentered(FontFamily family, String text, float centerX, float y, float size, float tracking, int argb) {
		float width = width(family, text, size, tracking);
		drawSpaced(family, text, centerX - width / 2F, y, size, tracking, argb);
	}

	/** Igual a {@link #draw}, com uma sombra suave embaixo (como nos títulos). */
	public static void drawWithShadow(FontFamily family, String text, float x, float y, float size, int argb) {
		int alpha = (argb >>> 24) & 0xFF;
		float offset = Math.max(1F, size / 14F);
		draw(family, text, x + offset * 0.5F, y + offset, size, (alpha * 2 / 5) << 24);
		draw(family, text, x, y, size, argb);
	}

	/** Desenha o texto centralizado em {@code centerX}. */
	public static void drawCentered(FontFamily family, String text, float centerX, float y, float size, int argb) {
		draw(family, text, centerX - width(family, text, size) / 2F, y, size, argb);
	}

	/**
	 * @return largura do texto, em unidades
	 */
	public static float width(FontFamily family, String text, float size) {
		return width(family, text, size, 0F);
	}

	/**
	 * @return largura do texto com espaçamento extra, em unidades
	 */
	public static float width(FontFamily family, String text, float size, float tracking) {
		GlyphAtlas atlas = family.atlas(Math.round(size * pixelsPerUnit));
		if (atlas == null) {
			return MinecraftClient.getInstance().textRenderer.getStringWidth(text);
		}
		float extra = text.isEmpty() ? 0F : tracking * size * (text.length() - 1);
		return atlas.width(text) / pixelsPerUnit + extra;
	}

	/**
	 * @return altura de uma linha de texto, em unidades
	 */
	public static float lineHeight(FontFamily family, float size) {
		GlyphAtlas atlas = family.atlas(Math.round(size * pixelsPerUnit));
		return atlas == null ? 9F : atlas.lineHeight() / pixelsPerUnit;
	}

	/** Cores dos códigos de formatação do Minecraft (§0 a §f). */
	private static final int[] FORMAT_COLORS = {
		0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
		0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
	};

	/**
	 * Desenha um texto com os códigos de cor do Minecraft (§a, §c...), como a mensagem dos
	 * servidores. Negrito, itálico e afins são ignorados.
	 *
	 * @param defaultColor cor antes do primeiro código e depois de §r
	 */
	public static void drawFormatted(FontFamily family, String text, float x, float y, float size, int defaultColor) {
		int alpha = defaultColor & 0xFF000000;
		int color = defaultColor;
		StringBuilder part = new StringBuilder();
		float cursor = x;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\u00a7' && i + 1 < text.length()) {
				cursor = flush(family, part, cursor, y, size, color);
				int code = "0123456789abcdef".indexOf(Character.toLowerCase(text.charAt(i + 1)));
				if (code >= 0) {
					color = alpha | FORMAT_COLORS[code];
				} else if (Character.toLowerCase(text.charAt(i + 1)) == 'r') {
					color = defaultColor;
				}
				i++;
			} else {
				part.append(c);
			}
		}

		flush(family, part, cursor, y, size, color);
	}

	private static float flush(FontFamily family, StringBuilder part, float x, float y, float size, int color) {
		if (part.length() == 0) {
			return x;
		}
		String text = part.toString();
		draw(family, text, x, y, size, color);
		part.setLength(0);
		return x + width(family, text, size);
	}

	/** @return o texto sem os códigos de cor do Minecraft */
	public static String stripFormatting(String text) {
		return text == null ? "" : text.replaceAll("\u00a7.", "");
	}
}