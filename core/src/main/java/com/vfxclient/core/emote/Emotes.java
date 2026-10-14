package com.vfxclient.core.emote;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.render.model.ModelPart;
import net.minecraft.util.math.MathHelper;

/**
 * Os emotes do VfxClient. Animações nossas, feitas com os ângulos do modelo do jogador.
 *
 * <p>Ângulos em graus nos parâmetros (mais fácil de ler). Lembretes do modelo do 1.8.9:
 * braço com inclinação {@code -180} aponta para cima e {@code -90} para a frente. No giro
 * para os lados (rolagem), positivo afasta o braço direito do corpo quando ele está abaixo do
 * ombro (acima, inverte: veja {@link #outward}); no giro em volta do eixo
 * vertical, negativo leva o braço direito para dentro (uns 20 graus já juntam as mãos). O braço esquerdo é o contrário nos dois.</p>
 */
public final class Emotes {

	private static final float DEGREES = (float) (180.0 / Math.PI);

	public static final Emote WAVE = new Emote("acenar", "Acenar", 2.5F, (model, t, blend) -> {
		pose(model.rightArm, -170, 0, -(30 + 20 * MathHelper.sin(t * 12F)), blend);
		pose(model.head, -5, 0, 6 * MathHelper.sin(t * 6F), blend);
	});

	public static final Emote CLAP = new Emote("palmas", "Palmas", 2.5F, (model, t, blend) -> {
		float together = (MathHelper.sin(t * 16F) + 1F) / 2F;
		float inward = 6 + 14 * together;
		pose(model.rightArm, -75, -inward, 0, blend);
		pose(model.leftArm, -75, inward, 0, blend);
	});

	public static final Emote DANCE = new Emote("dancar", "Dançar", 0F, (model, t, blend) -> {
		float beat = MathHelper.sin(t * 8F);
		float rightPitch = -90 + 70 * beat;
		float leftPitch = -90 - 70 * beat;
		pose(model.rightArm, rightPitch, 0, outward(rightPitch, 25), blend);
		pose(model.leftArm, leftPitch, 0, -outward(leftPitch, 25), blend);
		pose(model.rightLeg, 25 * beat, 0, 4, blend);
		pose(model.leftLeg, -25 * beat, 0, -4, blend);
		pose(model.head, 8 * MathHelper.sin(t * 16F), 0, 12 * beat, blend);
	});

	public static final Emote CHEER = new Emote("comemorar", "Comemorar", 3F, (model, t, blend) -> {
		float pump = 18 * MathHelper.sin(t * 12F);
		pose(model.rightArm, -165 + pump, 0, -28, blend);
		pose(model.leftArm, -165 + pump, 0, 28, blend);
		pose(model.head, -20, 0, 0, blend);
	});

	public static final Emote THINK = new Emote("pensar", "Pensar", 3F, (model, t, blend) -> {
		pose(model.rightArm, -115, -25, 0, blend);
		pose(model.leftArm, -45, 30, 0, blend);
		pose(model.head, 12, 0, 10 + 4 * MathHelper.sin(t * 2.5F), blend);
	});

	public static final Emote FACEPALM = new Emote("facepalm", "Facepalm", 2.5F, (model, t, blend) -> {
		pose(model.rightArm, -135, -28, 0, blend);
		pose(model.head, 28, 10 * MathHelper.sin(t * 3F), 0, blend);
	});

	public static final Emote POINT = new Emote("apontar", "Apontar", 2.5F, (model, t, blend) -> {
		// O braço segue para onde o jogador está olhando.
		float pitch = model.head.posX * DEGREES;
		float yaw = model.head.posY * DEGREES;
		pose(model.rightArm, pitch - 90 + 3 * MathHelper.sin(t * 5F), yaw, 0, blend);
	});

	public static final Emote SALUTE = new Emote("continencia", "Continência", 2.5F, (model, t, blend) -> {
		pose(model.rightArm, -125, -40, 15, blend);
		pose(model.head, -4, 0, 0, blend);
	});

	/** Na ordem da roda, começando em cima e indo no sentido do relógio. */
	public static final List<Emote> ALL = Collections.unmodifiableList(Arrays.asList(
			WAVE, CLAP, DANCE, CHEER, THINK, FACEPALM, POINT, SALUTE));

	private Emotes() {
	}

	/** @return o emote com esse id, ou {@code null} se não existir (versão mais nova de outro jogador) */
	public static Emote byId(String id) {
		for (Emote emote : ALL) {
			if (emote.id().equals(id)) {
				return emote;
			}
		}
		return null;
	}

	/**
	 * Inclinação para fora do corpo (braço direito; no esquerdo, use o negativo). Com o braço
	 * acima da linha do ombro, a mesma rolagem vira para dentro, então o sinal troca.
	 */
	private static float outward(float pitch, float degrees) {
		return pitch < -90 ? -degrees : degrees;
	}

	/** Leva a parte do modelo da pose atual até os ângulos do emote, conforme o blend. */
	private static void pose(ModelPart part, float pitch, float yaw, float roll, float blend) {
		part.posX = lerp(part.posX, pitch / DEGREES, blend);
		part.posY = lerp(part.posY, yaw / DEGREES, blend);
		part.posZ = lerp(part.posZ, roll / DEGREES, blend);
	}

	private static float lerp(float from, float to, float amount) {
		return from + (to - from) * amount;
	}
}
