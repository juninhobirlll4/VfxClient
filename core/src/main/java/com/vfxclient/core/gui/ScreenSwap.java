package com.vfxclient.core.gui;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.mixin.MultiplayerScreenAccessor;
import com.vfxclient.core.module.general.TitleScreenModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;

/**
 * Decide quando uma tela do jogo vira a versão do VfxClient. Tudo pode ser desligado no
 * módulo "Telas do VfxClient" (Geral), que volta às telas originais.
 */
public final class ScreenSwap {

	private ScreenSwap() {
	}

	/**
	 * @param client    instância do Minecraft
	 * @param requested tela que o jogo quer abrir ({@code null} = nenhuma)
	 * @return a tela do VfxClient no lugar dela, ou {@code null} para manter a do jogo
	 */
	public static Screen replace(MinecraftClient client, Screen requested) {
		TitleScreenModule module = VfxCore.module(TitleScreenModule.class);
		if (module == null || !module.isEnabled()) {
			return null;
		}

		// Sem tela e sem mundo, o jogo abriria a tela inicial.
		if (requested == null) {
			return client.world == null ? new VfxTitleScreen() : null;
		}

		Class<?> type = requested.getClass();
		if (type == TitleScreen.class) {
			return new VfxTitleScreen();
		}
		if (type == GameMenuScreen.class) {
			return new VfxPauseScreen();
		}
		if (type == MultiplayerScreen.class) {
			return new VfxServerScreen(((MultiplayerScreenAccessor) requested).vfx$getParent());
		}
		return null;
	}
}