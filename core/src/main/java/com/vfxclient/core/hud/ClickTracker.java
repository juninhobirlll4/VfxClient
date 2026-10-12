package com.vfxclient.core.hud;

import java.util.ArrayDeque;
import java.util.Deque;

/** Conta cliques do mouse no último segundo (CPS). Usado pela HUD de CPS e pelas teclas. */
public final class ClickTracker {

	/** Código do Minecraft para o botão esquerdo do mouse. */
	public static final int LEFT = -100;
	/** Código do Minecraft para o botão direito do mouse. */
	public static final int RIGHT = -99;

	private static final long WINDOW_MS = 1000L;
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
		return count(LEFT_CLICKS);
	}

	/** @return cliques do botão direito no último segundo */
	public static int rightCps() {
		return count(RIGHT_CLICKS);
	}

	private static int count(Deque<Long> clicks) {
		long limit = System.currentTimeMillis() - WINDOW_MS;
		while (!clicks.isEmpty() && clicks.peekFirst() < limit) {
			clicks.pollFirst();
		}
		return clicks.size();
	}
}
