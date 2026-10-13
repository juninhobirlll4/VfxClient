package com.vfxclient.core.gui;

/**
 * Cores do VfxClient no formato ARGB ({@code 0xAARRGGBB}).
 *
 * <p>Espelha a paleta de {@code branding/README.md}; mantenha os dois em sincronia.</p>
 */
public final class Theme {

	public static final int PRIMARY_DEEP = 0xFF5B16D0;
	public static final int PRIMARY = 0xFF7B2FF7;
	public static final int PRIMARY_LIGHT = 0xFFB48CFF;
	public static final int WHITE = 0xFFFFFFFF;

	public static final int BACKDROP = 0x88000000;
	public static final int PANEL = 0xF20D0A14;
	public static final int SIDEBAR = 0xFF151021;
	public static final int CARD = 0xFF1B1529;
	public static final int CARD_HOVER = 0xFF251C3A;
	public static final int CARD_ENABLED = 0xFF27194A;
	public static final int TRACK_OFF = 0xFF332A48;
	public static final int DIVIDER = 0x18FFFFFF;

	public static final int TEXT = 0xFFF4EFFF;
	public static final int TEXT_MUTED = 0xFFA99CC4;

	public static final int HUD_BACKGROUND = 0x590E0A16;

	private Theme() {
	}

	/**
	 * Mistura duas cores ARGB.
	 *
	 * @param from cor inicial
	 * @param to   cor final
	 * @param t    de 0 ({@code from}) a 1 ({@code to})
	 * @return cor intermediária
	 */
	public static int lerp(int from, int to, float t) {
		t = Math.max(0F, Math.min(1F, t));
		int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * Multiplica o canal alfa de uma cor (usado nas animações de abertura).
	 *
	 * @param color cor ARGB
	 * @param alpha fator de 0 a 1
	 * @return cor com alfa ajustado
	 */
	public static int withAlpha(int color, float alpha) {
		int a = (int) (((color >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, alpha)));
		return (a << 24) | (color & 0x00FFFFFF);
	}
}
