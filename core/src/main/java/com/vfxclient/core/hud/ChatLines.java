package com.vfxclient.core.hud;

/** Implementado pelo chat do jogo (via mixin): devolve o texto da mensagem sob o mouse. */
public interface ChatLines {

	/**
	 * @param rawX X do mouse em pixels da janela (como {@code Mouse.getX()})
	 * @param rawY Y do mouse em pixels da janela, de baixo para cima (como {@code Mouse.getY()})
	 * @return a mensagem inteira sob o mouse, sem cores, ou {@code null}
	 */
	String vfx$messageAt(int rawX, int rawY);
}