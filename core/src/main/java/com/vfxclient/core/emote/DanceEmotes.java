package com.vfxclient.core.emote;

import static com.vfxclient.core.emote.Poses.arms;
import static com.vfxclient.core.emote.Poses.head;
import static com.vfxclient.core.emote.Poses.leftArm;
import static com.vfxclient.core.emote.Poses.leftLeg;
import static com.vfxclient.core.emote.Poses.rightArm;
import static com.vfxclient.core.emote.Poses.rightLeg;
import static com.vfxclient.core.emote.Poses.s;
import static com.vfxclient.core.emote.Poses.step;

import java.util.Arrays;
import java.util.List;

/** Página 3: danças. Todas repetem até o jogador se mexer. */
final class DanceEmotes {

	private DanceEmotes() {
	}

	static List<Emote> all() {
		return Arrays.asList(
				new Emote("robo", "Robô", 0F, (m, t, b) -> {
					// Movimentos travados, trocando de pose a cada instante.
					float p = step(t, 3);
					rightArm(m, 1F, p, 0F, b);
					leftArm(m, 1F, 1F - p, 0F, b);
					head(m, 0, 30 * (p * 2 - 1), 0, b);
				}),
				new Emote("balanco", "Balançar", 0F, (m, t, b) -> {
					float sway = s(t, 5);
					rightArm(m, 0.4F + 0.5F * sway, -0.6F, 0F, b);
					leftArm(m, 0.4F - 0.5F * sway, -0.6F, 0F, b);
					head(m, 0, 0, 10 * sway, b);
				}),
				new Emote("disco", "Disco", 0F, (m, t, b) -> {
					float p = step(t, 2.5F);
					rightArm(m, 0.5F, p > 0 ? 1F : -0.8F, 0F, b);
					leftArm(m, 0.6F, p > 0 ? -0.8F : 1F, 0F, b);
					rightLeg(m, 0.15F, -1F, 0F, b);
					leftLeg(m, 0.15F, -1F, 0F, b);
					head(m, 0, 0, p > 0 ? 10 : -10, b);
				}),
				new Emote("ondinha", "Ondinha", 0F, (m, t, b) -> {
					rightArm(m, 1F, 0.7F * s(t, 6), 0F, b);
					leftArm(m, 1F, 0.7F * s(t - 0.3F, 6), 0F, b);
				}),
				new Emote("passinho", "Passinho", 0F, (m, t, b) -> {
					float beat = s(t, 7);
					rightArm(m, 0F, 0.8F * beat, 1F, b);
					leftArm(m, 0F, -0.8F * beat, 1F, b);
					rightLeg(m, 0.05F, -1F, 0.5F * beat, b);
					leftLeg(m, 0.05F, -1F, -0.5F * beat, b);
				}),
				new Emote("twist", "Twist", 0F, (m, t, b) -> {
					float beat = s(t, 9);
					rightLeg(m, 0.2F + 0.2F * beat, -1F, 0F, b);
					leftLeg(m, 0.2F - 0.2F * beat, -1F, 0F, b);
					rightArm(m, 0.3F, -0.4F, 0.8F * beat, b);
					leftArm(m, 0.3F, -0.4F, -0.8F * beat, b);
					head(m, 0, 15 * beat, 0, b);
				}),
				new Emote("moonwalk", "Moonwalk", 0F, (m, t, b) -> {
					float slide = s(t, 4);
					rightLeg(m, 0F, -1F, 0.6F * slide, b);
					leftLeg(m, 0F, -1F, -0.6F * slide, b);
					arms(m, 0F, 0.3F, 0.7F, b);
					head(m, -8, 0, 0, b);
				}),
				new Emote("festa", "Festa", 0F, (m, t, b) -> {
					float pump = s(t, 10);
					rightArm(m, 0.35F, 1F, 0.3F * pump, b);
					leftArm(m, 0.35F, 1F, -0.3F * pump, b);
					rightLeg(m, 0.15F, -1F, 0.25F * s(t, 5), b);
					leftLeg(m, 0.15F, -1F, -0.25F * s(t, 5), b);
					head(m, -10, 0, 8 * s(t, 5), b);
				}));
	}
}
