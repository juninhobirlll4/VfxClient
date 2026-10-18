package com.vfxclient.core.emote;

import static com.vfxclient.core.emote.Poses.arms;
import static com.vfxclient.core.emote.Poses.head;
import static com.vfxclient.core.emote.Poses.leftArm;
import static com.vfxclient.core.emote.Poses.leftLeg;
import static com.vfxclient.core.emote.Poses.rightArm;
import static com.vfxclient.core.emote.Poses.rightLeg;
import static com.vfxclient.core.emote.Poses.s;

import java.util.Arrays;
import java.util.List;

/** Página 7: poses (herói, relaxar, dab, floss...). */
final class PoseEmotes {

	private PoseEmotes() {
	}

	static List<Emote> all() {
		return Arrays.asList(
				new Emote("heroi", "Herói", 3F, (m, t, b) -> {
					arms(m, 0.7F, -0.7F, 0F, b);
					head(m, -10, 0, 0, b);
				}),
				new Emote("relaxar", "Relaxar", 4F, (m, t, b) -> {
					arms(m, 0.5F, 0.9F, -0.2F, b);
					rightLeg(m, 0.1F, -1F, 0F, b);
					leftLeg(m, 0.1F, -1F, 0F, b);
					head(m, -5, 0, 0, b);
				}),
				new Emote("dab", "Dab", 2.5F, (m, t, b) -> {
					rightArm(m, -0.5F, 0.6F, 1F, b);
					leftArm(m, 1F, 0.6F, -0.2F, b);
					head(m, 35, 0, 0, b);
				}),
				new Emote("floss", "Floss", 0F, (m, t, b) -> {
					// Os dois braços balançam para o mesmo lado, trocando de lado.
					float sway = s(t, 9);
					rightArm(m, 0.2F + 0.7F * sway, -0.6F, 0.5F, b);
					leftArm(m, 0.2F - 0.7F * sway, -0.6F, 0.5F, b);
					rightLeg(m, 0.1F + 0.1F * sway, -1F, 0F, b);
					leftLeg(m, 0.1F - 0.1F * sway, -1F, 0F, b);
					head(m, 0, 0, 8 * sway, b);
				}),
				new Emote("sentar", "Sentar", 4F, (m, t, b) -> {
					rightLeg(m, 0.12F, 0F, 1F, b);
					leftLeg(m, 0.12F, 0F, 1F, b);
					arms(m, 0.2F, -0.6F, 0.5F, b);
				}),
				new Emote("rezar", "Rezar", 4F, (m, t, b) -> {
					arms(m, -0.08F, 0.35F, 1F, b);
					head(m, 18, 0, 0, b);
				}),
				new Emote("estatua", "Estátua", 4F, (m, t, b) -> {
					rightArm(m, 0.3F, 1F, -0.3F, b);
					leftArm(m, 0.3F, -0.2F, 0.9F, b);
					rightLeg(m, 0.15F, -1F, 0.4F, b);
					leftLeg(m, 0.1F, -1F, -0.3F, b);
					head(m, -6, 18, 0, b);
				}),
				new Emote("gritar", "Gritar", 3F, (m, t, b) -> {
					float shake = 0.08F * s(t, 22);
					arms(m, 0.75F + shake, 0.7F, 0.1F, b);
					head(m, -28, 0, 0, b);
				}));
	}
}
