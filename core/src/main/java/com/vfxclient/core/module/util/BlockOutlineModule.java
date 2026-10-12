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

	private final ColorSetting color = add(new ColorSetting("color", "Cor", ColorSetting.BRAND));
	private final NumberSetting opacity = add(new NumberSetting("opacity", "Opacidade da linha", 80, 10, 100, 5, "%"));
	private final NumberSetting width = add(new NumberSetting("width", "Espessura", 2, 1, 6, 0.5, ""));
	private final BooleanSetting fill = add(new BooleanSetting("fill", "Preencher o bloco", false));
	private final NumberSetting fillOpacity = add(new NumberSetting("fillOpacity", "Opacidade do preenchimento", 15, 5, 60, 5, "%"));

	public BlockOutlineModule() {
		super("blockOutline", "Contorno de bloco", "Cor e espessura do contorno", Category.UTILITY, false);
	}

	/**
	 * Desenha o contorno (e o preenchimento) da caixa já deslocada para a câmera.
	 *
	 * @param box caixa do bloco em coordenadas relativas à câmera
	 */
	public void draw(Box box) {
		int rgb = color.rgb();
		float r = ((rgb >> 16) & 0xFF) / 255F;
		float g = ((rgb >> 8) & 0xFF) / 255F;
		float b = (rgb & 0xFF) / 255F;

		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableTexture();
		GlStateManager.depthMask(false);

		if (fill.get()) {
			GlStateManager.color(r, g, b, fillOpacity.get().floatValue() / 100F);
			fillBox(box);
		}
		GlStateManager.color(r, g, b, opacity.get().floatValue() / 100F);
		GL11.glLineWidth(width.get().floatValue());
		WorldRenderer.drawBox(box);
		GL11.glLineWidth(2F);

		GlStateManager.depthMask(true);
		GlStateManager.enableTexture();
		GlStateManager.disableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
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
