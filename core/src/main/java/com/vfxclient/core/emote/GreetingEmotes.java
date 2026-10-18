package com.vfxclient.core.emote;

import static com.vfxclient.core.emote.Poses.arms;
import static com.vfxclient.core.emote.Poses.head;
import static com.vfxclient.core.emote.Poses.leftArm;
import static com.vfxclient.core.emote.Poses.leftLeg;
import static com.vfxclient.core.emote.Poses.pulse;
import static com.vfxclient.core.emote.Poses.rightArm;
import static com.vfxclient.core.emote.Poses.rightLeg;
import static com.vfxclient.core.emote.Poses.s;

import java.util.Arrays;
import java.util.List;

import net.minecraft.util.math.MathHelper;

/** Página 1: os emotes de sempre (acenar, palmas, dançar...). */
final class GreetingEmotes {

	private GreetingEmotes() {
	}

	static List<Emote> all() {
		return Arrays.asList(
				new Emote("acenar", "Acenar", 2.5F, (m, t, b) -> {
					rightArm(m, 0.55F + 0.35F * s(t, 12), 1F, 0F, b);
					head(m, -5, 0, 6 * s(t, 6), b);
				}),
				new Emote("palmas", "Palmas", 2.5F, (m, t, b) -> {
					float apart = 0.12F + 0.3F * pulse(t, 16);
					arms(m, -apart, 0.1F, 1F, b);
				}),
				new Emote("dancar", "Dançar", 0F, (m, t, b) -> {
					float beat = s(t, 8);
					rightArm(m, 0.6F, 0.9F * beat, 0.3F, b);
					leftArm(m, 0.6F, -0.9F * beat, 0.3F, b);
					rightLeg(m, 0.08F, -1F, 0.45F * beat, b);
					leftLeg(m, 0.08F, -1F, -0.45F * beat, b);
					head(m, 8 * s(t, 16), 0, 12 * beat, b);
				}),
				new Emote("comemorar", "Comemorar", 3F, (m, t, b) -> {
					arms(m, 0.45F, 1F, 0.25F * s(t, 12), b);
					head(m, -20, 0, 0, b);
				}),
				new Emote("pensar", "Pensar", 3F, (m, t, b) -> {
					rightArm(m, -0.4F, 0.5F, 0.9F, b);
					leftArm(m, -0.3F, -0.2F, 0.9F, b);
					head(m, 12, 0, 10 + 4 * s(t, 2.5F), b);
				}),
				new Emote("facepalm", "Facepalm", 2.5F, (m, t, b) -> {
					rightArm(m, -0.35F, 0.8F, 0.8F, b);
					head(m, 28, 10 * s(t, 3), 0, b);
				}),
				new Emote("apontar", "Apontar", 2.5F, (m, t, b) -> {
					// O braço segue para onde o jogador está olhando.
					float pitch = m.head.posX;
					rightArm(m, 0F, -MathHelper.sin(pitch) + 0.04F * s(t, 5), MathHelper.cos(pitch), b);
				}),
				new Emote("continencia", "Continência", 2.5F, (m, t, b) -> {
					rightArm(m, -0.3F, 0.9F, 0.6F, b);
					head(m, -4, 0, 0, b);
				}));
	}
}
