package com.vfxclient.core.emote;

import static com.vfxclient.core.emote.Poses.arms;
import static com.vfxclient.core.emote.Poses.head;
import static com.vfxclient.core.emote.Poses.rightArm;
import static com.vfxclient.core.emote.Poses.s;

import java.util.Arrays;
import java.util.List;

/** Página 4: reações (rir, chorar, raiva, medo...). */
final class ReactionEmotes {

	private ReactionEmotes() {
	}

	static List<Emote> all() {
		return Arrays.asList(
				new Emote("rir", "Rir", 3F, (m, t, b) -> {
					arms(m, 0.1F, -0.3F, 0.9F, b);
					head(m, -25 + 6 * s(t, 22), 0, 4 * s(t, 11), b);
				}),
				new Emote("chorar", "Chorar", 3.5F, (m, t, b) -> {
					arms(m, -0.1F, 0.85F, 0.6F, b);
					head(m, 30 + 3 * s(t, 18), 0, 0, b);
				}),
				new Emote("raiva", "Raiva", 3F, (m, t, b) -> {
					arms(m, 0.25F, -1F, 0.15F, b);
					head(m, 10, 20 * s(t, 16), 0, b);
				}),
				new Emote("medo", "Medo", 3F, (m, t, b) -> {
					arms(m, 0.3F + 0.06F * s(t, 25), 0.55F, 0.6F, b);
					head(m, -8, 6 * s(t, 22), 0, b);
				}),
				new Emote("duvida", "Dúvida", 3F, (m, t, b) -> {
					arms(m, 0.8F, 0.15F + 0.1F * s(t, 3), 0.3F, b);
					head(m, 0, 12 * s(t, 2), 14, b);
				}),
				new Emote("sono", "Sono", 4F, (m, t, b) -> {
					arms(m, 0.1F, -1F, 0.35F + 0.05F * s(t, 2), b);
					head(m, 38 + 3 * s(t, 2), 0, 6 * s(t, 1.5F), b);
				}),
				new Emote("cansado", "Cansado", 3.5F, (m, t, b) -> {
					arms(m, 0.15F, -0.8F, 0.6F, b);
					head(m, 25 + 4 * s(t, 3), 0, 0, b);
				}),
				new Emote("tedio", "Tédio", 4F, (m, t, b) -> {
					// Apoia a cabeça na mão.
					rightArm(m, -0.4F, 0.8F, 0.3F, b);
					head(m, 10, 6 * s(t, 1.2F), 15, b);
				}));
	}
}
