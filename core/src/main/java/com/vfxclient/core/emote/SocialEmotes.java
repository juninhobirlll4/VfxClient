package com.vfxclient.core.emote;

import static com.vfxclient.core.emote.Poses.arms;
import static com.vfxclient.core.emote.Poses.head;
import static com.vfxclient.core.emote.Poses.leftArm;
import static com.vfxclient.core.emote.Poses.mix;
import static com.vfxclient.core.emote.Poses.pulse;
import static com.vfxclient.core.emote.Poses.rightArm;
import static com.vfxclient.core.emote.Poses.s;

import java.util.Arrays;
import java.util.List;

/** Página 2: gestos sociais (tchau, joinha, coração...). */
final class SocialEmotes {

	private SocialEmotes() {
	}

	static List<Emote> all() {
		return Arrays.asList(
				new Emote("tchau", "Tchau", 2.5F, (m, t, b) -> {
					rightArm(m, 0.5F + 0.4F * s(t, 11), 1F, 0F, b);
					leftArm(m, 0.5F + 0.4F * s(t + 0.3F, 11), 1F, 0F, b);
				}),
				new Emote("joinha", "Joinha", 2.5F, (m, t, b) -> {
					rightArm(m, -0.1F, 0.6F, 1F, b);
					head(m, 8 * s(t, 6), 0, 0, b);
				}),
				new Emote("coracao", "Coração", 3F, (m, t, b) -> {
					arms(m, -0.3F - 0.1F * pulse(t, 7), 1F, 0.25F, b);
					head(m, -8, 0, 0, b);
				}),
				new Emote("musculos", "Músculos", 3F, (m, t, b) -> {
					arms(m, 1F, 0.45F + 0.2F * pulse(t, 6), 0F, b);
					head(m, 6, 0, 5 * s(t, 3), b);
				}),
				new Emote("vitoria", "Vitória", 3F, (m, t, b) -> {
					arms(m, 0.6F, 1F + 0.12F * s(t, 7), 0F, b);
					head(m, -15, 0, 0, b);
				}),
				new Emote("respeito", "Respeito", 2.5F, (m, t, b) -> {
					rightArm(m, -0.7F, 0.2F, 0.75F, b);
					head(m, 10, 0, 0, b);
				}),
				new Emote("beijo", "Beijo", 3F, (m, t, b) -> {
					// Leva a mão à boca e depois a joga para frente.
					float throwing = pulse(t, 4.2F);
					rightArm(m, mix(-0.1F, 0.9F, throwing), 0.6F, mix(0.9F, 0.2F, throwing), b);
					head(m, 0, 0, 6, b);
				}),
				new Emote("surpresa", "Surpresa", 2.5F, (m, t, b) -> {
					arms(m, -0.2F, 0.75F, 0.75F, b);
					head(m, -12 + 2 * s(t, 25), 0, 0, b);
				}));
	}
}
