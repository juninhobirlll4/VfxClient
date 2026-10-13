package com.vfxclient.core.hud;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Conta cliques do mouse (CPS). Guarda os últimos 5 segundos para dar o valor do último
 * segundo e a média de 5 segundos. Usado pela HUD de CPS e pelas teclas.
 */
public final class ClickTracker {

	/** Código do Minecraft para o botão esquerdo do mouse. */
	public static final int LEFT = -100;
	/** Código do Minecraft para o botão direito do mouse. */
	public static final int RIGHT = -99;

	private static final long KEEP_MS = 5000L;
	private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<Long>();
	private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<Long>();

	private ClickTracker() {
	}

	/**
	 * Registra um clique.
	 *
	 * @param code código da tecla/botão vindo do Minecraft
	 */
	public static void onPress(int code) {
		if (code == LEFT) {
			LEFT_CLICKS.addLast(System.currentTimeMillis());
		} else if (code == RIGHT) {
			RIGHT_CLICKS.addLast(System.currentTimeMillis());
		}
	}

	/** @return cliques do botão esquerdo no último segundo */
	public static int leftCps() {
		return count(LEFT_CLICKS, 1000L);
	}

	/** @return cliques do botão direito no último segundo */
	public static int rightCps() {
		return count(RIGHT_CLICKS, 1000L);
	}

	/**
	 * @param left {@code true} para o botão esquerdo
	 * @return média de cliques por segundo nos últimos 5 segundos
	 */
	public static double average5s(boolean left) {
		return count(left ? LEFT_CLICKS : RIGHT_CLICKS, KEEP_MS) / 5.0;
	}

	private static int count(Deque<Long> clicks, long windowMs) {
		long now = System.currentTimeMillis();
		while (!clicks.isEmpty() && clicks.peekFirst() < now - KEEP_MS) {
			clicks.pollFirst();
		}
		if (windowMs >= KEEP_MS) {
			return clicks.size();
		}
		int total = 0;
		long limit = now - windowMs;
		Iterator<Long> it = clicks.descendingIterator();
		while (it.hasNext() && it.next() >= limit) {
			total++;
		}
		return total;
	}
}
