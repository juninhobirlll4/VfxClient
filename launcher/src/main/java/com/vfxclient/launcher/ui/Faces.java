package com.vfxclient.launcher.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/** Recorta o rosto de uma skin (8x8 + camada do chapéu) e amplia sem suavizar. */
public final class Faces {

	private Faces() {
	}

	/**
	 * @param skin  textura da skin (64x64 ou 64x32)
	 * @param scale quantos pixels por pixel da skin
	 * @return o rosto, ou {@code null} se a imagem não for uma skin
	 */
	public static Image face(Image skin, int scale) {
		if (skin == null || skin.isError() || skin.getWidth() < 64) {
			return null;
		}
		WritableImage out = new WritableImage(8 * scale, 8 * scale);
		PixelReader px = skin.getPixelReader();
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				int argb = px.getArgb(8 + x, 8 + y);
				int hat = px.getArgb(40 + x, 8 + y);
				if ((hat >>> 24) > 10) {
					argb = hat;
				}
				for (int dx = 0; dx < scale; dx++) {
					for (int dy = 0; dy < scale; dy++) {
						out.getPixelWriter().setArgb(x * scale + dx, y * scale + dy, argb | 0xFF000000);
					}
				}
			}
		}
		return out;
	}
}
