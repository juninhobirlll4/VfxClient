package com.vfxclient.core.gui;

/**
 * Telas do VfxClient que desfocam o jogo atrás delas (menu de pausa, por exemplo). O desfoque
 * usa o efeito "blur" que já vem no Minecraft e entra aos poucos quando a tela abre.
 */
public interface BlurredScreen {

	/** @return de 0 (sem desfoque) a 1 (desfoque completo) */
	float blurProgress();
}