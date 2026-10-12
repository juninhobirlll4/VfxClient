package com.vfxclient.core.module.setting;

/** Escolha de cor a partir de uma paleta fixa (cicla na Click GUI como uma {@link ChoiceSetting}). */
public class ColorSetting extends ChoiceSetting {

	private static final String[] NAMES = {
			"Branco", "Roxo VfxClient", "Lilás", "Vermelho", "Laranja", "Amarelo",
			"Verde", "Ciano", "Azul", "Rosa", "Preto"
	};
	private static final int[] COLORS = {
			0xFFFFFF, 0x7B2FF7, 0xB48CFF, 0xFF4040, 0xFF9A2E, 0xFFE14D,
			0x4DFF6A, 0x4DE8FF, 0x4D7BFF, 0xFF6FD8, 0x000000
	};

	/** Índice do roxo da marca, para usar como padrão. */
	public static final int BRAND = 1;
	/** Índice do branco. */
	public static final int WHITE = 0;
	/** Índice do vermelho. */
	public static final int RED = 3;
	/** Índice do preto. */
	public static final int BLACK = 10;

	/**
	 * @param id           chave do JSON
	 * @param name         nome exibido
	 * @param defaultIndex cor padrão (use as constantes desta classe)
	 */
	public ColorSetting(String id, String name, int defaultIndex) {
		super(id, name, defaultIndex, NAMES);
	}

	/** @return cor atual em RGB ({@code 0xRRGGBB}) */
	public int rgb() {
		return COLORS[get()];
	}

	/**
	 * @param alpha opacidade de 0 a 255
	 * @return cor atual em ARGB
	 */
	public int argb(int alpha) {
		return (alpha & 0xFF) << 24 | rgb();
	}
}
