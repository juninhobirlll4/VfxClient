package com.vfxclient.core.gui;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Carrega imagens da marca direto do jar e as desenha em qualquer tamanho.
 *
 * <p>O 1.8.9 não registra o namespace {@code vfx} no gerenciador de recursos, então as
 * imagens são lidas do classpath e viram texturas dinâmicas na primeira vez que são usadas
 * (precisa do OpenGL pronto).</p>
 */
public final class Logo {

	/** Ícone quadrado (V roxo). */
	public static final String ICON = "/assets/vfx/icon.png";
	/** Logotipo horizontal (ícone + "VfxClient"), proporção 4:1. */
	public static final String WORDMARK = "/assets/vfx/textures/gui/wordmark.png";

	private static final Map<String, Loaded> CACHE = new HashMap<String, Loaded>();

	private Logo() {
	}

	/**
	 * Desenha o ícone quadrado.
	 *
	 * @param x    esquerda
	 * @param y    topo
	 * @param size lado em pixels escalados
	 */
	public static void draw(int x, int y, int size) {
		draw(ICON, x, y, size, size);
	}

	/**
	 * Desenha uma imagem da marca.
	 *
	 * @param resource caminho no jar (use as constantes desta classe)
	 * @param x        esquerda
	 * @param y        topo
	 * @param w        largura
	 * @param h        altura
	 */
	public static void draw(String resource, int x, int y, int w, int h) {
		Loaded image = load(resource);
		if (image == null) {
			return;
		}
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(1F, 1F, 1F, 1F);
		MinecraftClient.getInstance().getTextureManager().bindTexture(image.id);
		DrawableHelper.drawTexture(x, y, 0F, 0F, image.width, image.height, w, h, image.width, image.height);
		GlStateManager.disableBlend();
	}

	private static Loaded load(String resource) {
		if (CACHE.containsKey(resource)) {
			return CACHE.get(resource);
		}

		Loaded loaded = null;
		try (InputStream in = Logo.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("recurso não encontrado: " + resource);
			}
			BufferedImage image = ImageIO.read(in);
			Identifier id = MinecraftClient.getInstance().getTextureManager()
					.registerDynamicTexture("vfx_" + CACHE.size(), new NativeImageBackedTexture(image));
			loaded = new Loaded(id, image.getWidth(), image.getHeight());
		} catch (Exception e) {
			VfxCore.LOGGER.warn("Não foi possível carregar {}", resource, e);
		}

		CACHE.put(resource, loaded);
		return loaded;
	}

	private static final class Loaded {
		private final Identifier id;
		private final int width;
		private final int height;

		private Loaded(Identifier id, int width, int height) {
			this.id = id;
			this.width = width;
			this.height = height;
		}
	}
}
