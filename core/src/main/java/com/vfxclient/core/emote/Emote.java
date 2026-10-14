package com.vfxclient.core.emote;

import net.minecraft.client.render.entity.model.PlayerEntityModel;

/**
 * Um emote: nome, duração e a animação.
 *
 * <p>Os ids são o que vai pela rede (só letras minúsculas e _), então não mude um id que já
 * foi publicado: quem estiver com a versão antiga deixaria de ver o emote.</p>
 */
public final class Emote {

	/** Movimento do emote, aplicado em cima da pose normal do jogador. */
	public interface Animation {

		/**
		 * @param model   modelo do jogador (ângulos em radianos)
		 * @param seconds tempo desde o começo do emote
		 * @param blend   0 = pose normal, 1 = pose do emote (entrada e saída suaves)
		 */
		void apply(PlayerEntityModel model, float seconds, float blend);
	}

	/** Emotes que ficam repetindo param sozinhos depois deste tempo. */
	static final float LOOP_LIMIT = 12F;

	private final String id;
	private final String name;
	private final float duration;
	private final Animation animation;

	/**
	 * @param id        id enviado pela rede
	 * @param name      nome na roda de emotes
	 * @param duration  duração em segundos ({@code 0} = repete até o jogador se mexer)
	 * @param animation movimento
	 */
	Emote(String id, String name, float duration, Animation animation) {
		this.id = id;
		this.name = name;
		this.duration = duration;
		this.animation = animation;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	/** @return se repete até o jogador se mexer */
	public boolean loops() {
		return duration <= 0;
	}

	/** @return quanto tempo o emote fica na tela, em segundos */
	public float length() {
		return loops() ? LOOP_LIMIT : duration;
	}

	void apply(PlayerEntityModel model, float seconds, float blend) {
		animation.apply(model, seconds, blend);
	}
}
