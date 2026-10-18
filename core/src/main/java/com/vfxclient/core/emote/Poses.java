package com.vfxclient.core.emote;

import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.util.math.MathHelper;

/**
 * Peças para montar os emotes. Em vez de ângulos soltos, braços e pernas são apontados numa
 * direção: {@code out} para fora do corpo, {@code up} para cima e {@code forward} para a frente
 * (valores de -1 a 1; só a direção importa, não o tamanho).
 *
 * <p>O modelo do 1.8.9 não tem cotovelo nem joelho, então todo membro é reto.</p>
 */
final class Poses {

	private static final float DEGREES = (float) (180.0 / Math.PI);
	private static final float TINY = 1e-4F;

	private Poses() {
	}

	/** @return seno de {@code t * speed} (um balanço que vai de -1 a 1) */
	static float s(float t, float speed) {
		return MathHelper.sin(t * speed);
	}

	/** @return o seno acima, mas indo de 0 a 1 */
	static float pulse(float t, float speed) {
		return (MathHelper.sin(t * speed) + 1F) / 2F;
	}

	/** @return 0 ou 1, trocando {@code perSecond} vezes por segundo (movimentos "travados", como robô) */
	static float step(float t, float perSecond) {
		return ((int) Math.floor(t * perSecond) & 1) == 0 ? 0F : 1F;
	}

	/** @return {@code a} e {@code b} misturados; {@code amount} de 0 a 1 */
	static float mix(float a, float b, float amount) {
		return a + (b - a) * amount;
	}

	static void rightArm(PlayerEntityModel m, float out, float up, float forward, float blend) {
		aim(m.rightArm, true, out, up, forward, blend);
	}

	static void leftArm(PlayerEntityModel m, float out, float up, float forward, float blend) {
		aim(m.leftArm, false, out, up, forward, blend);
	}

	static void rightLeg(PlayerEntityModel m, float out, float up, float forward, float blend) {
		aim(m.rightLeg, true, out, up, forward, blend);
	}

	static void leftLeg(PlayerEntityModel m, float out, float up, float forward, float blend) {
		aim(m.leftLeg, false, out, up, forward, blend);
	}

	/** Os dois braços na mesma direção (espelhados). */
	static void arms(PlayerEntityModel m, float out, float up, float forward, float blend) {
		rightArm(m, out, up, forward, blend);
		leftArm(m, out, up, forward, blend);
	}

	/** Cabeça, em graus: {@code pitch} positivo olha para baixo, {@code yaw} gira, {@code roll} inclina. */
	static void head(PlayerEntityModel m, float pitch, float yaw, float roll, float blend) {
		angles(m.head, pitch, yaw, roll, blend);
	}

	/**
	 * Aponta um braço ou perna (que no modelo pendem para baixo) na direção pedida.
	 *
	 * @param rightSide {@code true} para o lado direito do jogador (define o que é "para fora")
	 * @param blend     0 = pose atual, 1 = a direção pedida
	 */
	static void aim(ModelPart part, boolean rightSide, float out, float up, float forward, float blend) {
		// No modelo, o eixo x cresce para a esquerda do jogador, y para baixo e z para trás.
		float dx = rightSide ? -out : out;
		float dy = -up;
		float dz = -forward;

		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < TINY) {
			dx = 0;
			dy = 1;
			dz = 0;
			length = 1;
		}
		dx /= length;
		dy /= length;
		dz /= length;

		// A parte gira primeiro no eixo x (inclinação) e depois no z (para os lados).
		float pitch = (float) Math.asin(Math.max(-1F, Math.min(1F, dz)));
		float cosPitch = (float) Math.sqrt(Math.max(0F, 1F - dz * dz));
		float roll = cosPitch < TINY ? 0F : (float) Math.atan2(Math.abs(dx) < TINY ? 0F : -dx, dy);

		part.posX = mix(part.posX, pitch, blend);
		part.posY = mix(part.posY, 0F, blend);
		part.posZ = mix(part.posZ, roll, blend);
	}

	/** Ângulos diretos, em graus. */
	static void angles(ModelPart part, float pitch, float yaw, float roll, float blend) {
		part.posX = mix(part.posX, pitch / DEGREES, blend);
		part.posY = mix(part.posY, yaw / DEGREES, blend);
		part.posZ = mix(part.posZ, roll / DEGREES, blend);
	}
}
