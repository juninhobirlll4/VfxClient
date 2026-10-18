package com.vfxclient.core.emote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Todos os emotes do VfxClient, em páginas de 8 (uma por roda). As animações são nossas e ficam
 * nos arquivos {@code *Emotes} desta pasta, uma página por arquivo.
 *
 * <p>Os ids vão pela rede: não troque o id de um emote já publicado.</p>
 */
public final class Emotes {

	/** Emotes por página da roda. */
	public static final int PER_PAGE = 8;

	/** Nome de cada página, na ordem. */
	public static final List<String> PAGE_NAMES = Collections.unmodifiableList(Arrays.asList(
			"Cumprimentos", "Social", "Danças", "Reações", "Gestos", "Ação", "Poses"));

	/** Todos, na ordem das páginas. */
	public static final List<Emote> ALL;

	private static final Map<String, Emote> BY_ID = new HashMap<String, Emote>();

	static {
		List<Emote> all = new ArrayList<Emote>();
		all.addAll(GreetingEmotes.all());
		all.addAll(SocialEmotes.all());
		all.addAll(DanceEmotes.all());
		all.addAll(ReactionEmotes.all());
		all.addAll(GestureEmotes.all());
		all.addAll(ActionEmotes.all());
		all.addAll(PoseEmotes.all());

		for (Emote emote : all) {
			if (BY_ID.put(emote.id(), emote) != null) {
				throw new IllegalStateException("Emote com id repetido: " + emote.id());
			}
		}
		ALL = Collections.unmodifiableList(all);
	}

	private Emotes() {
	}

	/** @return quantas páginas a roda tem */
	public static int pages() {
		return (ALL.size() + PER_PAGE - 1) / PER_PAGE;
	}

	/**
	 * @param index número da página (a partir de 0)
	 * @return os emotes dessa página
	 */
	public static List<Emote> page(int index) {
		int from = Math.max(0, Math.min(index, pages() - 1)) * PER_PAGE;
		return ALL.subList(from, Math.min(from + PER_PAGE, ALL.size()));
	}

	/** @return o emote com esse id, ou {@code null} se não existir (versão mais nova de outro jogador) */
	public static Emote byId(String id) {
		return BY_ID.get(id);
	}
}
