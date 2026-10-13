package com.vfxclient.core.module.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

/** Contorno do bloco mirado com cor, espessura e preenchimento escolhidos pelo jogador. */
public class BlockOutlineModule extends Module {

	private final ColorSetting color = add(new ColorSetting("color", "Cor da linha", 0xCC7B2FF7));
	private final NumberSetting width = add(new NumberSetting("width", "Espessura", 2, 1, 6, 0.5, ""));
	private final BooleanSetting fill = add(new BooleanSetting("fill", "Preencher o bloco", false));
	private final ColorSetting fillColor = add(new ColorSetting("fillColor", "Cor do preenchimento", 0x267B2FF7));

	public BlockOutlineModule() {
		super("blockOutline", "Contorno de bloco", "Cor e espessura do contorno", Category.UTILITY, false);
	}

	/**
	 * Desenha o contorno (e o preenchimento) da caixa já deslocada para a câmera.
	 *
	 * @param box caixa do bloco em coordenadas relativas à câmera
	 */
	public void draw(Box box) {

		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableTexture();
		GlStateManager.depthMask(false);

		if (fill.get()) {
			glColor(fillColor.argb());
			fillBox(box);
		}
		glColor(color.argb());
		GL11.glLineWidth(width.get().floatValue());
		WorldRenderer.drawBox(box);
		GL11.glLineWidth(2F);

		GlStateManager.depthMask(true);
		GlStateManager.enableTexture();
		GlStateManager.disableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private static void glColor(int argb) {
		GlStateManager.color(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F, ((argb >>> 24) & 0xFF) / 255F);
	}

	private static void fillBox(Box b) {
		// Desenha as 6 faces nos dois sentidos para aparecer de qualquer lado, sem depender do culling.
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(GL11.GL_QUADS, VertexFormats.POSITION);
		double[][] faces = {
				{b.minX, b.minY, b.minZ, b.maxX, b.minY, b.minZ, b.maxX, b.minY, b.maxZ, b.minX, b.minY, b.maxZ},
				{b.minX, b.maxY, b.minZ, b.minX, b.maxY, b.maxZ, b.maxX, b.maxY, b.maxZ, b.maxX, b.maxY, b.minZ},
				{b.minX, b.minY, b.minZ, b.minX, b.maxY, b.minZ, b.maxX, b.maxY, b.minZ, b.maxX, b.minY, b.minZ},
				{b.minX, b.minY, b.maxZ, b.maxX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ, b.minX, b.maxY, b.maxZ},
				{b.minX, b.minY, b.minZ, b.minX, b.minY, b.maxZ, b.minX, b.maxY, b.maxZ, b.minX, b.maxY, b.minZ},
				{b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ, b.maxX, b.maxY, b.maxZ, b.maxX, b.minY, b.maxZ}
		};
		for (double[] f : faces) {
			for (int i = 0; i < 12; i += 3) {
				buf.vertex(f[i], f[i + 1], f[i + 2]).next();
			}
			for (int i = 9; i >= 0; i -= 3) {
				buf.vertex(f[i], f[i + 1], f[i + 2]).next();
			}
		}
		tessellator.draw();
	}
}
