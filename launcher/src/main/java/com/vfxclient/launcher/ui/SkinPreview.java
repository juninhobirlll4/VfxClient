package com.vfxclient.launcher.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * Desenha a skin vista de frente (cabeça, corpo, braços e pernas, com as camadas externas),
 * pixel a pixel, sem suavização, como dentro do jogo.
 */
public final class SkinPreview extends Canvas {

	private static final int UNIT = 16;
	private static final int UNITS_H = 32;

	private Image skin;
	private boolean slim;

	/**
	 * @param scale tamanho de cada pixel da skin na tela
	 */
	public SkinPreview(double scale) {
		super(UNIT * scale, UNITS_H * scale);
		draw();
	}

	/**
	 * @param skin imagem da skin (64x64 ou 64x32) ou {@code null}
	 * @param slim braço fino (3 px)
	 */
	public void setSkin(Image skin, boolean slim) {
		this.skin = skin;
		this.slim = slim;
		draw();
	}

	private void draw() {
		GraphicsContext g = getGraphicsContext2D();
		g.clearRect(0, 0, getWidth(), getHeight());
		double s = getWidth() / UNIT;
		if (skin == null || skin.isError() || skin.getWidth() != 64) {
			drawPlaceholder(g, s);
			return;
		}
		PixelReader px = skin.getPixelReader();
		boolean modern = skin.getHeight() == 64;
		int arm = slim ? 3 : 4;
		double armLeftX = 4 - arm;

		// Camada base.
		part(g, px, s, 8, 8, 8, 8, 4, 0, false);                   // cabeça
		part(g, px, s, 20, 20, 8, 12, 4, 8, false);                // corpo
		part(g, px, s, 44, 20, arm, 12, armLeftX, 8, false);       // braço direito (à esquerda na tela)
		part(g, px, s, 4, 20, 4, 12, 4, 20, false);                // perna direita
		if (modern) {
			part(g, px, s, 36, 52, arm, 12, 12, 8, false);         // braço esquerdo
			part(g, px, s, 20, 52, 4, 12, 8, 20, false);           // perna esquerda
		} else {
			part(g, px, s, 44, 20, arm, 12, 12, 8, true);          // skins antigas: espelha o braço
			part(g, px, s, 4, 20, 4, 12, 8, 20, true);
		}

		// Camada externa (chapéu, jaqueta, mangas, calças).
		part(g, px, s, 40, 8, 8, 8, 4, 0, false);
		if (modern) {
			part(g, px, s, 20, 36, 8, 12, 4, 8, false);
			part(g, px, s, 44, 36, arm, 12, armLeftX, 8, false);
			part(g, px, s, 52, 52, arm, 12, 12, 8, false);
			part(g, px, s, 4, 36, 4, 12, 4, 20, false);
			part(g, px, s, 4, 52, 4, 12, 8, 20, false);
		}
	}

	/** Copia um retângulo da textura para a tela, pulando pixels transparentes. */
	private static void part(GraphicsContext g, PixelReader px, double s, int u, int v, int w, int h,
			double dx, double dy, boolean mirror) {
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int sx = mirror ? u + w - 1 - x : u + x;
				Color color = px.getColor(sx, v + y);
				if (color.getOpacity() < 0.05) {
					continue;
				}
				g.setFill(color);
				g.fillRect((dx + x) * s, (dy + y) * s, s + 0.5, s + 0.5);
			}
		}
	}

	private static void drawPlaceholder(GraphicsContext g, double s) {
		g.setFill(Color.web("#2a1f48"));
		g.fillRect(4 * s, 0, 8 * s, 8 * s);
		g.fillRect(4 * s, 8 * s, 8 * s, 12 * s);
		g.fillRect(0, 8 * s, 4 * s, 12 * s);
		g.fillRect(12 * s, 8 * s, 4 * s, 12 * s);
		g.fillRect(4 * s, 20 * s, 8 * s, 12 * s);
		g.setFill(Color.web("#B48CFF"));
		g.fillText("sem skin", 2.5 * s, 14.5 * s);
	}
}
