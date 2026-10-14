package com.vfxclient.core.gui.font;

import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.vfxclient.core.VfxCore;

/**
 * Uma fonte TrueType (ex.: Montserrat ExtraBold) carregada do mod, com um atlas de letras para
 * cada tamanho em pixels reais que já foi pedido.
 */
public final class FontFamily {

	private final String resource;
	private final Map<Integer, GlyphAtlas> atlases = new HashMap<Integer, GlyphAtlas>();
	private Font base;
	private boolean failed;

	/** @param resource caminho do .ttf dentro do mod */
	FontFamily(String resource) {
		this.resource = resource;
	}

	/**
	 * @param pixels altura pedida, em pixels reais
	 * @return atlas nesse tamanho, ou {@code null} se a fonte não carregou
	 */
	GlyphAtlas atlas(int pixels) {
		Font font = base();
		if (font == null) {
			return null;
		}

		int size = Math.max(4, Math.min(160, pixels));
		GlyphAtlas atlas = atlases.get(size);
		if (atlas == null) {
			atlas = new GlyphAtlas(font.deriveFont((float) size));
			atlases.put(size, atlas);
			trimCache(size);
		}
		return atlas;
	}

	private Font base() {
		if (base == null && !failed) {
			InputStream in = FontFamily.class.getResourceAsStream(resource);
			try {
				base = Font.createFont(Font.TRUETYPE_FONT, in);
			} catch (Exception e) {
				failed = true;
				VfxCore.LOGGER.warn("Fonte {} não carregou: {}", resource, e.toString());
			} finally {
				closeQuietly(in);
			}
		}
		return base;
	}

	/** Ao redimensionar a janela os tamanhos mudam; guarda só os mais recentes. */
	private void trimCache(int keep) {
		if (atlases.size() <= 6) {
			return;
		}
		for (Integer size : atlases.keySet().toArray(new Integer[0])) {
			if (size != keep && atlases.size() > 6) {
				atlases.remove(size).release();
			}
		}
	}

	private static void closeQuietly(InputStream in) {
		if (in == null) {
			return;
		}
		try {
			in.close();
		} catch (Exception ignored) {
			// Nada a fazer.
		}
	}
}
