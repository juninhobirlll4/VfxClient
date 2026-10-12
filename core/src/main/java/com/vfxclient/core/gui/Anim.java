package com.vfxclient.core.gui;

/** Valor animado suavemente em direção a um alvo, independente do FPS. */
public final class Anim {

	private float value;
	private long lastUpdate = System.nanoTime();

	/** @param initial valor inicial */
	public Anim(float initial) {
		this.value = initial;
	}

	/**
	 * Aproxima o valor do alvo.
	 *
	 * @param target alvo
	 * @param speed  quanto maior, mais rápido (8–20 fica bom)
	 * @return o valor atualizado
	 */
	public float update(float target, float speed) {
		long now = System.nanoTime();
		float seconds = Math.min(0.1F, (now - lastUpdate) / 1_000_000_000F);
		lastUpdate = now;
		value += (target - value) * (1F - (float) Math.exp(-speed * seconds));
		if (Math.abs(target - value) < 0.001F) {
			value = target;
		}
		return value;
	}

	/** @return valor atual */
	public float get() {
		return value;
	}

	/**
	 * Curva "ease out cubic": rápida no começo e suave no fim.
	 *
	 * @param t de 0 a 1
	 * @return valor suavizado de 0 a 1
	 */
	public static float easeOutCubic(float t) {
		float clamped = Math.max(0F, Math.min(1F, t));
		float inv = 1F - clamped;
		return 1F - inv * inv * inv;
	}
}
