package com.vfxclient.core.gui;

/** Retângulo clicável desenhado numa tela, com a ação que ele dispara. */
public final class ClickArea {

	private final int action;
	private final float x;
	private final float y;
	private final float width;
	private final float height;

	/**
	 * @param action número da ação (cada tela define as suas)
	 * @param x      esquerda
	 * @param y      topo
	 * @param width  largura
	 * @param height altura
	 */
	public ClickArea(int action, float x, float y, float width, float height) {
		this.action = action;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	/** @return ação desta área */
	public int action() {
		return action;
	}

	/** @return se o ponto está dentro da área */
	public boolean contains(float px, float py) {
		return Ui.inside(px, py, x, y, width, height);
	}
}