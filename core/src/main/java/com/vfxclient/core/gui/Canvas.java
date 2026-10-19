package com.vfxclient.core.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * Área de desenho com tamanho de referência 1920 x 1080, igual às imagens de exemplo das
 * telas. Tudo é desenhado nessas medidas e escalado para a janela, então a tela fica com a
 * mesma cara em qualquer resolução.
 *
 * <p>Se a janela não for 16:9, a área cresce na direção que sobrar (nunca corta nada).</p>
 */
public final class Canvas {

	private static final float BASE_WIDTH = 1920F;
	private static final float BASE_HEIGHT = 1080F;

	private final float unit;
	private final float width;
	private final float height;

	private Canvas(float unit, float width, float height) {
		this.unit = unit;
		this.width = width;
		this.height = height;
	}

	/**
	 * Começa a desenhar em medidas de referência. Termine com {@link #end()}.
	 *
	 * @param guiWidth  largura da tela, em unidades da GUI
	 * @param guiHeight altura da tela, em unidades da GUI
	 * @return a área de desenho
	 */
	public static Canvas begin(int guiWidth, int guiHeight) {
		float unit = Math.min(guiWidth / BASE_WIDTH, guiHeight / BASE_HEIGHT);
		Canvas canvas = new Canvas(unit, guiWidth / unit, guiHeight / unit);

		GlStateManager.pushMatrix();
		GlStateManager.scale(unit, unit, 1F);
		int scaleFactor = new Window(MinecraftClient.getInstance()).getScaleFactor();
		Fonts.setPixelsPerUnit(unit * scaleFactor);
		return canvas;
	}

	/** Volta ao desenho normal da GUI. */
	public void end() {
		GlStateManager.popMatrix();
		Fonts.useGuiScale();
	}

	/** @return quantas unidades da tela cada medida de referência ocupa (para desenhar algo em tamanho real) */
	public float unit() {
		return unit;
	}

	/** @return largura da área, em medidas de referência (1920 ou mais) */
	public float width() {
		return width;
	}

	/** @return altura da área, em medidas de referência (1080 ou mais) */
	public float height() {
		return height;
	}

	/** @return centro horizontal */
	public float centerX() {
		return width / 2F;
	}

	/** @return centro vertical */
	public float centerY() {
		return height / 2F;
	}

	/**
	 * Converte a posição do mouse (unidades da GUI) para medidas de referência.
	 *
	 * @param guiCoordinate posição em unidades da GUI
	 * @return posição na área de desenho
	 */
	public float toCanvas(int guiCoordinate) {
		return guiCoordinate / unit;
	}
}
