package com.vfxclient.core.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
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
	 * Contorno arredondado de 1 pixel com o miolo preenchido (cartões do menu).
	 *
	 * @param x      esquerda
	 * @param y      topo
	 * @param w      largura
	 * @param h      altura
	 * @param radius raio dos cantos
	 * @param border cor do contorno
	 * @param fill   cor do miolo (opaca, para o contorno não aparecer por baixo)
	 */
	public static void outlinedRect(float x, float y, float w, float h, float radius, int border, int fill) {
		roundedRect(x + 1, y + 1, w - 2, h - 2, Math.max(0F, radius - 1), fill);
		roundedOutline(x, y, w, h, radius, 1.5F, border);
	}

	/**
	 * Só a borda de um retângulo arredondado (o miolo fica livre, então fundos translúcidos não
	 * mostram a cor da borda por baixo).
	 *
	 * @param thickness espessura da borda
	 */
	public static void roundedOutline(float x, float y, float w, float h, float radius, float thickness, int color) {
		if (w <= 0 || h <= 0) {
			return;
		}
		// O raio nunca fica menor que a espessura, senão a borda some nos cantos retos.
		float r = Math.max(thickness, Math.min(radius, Math.min(w, h) / 2F));
		float inner = r - thickness;
		begin(color);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		if (cull) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}

		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION);
		// Quatro cantos em sequência; o primeiro ponto se repete no fim para fechar a borda.
		float[][] corners = {
			{x + w - r, y + r, 270}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}, {x + r, y + r, 180}
		};
		int steps = 8;
		for (int c = 0; c <= corners.length; c++) {
			float[] corner = corners[c % corners.length];
			int count = c == corners.length ? 0 : steps;
			for (int i = 0; i <= count; i++) {
				double angle = Math.toRadians(corner[2] + 90.0 * i / steps);
				double cos = Math.cos(angle);
				double sin = Math.sin(angle);
				buffer.vertex(corner[0] + cos * r, corner[1] + sin * r, 0D).next();
				buffer.vertex(corner[0] + cos * inner, corner[1] + sin * inner, 0D).next();
			}
		}

		Tessellator.getInstance().draw();
		if (cull) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		}

		end();
	}

	/**
	 * Engrenagem pequena (ícone de opções) desenhada com formas simples.
	 *
	 * @param cx    centro X
	 * @param cy    centro Y
	 * @param r     raio
	 * @param color cor
	 */
	public static void gear(float cx, float cy, float r, int color) {
		for (int i = 0; i < 8; i++) {
			double a = Math.PI / 4 * i;
			float tx = cx + (float) Math.cos(a) * r;
			float ty = cy + (float) Math.sin(a) * r;
			rawRect(tx - 1F, ty - 1F, 2F, 2F, color);
		}
		ring(cx, cy, r - 0.5F, 1.5F, color);
	}

	/**
	 * Triângulo preenchido (usado na estrela de favorito).
	 */
	public static void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
		begin(color);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		if (cull) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION);
		buffer.vertex(x1, y1, 0D).next();
		buffer.vertex(x2, y2, 0D).next();
		buffer.vertex(x3, y3, 0D).next();
		Tessellator.getInstance().draw();
		if (cull) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
		end();
	}

	/**
	 * Linha grossa entre dois pontos, com pontas retas.
	 *
	 * @param width espessura
	 */
	public static void line(float x1, float y1, float x2, float y2, float width, int color) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 0.001F) {
			return;
		}
		// Perpendicular, com metade da espessura.
		float px = -dy / length * width / 2F;
		float py = dx / length * width / 2F;
		quad(x1 + px, y1 + py, x2 + px, y2 + py, x2 - px, y2 - py, x1 - px, y1 - py, color);
	}

	/** Quadrilátero preenchido (os quatro pontos em volta, em qualquer sentido). */
	public static void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int color) {
		begin(color);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		if (cull) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION);
		buffer.vertex(x1, y1, 0D).next();
		buffer.vertex(x2, y2, 0D).next();
		buffer.vertex(x3, y3, 0D).next();
		buffer.vertex(x4, y4, 0D).next();
		Tessellator.getInstance().draw();
		if (cull) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
		end();
	}

	/**
	 * Fatia de anel (uma "casca" entre dois raios e dois ângulos), usada na roda de emotes.
	 *
	 * @param inner raio de dentro
	 * @param outer raio de fora
	 * @param from  ângulo inicial, em radianos (0 = direita, cresce no sentido do relógio)
	 * @param to    ângulo final, em radianos
	 */
	public static void ringSegment(float cx, float cy, float inner, float outer, double from, double to, int color) {
		int steps = Math.max(2, (int) Math.ceil(Math.abs(to - from) / (Math.PI / 24)));
		begin(color);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		if (cull) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION);
		for (int i = 0; i <= steps; i++) {
			double angle = from + (to - from) * i / steps;
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			buffer.vertex(cx + cos * outer, cy + sin * outer, 0D).next();
			buffer.vertex(cx + cos * inner, cy + sin * inner, 0D).next();
		}
		Tessellator.getInstance().draw();
		if (cull) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
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
	 * Anel (círculo vazado), usado pela mira em formato de círculo.
	 *
	 * @param cx        centro X
	 * @param cy        centro Y
	 * @param radius    raio externo
	 * @param thickness espessura do anel
	 * @param color     cor ARGB
	 */
	public static void ring(float cx, float cy, float radius, float thickness, int color) {
		float inner = Math.max(0F, radius - thickness);
		int segments = 32;
		begin(color);
		// A ordem dos vértices de uma faixa não garante o sentido; desliga o culling só aqui
		// (direto no GL e restaurando igual, para não dessincronizar o cache do GlStateManager).
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		if (cull) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}

		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION);
		for (int i = 0; i <= segments; i++) {
			double angle = Math.PI * 2 * i / segments;
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			buffer.vertex(cx + cos * radius, cy + sin * radius, 0D).next();
			buffer.vertex(cx + cos * inner, cy + sin * inner, 0D).next();
		}

		Tessellator.getInstance().draw();
		if (cull) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		}

		end();
	}

	/**
	 * Retângulo sólido que não mexe no modo de mistura atual (para efeitos como inverter cor).
	 *
	 * @param x     esquerda
	 * @param y     topo
	 * @param w     largura
	 * @param h     altura
	 * @param color cor ARGB
	 */
	public static void rawRect(float x, float y, float w, float h, int color) {
		GlStateManager.disableTexture();
		GlStateManager.color(
				((color >> 16) & 0xFF) / 255F,
				((color >> 8) & 0xFF) / 255F,
				(color & 0xFF) / 255F,
				((color >>> 24) & 0xFF) / 255F);
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION);
		buffer.vertex(x, y + h, 0D).next();
		buffer.vertex(x + w, y + h, 0D).next();
		buffer.vertex(x + w, y, 0D).next();
		buffer.vertex(x, y, 0D).next();
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture();
		GlStateManager.color(1F, 1F, 1F, 1F);
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
		gradient(x, y, w, h, left, right, right, left);
	}

	/**
	 * Retângulo com degradê vertical (cima → baixo).
	 *
	 * @param x      esquerda
	 * @param y      topo
	 * @param w      largura
	 * @param h      altura
	 * @param top    cor ARGB de cima
	 * @param bottom cor ARGB de baixo
	 */
	public static void gradientV(float x, float y, float w, float h, int top, int bottom) {
		gradient(x, y, w, h, bottom, bottom, top, top);
	}

	/** Cores dos cantos na ordem: baixo-esquerda, baixo-direita, cima-direita, cima-esquerda. */
	private static void gradient(float x, float y, float w, float h, int bottomLeft, int bottomRight, int topRight, int topLeft) {
		GlStateManager.enableBlend();
		GlStateManager.disableTexture();
		GlStateManager.disableAlphaTest();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
		vertex(buffer, x, y + h, bottomLeft);
		vertex(buffer, x + w, y + h, bottomRight);
		vertex(buffer, x + w, y, topRight);
		vertex(buffer, x, y, topLeft);
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
		DrawableHelper.fill(x, y, x + w, y + 1, color);
		DrawableHelper.fill(x, y + h - 1, x + w, y + h, color);
		DrawableHelper.fill(x, y + 1, x + 1, y + h - 1, color);
		DrawableHelper.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
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
		MinecraftClient client = MinecraftClient.getInstance();
		int factor = new Window(client).getScaleFactor();
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
