package com.vfxclient.core.module.setting;

import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Cor ARGB livre (vermelho, verde, azul e transparência), editada na Click GUI pelo
 * seletor de cor: sliders RGB/alfa, código HEX, paleta pronta e prévia ao vivo.
 *
 * <p>No JSON fica como texto {@code "#AARRGGBB"}. Configs antigas (v0.4), que guardavam
 * o índice de uma paleta fixa, são convertidas automaticamente.</p>
 */
public class ColorSetting extends Setting<Integer> {

	/** Branco opaco. */
	public static final int WHITE = 0xFFFFFFFF;
	/** Roxo da marca, opaco. */
	public static final int BRAND = 0xFF7B2FF7;
	/** Vermelho opaco. */
	public static final int RED = 0xFFFF4040;
	/** Preto opaco. */
	public static final int BLACK = 0xFF000000;

	/** Paleta mostrada no seletor (as cores da antiga paleta fixa vêm primeiro, na mesma ordem). */
	public static final int[] PALETTE = {
			0xFFFFFFFF, 0xFF7B2FF7, 0xFFB48CFF, 0xFFFF4040, 0xFFFF9A2E, 0xFFFFE14D,
			0xFF4DFF6A, 0xFF4DE8FF, 0xFF4D7BFF, 0xFFFF6FD8, 0xFF000000, 0xFF5B16D0,
			0xFF9E9E9E, 0xFF2B2B2B, 0xFF00C853, 0xFFFF1744
	};

	/** Quantas cores a paleta antiga (por índice) tinha. */
	private static final int LEGACY_PALETTE_SIZE = 11;

	/**
	 * @param id          chave do JSON
	 * @param name        nome exibido
	 * @param defaultArgb cor padrão em ARGB ({@code 0xAARRGGBB})
	 */
	public ColorSetting(String id, String name, int defaultArgb) {
		super(id, name, defaultArgb);
	}

	/** @return cor atual em ARGB */
	public int argb() {
		return value;
	}

	/**
	 * Cor atual com outra transparência.
	 *
	 * @param alpha opacidade de 0 a 255
	 * @return cor em ARGB
	 */
	public int argb(int alpha) {
		return (alpha & 0xFF) << 24 | rgb();
	}

	/** @return cor atual sem transparência ({@code 0xRRGGBB}) */
	public int rgb() {
		return value & 0xFFFFFF;
	}

	/** @return transparência de 0 a 255 */
	public int alpha() {
		return (value >>> 24) & 0xFF;
	}

	/**
	 * Troca um canal da cor.
	 *
	 * @param channel 0 = alfa, 1 = vermelho, 2 = verde, 3 = azul
	 * @param amount  valor de 0 a 255
	 */
	public void setChannel(int channel, int amount) {
		int shift = (3 - channel) * 8;
		int clamped = Math.max(0, Math.min(255, amount));
		set((value & ~(0xFF << shift)) | (clamped << shift));
	}

	/**
	 * @param channel 0 = alfa, 1 = vermelho, 2 = verde, 3 = azul
	 * @return valor do canal de 0 a 255
	 */
	public int channel(int channel) {
		return (value >>> ((3 - channel) * 8)) & 0xFF;
	}

	/** @return código HEX, ex.: {@code #7B2FF7} (ou {@code #807B2FF7} quando há transparência) */
	public String toHex() {
		if (alpha() == 0xFF) {
			return String.format(Locale.ROOT, "#%06X", rgb());
		}
		return String.format(Locale.ROOT, "#%08X", value);
	}

	/**
	 * Aplica um código HEX digitado ({@code #RGB}, {@code #RRGGBB} ou {@code #AARRGGBB}).
	 *
	 * @param hex texto digitado (com ou sem {@code #})
	 * @return {@code true} se o código era válido
	 */
	public boolean setHex(String hex) {
		Integer parsed = parseHex(hex, alpha());
		if (parsed == null) {
			return false;
		}
		set(parsed);
		return true;
	}

	/**
	 * Converte texto HEX em ARGB.
	 *
	 * @param hex          texto
	 * @param defaultAlpha alfa usado quando o código não traz transparência
	 * @return cor ARGB ou {@code null} se inválido
	 */
	public static Integer parseHex(String hex, int defaultAlpha) {
		if (hex == null) {
			return null;
		}
		String h = hex.trim();
		if (h.startsWith("#")) {
			h = h.substring(1);
		}
		if (!h.matches("[0-9a-fA-F]+")) {
			return null;
		}
		if (h.length() == 3) {
			h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
		}
		if (h.length() == 6) {
			return (defaultAlpha & 0xFF) << 24 | Integer.parseInt(h, 16);
		}
		if (h.length() == 8) {
			return (int) Long.parseLong(h, 16);
		}
		return null;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(String.format(Locale.ROOT, "#%08X", value));
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json == null || !json.isJsonPrimitive()) {
			return;
		}
		if (json.getAsJsonPrimitive().isString()) {
			Integer parsed = parseHex(json.getAsString(), 0xFF);
			if (parsed != null) {
				set(parsed);
			}
		} else if (json.getAsJsonPrimitive().isNumber()) {
			int index = json.getAsInt();
			if (index >= 0 && index < LEGACY_PALETTE_SIZE) {
				set(PALETTE[index]);
			}
		}
	}
}
