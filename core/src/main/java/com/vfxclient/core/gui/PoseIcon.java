package com.vfxclient.core.gui;

import com.vfxclient.core.emote.Emote;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.ModelPart;

/**
 * Bonequinho que mostra a pose de um emote (o ícone da roda). A pose vem da própria animação do
 * emote, aplicada num modelo de jogador sem desenhá-lo; depois os braços, as pernas e a cabeça
 * são projetados em 2D, vistos um pouco de lado.
 */
final class PoseIcon {

	/** Giro do boneco para o lado, em radianos (um terço de vista, como nos ícones do Lunar). */
	private static final double TURN = Math.toRadians(35);
	private static final float COS_TURN = (float) Math.cos(TURN);
	private static final float SIN_TURN = (float) Math.sin(TURN);

	// Medidas do modelo do jogador, em "pixels" do modelo (y para baixo).
	private static final float SHOULDER_X = 5F;
	private static final float SHOULDER_Y = 2F;
	private static final float HIP_X = 2F;
	private static final float HIP_Y = 12F;
	private static final float LIMB = 12F;
	private static final float LIMB_WIDTH = 4F;
	private static final float BODY_HALF_WIDTH = 4F;
	private static final float HEAD = 8F;

	private static PlayerEntityModel model;

	private PoseIcon() {
	}

	/**
	 * Desenha o boneco na pose do emote.
	 *
	 * @param emote      emote
	 * @param centerX    centro (x)
	 * @param centerY    centro (y)
	 * @param size       altura do boneco, em unidades de desenho
	 * @param brightness 0 a 1: do cinza discreto ao branco
	 */
	static void draw(Emote emote, float centerX, float centerY, float size, float brightness) {
		PlayerEntityModel pose = poseOf(emote);
		float scale = size / 32F;
		float originX = centerX;
		float originY = centerY - size / 2F;

		int dark = shade(0xFF6E6E7A, brightness);
		int mid = shade(0xFF9C9CA8, brightness);
		int light = shade(0xFFCFCFD8, brightness);

		// Atrás para frente: perna e braço do lado de longe, corpo, e os do lado de perto.
		limb(pose.leftLeg, HIP_X, HIP_Y, originX, originY, scale, dark);
		limb(pose.rightLeg, -HIP_X, HIP_Y, originX, originY, scale, mid);
		limb(pose.leftArm, SHOULDER_X, SHOULDER_Y, originX, originY, scale, mid);
		body(originX, originY, scale, dark);
		limb(pose.rightArm, -SHOULDER_X, SHOULDER_Y, originX, originY, scale, light);
		head(pose.head, originX, originY, scale, light);
	}

	/** Modelo na pose do emote (começa do zero para não herdar nada do emote anterior). */
	private static PlayerEntityModel poseOf(Emote emote) {
		if (model == null) {
			model = new PlayerEntityModel(0F, false);
		}
		for (ModelPart part : new ModelPart[] {model.head, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg}) {
			part.posX = 0F;
			part.posY = 0F;
			part.posZ = 0F;
		}
		emote.apply(model, emote.previewSeconds(), 1F);
		return model;
	}

	/** Braço ou perna: sai da articulação e aponta para onde a pose manda. */
	private static void limb(ModelPart part, float jointX, float jointY, float originX, float originY, float scale, int color) {
		// O vetor "para baixo" gira como o jogo gira (primeiro x, depois y, depois z).
		float x = 0F;
		float y = 1F;
		float z = 0F;

		float cos = (float) Math.cos(part.posX);
		float sin = (float) Math.sin(part.posX);
		float ny = y * cos - z * sin;
		z = y * sin + z * cos;
		y = ny;

		cos = (float) Math.cos(part.posY);
		sin = (float) Math.sin(part.posY);
		float nx = x * cos + z * sin;
		z = -x * sin + z * cos;
		x = nx;

		cos = (float) Math.cos(part.posZ);
		sin = (float) Math.sin(part.posZ);
		nx = x * cos - y * sin;
		y = x * sin + y * cos;
		x = nx;

		float startX = originX + (jointX * COS_TURN) * scale;
		float startY = originY + (jointY + 8F) * scale;
		float endX = startX + (x * COS_TURN + z * SIN_TURN) * LIMB * scale;
		float endY = startY + y * LIMB * scale;
		Render2D.line(startX, startY, endX, endY, LIMB_WIDTH * scale, color);
	}

	private static void body(float originX, float originY, float scale, int color) {
		float half = BODY_HALF_WIDTH * COS_TURN * scale;
		float top = originY + 8F * scale;
		Render2D.quad(originX - half, top, originX + half, top, originX + half, top + 12F * scale, originX - half, top + 12F * scale, color);
	}

	/** Cabeça: um quadrado inclinado para o lado como a cabeça do emote. */
	private static void head(ModelPart part, float originX, float originY, float scale, int color) {
		float roll = part.posZ;
		float half = HEAD / 2F * scale;
		float cx = originX + (float) Math.sin(part.posY) * 2F * scale;
		float cy = originY + half;

		float cos = (float) Math.cos(roll);
		float sin = (float) Math.sin(roll);
		Render2D.quad(
				cx + (-half * cos + half * sin), cy + (-half * sin - half * cos),
				cx + (half * cos + half * sin), cy + (half * sin - half * cos),
				cx + (half * cos - half * sin), cy + (half * sin + half * cos),
				cx + (-half * cos - half * sin), cy + (-half * sin + half * cos),
				color);
	}

	/** Mistura a cor com branco conforme o brilho (0 = a cor, 1 = branco). */
	private static int shade(int argb, float brightness) {
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		r += (int) ((255 - r) * brightness);
		g += (int) ((255 - g) * brightness);
		b += (int) ((255 - b) * brightness);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}
}
