package com.vfxclient.core.gui;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Carrega a logo do VfxClient direto do jar e desenha em qualquer tamanho.
 *
 * <p>A textura é registrada na primeira vez que é usada (precisa do OpenGL pronto).</p>
 */
public final class Logo {

	private static final String RESOURCE = "/assets/vfx/icon.png";

	private static Identifier texture;
	private static int textureSize;
	private static boolean failed;

	private Logo() {
	}

	/**
	 * Desenha a logo quadrada.
	 *
	 * @param x    esquerda
	 * @param y    topo
	 * @param size lado em pixels escalados
	 */
	public static void draw(int x, int y, int size) {
		if (!ensureLoaded()) {
			return;
		}
		GlStateManager.enableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
		MinecraftClient.getInstance().getTextureManager().bindTexture(texture);
		DrawableHelper.drawTexture(x, y, 0F, 0F, textureSize, textureSize, size, size, textureSize, textureSize);
		GlStateManager.disableBlend();
	}

	private static boolean ensureLoaded() {
		if (texture != null) {
			return true;
		}
		if (failed) {
			return false;
		}
		try (InputStream in = Logo.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("recurso não encontrado: " + RESOURCE);
			}
			BufferedImage image = ImageIO.read(in);
			textureSize = image.getWidth();
			texture = MinecraftClient.getInstance().getTextureManager()
					.registerDynamicTexture("vfx_logo", new NativeImageBackedTexture(image));
			return true;
		} catch (Exception e) {
			failed = true;
			VfxCore.LOGGER.warn("Não foi possível carregar a logo", e);
			return false;
		}
	}
}
