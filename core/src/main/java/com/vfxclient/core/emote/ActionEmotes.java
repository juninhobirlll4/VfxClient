package com.vfxclient.core.emote;

import static com.vfxclient.core.emote.Poses.arms;
import static com.vfxclient.core.emote.Poses.head;
import static com.vfxclient.core.emote.Poses.leftArm;
import static com.vfxclient.core.emote.Poses.leftLeg;
import static com.vfxclient.core.emote.Poses.mix;
import static com.vfxclient.core.emote.Poses.pulse;
import static com.vfxclient.core.emote.Poses.rightArm;
import static com.vfxclient.core.emote.Poses.rightLeg;
import static com.vfxclient.core.emote.Poses.s;

import java.util.Arrays;
import java.util.List;

/** Página 6: ação (boxe, karatê, espada, arco...). */
final class ActionEmotes {

	private ActionEmotes() {
	}

	static List<Emote> all() {
		return Arrays.asList(
				new Emote("boxe", "Boxe", 0F, (m, t, b) -> {
					// Guarda alta; cada braço soca para frente na sua vez.
					float right = Math.max(0F, s(t, 9));
					float left = Math.max(0F, -s(t, 9));
					rightArm(m, -0.2F * (1F - right), 0.5F, 0.8F + 0.2F * right, b);
					leftArm(m, -0.2F * (1F - left), 0.5F, 0.8F + 0.2F * left, b);
					rightLeg(m, 0.1F, -1F, 0.3F, b);
					leftLeg(m, 0.1F, -1F, -0.3F, b);
					head(m, 5, 0, 0, b);
				}),
				new Emote("karate", "Karatê", 0F, (m, t, b) -> {
					rightArm(m, 0.9F, 0.6F * s(t, 6), 0.2F, b);
					leftArm(m, 0.9F, -0.6F * s(t, 6), 0.2F, b);
					rightLeg(m, 0.35F, -1F, 0F, b);
					leftLeg(m, 0.35F, -1F, 0F, b);
				}),
				new Emote("espada", "Espada", 3F, (m, t, b) -> {
					float swing = pulse(t, 5);
					rightArm(m, 0.2F, mix(1F, -0.2F, swing), mix(0.2F, 1F, swing), b);
					head(m, 5, 0, 0, b);
				}),
				new Emote("arco", "Arco", 4F, (m, t, b) -> {
					float draw = pulse(t, 2.2F);
					leftArm(m, 0.1F, 0.1F, 1F, b);
					rightArm(m, mix(-0.1F, 1F, draw), 0.15F, mix(0.9F, 0.1F, draw), b);
					head(m, 0, 20, 0, b);
				}),
				new Emote("soco", "Soco no ar", 0F, (m, t, b) -> {
					float punch = pulse(t, 7);
					rightArm(m, 0.15F, mix(0.55F, 1F, punch), mix(0.5F, 0F, punch), b);
					leftArm(m, -0.2F, 0.5F, 0.8F, b);
					rightLeg(m, 0.1F, -1F, 0.3F, b);
					leftLeg(m, 0.1F, -1F, -0.3F, b);
				}),
				new Emote("rendicao", "Rendição", 3F, (m, t, b) -> {
					arms(m, 0.2F, 1F, 0F, b);
					head(m, 15 + 2 * s(t, 20), 0, 0, b);
				}),
				new Emote("defesa", "Defesa", 3F, (m, t, b) -> {
					arms(m, -0.8F, 0.4F, 0.75F, b);
					head(m, 8, 0, 0, b);
				}),
				new Emote("super", "Super-herói", 3F, (m, t, b) -> {
					rightArm(m, 0.1F, 1F, 0.5F, b);
					leftArm(m, 0.6F, -0.5F, 0F, b);
					rightLeg(m, 0.15F, -1F, 0F, b);
					leftLeg(m, 0.15F, -1F, 0F, b);
					head(m, -10, 0, 0, b);
				}));
	}
}
