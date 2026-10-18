package com.vfxclient.core.emote;

import static com.vfxclient.core.emote.Poses.arms;
import static com.vfxclient.core.emote.Poses.head;
import static com.vfxclient.core.emote.Poses.leftArm;
import static com.vfxclient.core.emote.Poses.mix;
import static com.vfxclient.core.emote.Poses.rightArm;
import static com.vfxclient.core.emote.Poses.s;

import java.util.Arrays;
import java.util.List;

/** Página 5: gestos com a mão (vem cá, pare, silêncio...). */
final class GestureEmotes {

	private GestureEmotes() {
	}

	static List<Emote> all() {
		return Arrays.asList(
				new Emote("venha", "Vem cá", 3F, (m, t, b) -> {
					rightArm(m, -0.05F, 0.35F + 0.55F * s(t, 9), 1F, b);
					head(m, 0, 0, 5, b);
				}),
				new Emote("pare", "Pare", 2.5F, (m, t, b) -> {
					rightArm(m, -0.05F, 0.25F, 1F, b);
					head(m, 5, 0, 0, b);
				}),
				new Emote("silencio", "Silêncio", 3F, (m, t, b) -> {
					rightArm(m, -0.3F, 0.85F, 0.55F, b);
					head(m, 0, 0, 5, b);
				}),
				new Emote("olhar", "Olhar longe", 3F, (m, t, b) -> {
					rightArm(m, -0.35F, 0.75F, 0.7F, b);
					head(m, -4, 28 * s(t, 1.6F), 0, b);
				}),
				new Emote("atencao", "Atenção", 3F, (m, t, b) -> {
					rightArm(m, 0.1F, 1F, 0F, b);
					leftArm(m, 0.7F, -0.45F, 0F, b);
				}),
				new Emote("cumprimentar", "Cumprimentar", 2.5F, (m, t, b) -> {
					rightArm(m, -0.15F, 0.1F + 0.12F * s(t, 8), 1F, b);
					head(m, 8, 0, 0, b);
				}),
				new Emote("apresentar", "Apresentar", 3F, (m, t, b) -> {
					// O braço abre da frente para o lado.
					float sweep = Math.min(1F, t / 1.2F);
					rightArm(m, sweep, 0.2F, 1F - 0.9F * sweep, b);
					head(m, 0, -15 * sweep, 0, b);
				}),
				new Emote("pedir", "Pedir", 3F, (m, t, b) -> {
					arms(m, 0.12F, -0.45F, 1F, b);
					head(m, 0, 0, 8, b);
				}));
	}
}
