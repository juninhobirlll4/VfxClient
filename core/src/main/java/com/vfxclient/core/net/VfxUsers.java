package com.vfxclient.core.net;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.social.EmotesModule;

/**
 * Quem está usando o VfxClient no servidor de Minecraft em que você está, para mostrar a logo ao
 * lado do nick. A lista vem do servidor do VfxClient (mesma sala dos emotes) e começa vazia fora
 * de um servidor.
 */
public final class VfxUsers {

	/**
	 * Marca zero largura no começo do texto: avisa o desenho da Tab que ali vai uma logo. O jogo
	 * ignora o código {@code §z} (não existe), então ela não aparece e não ocupa espaço.
	 */
	public static final String MARK = "§z";

	private static final Set<String> NAMES = ConcurrentHashMap.newKeySet();

	private VfxUsers() {
	}

	public static void add(String name) {
		if (name != null && !name.isEmpty()) {
			NAMES.add(key(name));
		}
	}

	public static void remove(String name) {
		if (name != null) {
			NAMES.remove(key(name));
		}
	}

	public static void clear() {
		NAMES.clear();
	}

	/**
	 * @param name nick do jogador
	 * @return se a logo deve aparecer ao lado do nick (usa o VfxClient e a opção está ligada)
	 */
	public static boolean showsBadge(String name) {
		if (name == null || !NAMES.contains(key(name))) {
			return false;
		}
		EmotesModule module = VfxCore.module(EmotesModule.class);
		return module != null && module.isEnabled() && module.showBadge();
	}

	private static String key(String name) {
		return name.toLowerCase(Locale.ROOT);
	}
}
