package com.vfxclient.core.module.general;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;

/**
 * Telas do VfxClient no lugar das do jogo: tela inicial, menu de pausa, servidores e o visual
 * dos botões das opções. Desligado, volta tudo ao original.
 */
public class TitleScreenModule extends Module {

	public TitleScreenModule() {
		super("titleScreen", "Telas do VfxClient", "Tela inicial, pausa, servidores e opções", Category.GENERAL, true);
	}
}