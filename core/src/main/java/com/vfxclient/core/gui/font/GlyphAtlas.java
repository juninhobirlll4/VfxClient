package com.vfxclient.core.gui.font;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.lwjgl.opengl.GL11;

/**
 * As letras de uma fonte num tamanho exato (em pixels reais da janela), desenhadas uma vez com
 * o Java e enviadas para a placa de vídeo como uma textura só.
 *
 * <p>Cobre os caracteres de 32 a 255 (inclui acentos do português).</p>
 */
final class GlyphAtlas {

	private static final int FIRST = 32;
	private static final int LAST = 255;
	private static final int TEXTURE_SIZE = 1024;
	private static final int PADDING = 2;

	private final NativeImageBackedTexture texture;
	private final int ascent;
	private final int lineHeight;

	private final int[] glyphX = new int[LAST + 1];
	private final int[] glyphY = new int[LAST + 1];
	private final int[] glyphW = new int[LAST + 1];
	private final int[] advance = new int[LAST + 1];

	GlyphAtlas(Font font) {
		BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setFont(font);
		g.setColor(java.awt.Color.WHITE);

		FontMetrics metrics = g.getFontMetrics();
		ascent = metrics.getAscent();
		lineHeight = metrics.getAscent() + metrics.getDescent();

		int x = PADDING;
		int y = PADDING;
		for (int c = FIRST; c <= LAST; c++) {
			String letter = String.valueOf((char) c);
			int width = metrics.charWidth((char) c);
			int cell = width + PADDING * 2;

			if (x + cell > TEXTURE_SIZE) {
				x = PADDING;
				y += lineHeight + PADDING * 2;
			}

			g.drawString(letter, x + PADDING, y + ascent);
			glyphX[c] = x;
			glyphY[c] = y;
			glyphW[c] = cell;
			advance[c] = width;
			x += cell;
		}
		g.dispose();

		texture = new NativeImageBackedTexture(image);
		GlStateManager.bindTexture(texture.getGlId());
		// Filtro linear: as letras ficam lisas mesmo com escala fracionada.
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
	}

	/** @return altura de uma linha, em pixels reais */
	int lineHeight() {
		return lineHeight;
	}

	/** @return largura do texto, em pixels reais (sem espaçamento extra) */
	int width(String text) {
		int total = 0;
		for (int i = 0; i < text.length(); i++) {
			total += advance[index(text.charAt(i))];
		}
		return total;
	}

	/**
	 * Desenha o texto com o canto superior esquerdo em (x, y), já na escala atual da matriz.
	 *
	 * @param text  texto
	 * @param x     esquerda
	 * @param y     topo
	 * @param scale    unidades de tela por pixel real
	 * @param tracking espaço extra entre as letras, em unidades de tela
	 * @param argb     cor
	 */
	void draw(String text, float x, float y, float scale, float tracking, int argb) {
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.bindTexture(texture.getGlId());
		GlStateManager.color(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F, ((argb >>> 24) & 0xFF) / 255F);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);

		float cursor = x - PADDING * scale;
		float top = y - PADDING * scale;
		float height = (lineHeight + PADDING * 2) * scale;
		for (int i = 0; i < text.length(); i++) {
			int c = index(text.charAt(i));
			float w = glyphW[c] * scale;
			double u0 = glyphX[c] / (double) TEXTURE_SIZE;
			double v0 = glyphY[c] / (double) TEXTURE_SIZE;
			double u1 = (glyphX[c] + glyphW[c]) / (double) TEXTURE_SIZE;
			double v1 = (glyphY[c] + lineHeight + PADDING * 2) / (double) TEXTURE_SIZE;

			buffer.vertex(cursor, top + height, 0).texture(u0, v1).next();
			buffer.vertex(cursor + w, top + height, 0).texture(u1, v1).next();
			buffer.vertex(cursor + w, top, 0).texture(u1, v0).next();
			buffer.vertex(cursor, top, 0).texture(u0, v0).next();

			cursor += advance[c] * scale + tracking;
		}
		tessellator.draw();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	/** Libera a textura (quando o tamanho muda e este atlas não é mais usado). */
	void release() {
		GlStateManager.deleteTexture(texture.getGlId());
	}

	private static int index(char c) {
		return c >= FIRST && c <= LAST ? c : '?';
	}
}
