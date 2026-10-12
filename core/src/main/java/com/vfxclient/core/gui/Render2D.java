package com.vfxclient.core.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Primitivas 2D da interface do VfxClient: retângulos arredondados e degradês.
 *
 * <p>Todas as funções deixam o estado do OpenGL como o Minecraft espera ao desenhar
 * texto (textura ligada, cor branca).</p>
 */
public final class Render2D {

	private static final int CORNER_SEGMENTS = 6;

	private Render2D() {
	}

	/**
	 * Retângulo com cantos arredondados.
	 *
	 * @param x      esquerda
	 * @param y      topo
	 * @param w      largura
	 * @param h      altura
	 * @param radius raio dos cantos
	 * @param color  cor ARGB
	 */
	public static void roundedRect(float x, float y, float w, float h, float radius, int color) {
		if (w <= 0 || h <= 0) {
			return;
		}
		float r = Math.max(0F, Math.min(radius, Math.min(w, h) / 2F));
		begin(color);
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_POLYGON, VertexFormats.POSITION);
		// Sentido anti-horário na tela, igual ao DrawableHelper.fill, para não sofrer culling.
		corner(buffer, x + r, y + h - r, r, 180, 90);
		corner(buffer, x + w - r, y + h - r, r, 90, 0);
		corner(buffer, x + w - r, y + r, r, 360, 270);
		corner(buffer, x + r, y + r, r, 270, 180);
		Tessellator.getInstance().draw();
		end();
	}

	/**
	 * Círculo preenchido.
	 *
	 * @param cx     centro X
	 * @param cy     centro Y
	 * @param radius raio
	 * @param color  cor ARGB
	 */
	public static void circle(float cx, float cy, float radius, int color) {
		roundedRect(cx - radius, cy - radius, radius * 2F, radius * 2F, radius, color);
	}

	/**
	 * Retângulo com degradê horizontal (esquerda → direita).
	 *
	 * @param x     esquerda
	 * @param y     topo
	 * @param w     largura
	 * @param h     altura
	 * @param left  cor ARGB da esquerda
	 * @param right cor ARGB da direita
	 */
	public static void gradientH(float x, float y, float w, float h, int left, int right) {
		GlStateManager.enableBlend();
		GlStateManager.disableTexture();
		GlStateManager.disableAlphaTest();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
		vertex(buffer, x, y + h, left);
		vertex(buffer, x + w, y + h, right);
		vertex(buffer, x + w, y, right);
		vertex(buffer, x, y, left);
		Tessellator.getInstance().draw();
		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.enableAlphaTest();
		GlStateManager.enableTexture();
		GlStateManager.disableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	/**
	 * Contorno de retângulo (linhas de 1 px).
	 *
	 * @param x     esquerda
	 * @param y     topo
	 * @param w     largura
	 * @param h     altura
	 * @param color cor ARGB
	 */
	public static void outline(int x, int y, int w, int h, int color) {
		net.minecraft.client.gui.DrawableHelper.fill(x, y, x + w, y + 1, color);
		net.minecraft.client.gui.DrawableHelper.fill(x, y + h - 1, x + w, y + h, color);
		net.minecraft.client.gui.DrawableHelper.fill(x, y + 1, x + 1, y + h - 1, color);
		net.minecraft.client.gui.DrawableHelper.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	/**
	 * Limita o desenho a uma área da tela (coordenadas escaladas). Chame {@link #endScissor()} depois.
	 *
	 * @param x esquerda
	 * @param y topo
	 * @param w largura
	 * @param h altura
	 */
	public static void scissor(int x, int y, int w, int h) {
		net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		int factor = new net.minecraft.client.util.Window(client).getScaleFactor();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(x * factor, client.height - (y + h) * factor, Math.max(0, w * factor), Math.max(0, h * factor));
	}

	/** Desliga o recorte iniciado por {@link #scissor}. */
	public static void endScissor() {
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

	private static void corner(BufferBuilder buffer, float cx, float cy, float r, int fromDeg, int toDeg) {
		for (int i = 0; i <= CORNER_SEGMENTS; i++) {
			double angle = Math.toRadians(fromDeg + (toDeg - fromDeg) * (i / (double) CORNER_SEGMENTS));
			buffer.vertex(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r, 0D).next();
		}
	}

	private static void vertex(BufferBuilder buffer, float x, float y, int color) {
		buffer.vertex(x, y, 0D)
				.color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
				.next();
	}

	private static void begin(int color) {
		GlStateManager.enableBlend();
		GlStateManager.disableTexture();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.color(
				((color >> 16) & 0xFF) / 255F,
				((color >> 8) & 0xFF) / 255F,
				(color & 0xFF) / 255F,
				((color >>> 24) & 0xFF) / 255F);
	}

	private static void end() {
		GlStateManager.enableTexture();
		GlStateManager.disableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}
}
