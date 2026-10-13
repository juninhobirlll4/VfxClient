package com.vfxclient.core.hud;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.HudEditorScreen;
import com.vfxclient.core.module.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/** Desenha os módulos de HUD ligados por cima da HUD do jogo. */
public final class HudRenderer {

	private HudRenderer() {
	}

	/**
	 * Chamado pelo mixin no fim de {@code InGameHud.render}.
	 *
	 * @param client instância do Minecraft
	 */
	public static void render(MinecraftClient client) {
		VfxCore core = VfxCore.get();
		if (core == null || client.options.debugEnabled || client.currentScreen instanceof HudEditorScreen) {
			return;
		}
		Window window = new Window(client);
		for (HudModule module : core.getModules().hudModules()) {
			if (!module.isEnabled() || !module.isVisible(client, false)) {
				continue;
			}
			int w = module.getWidth(client, false);
			int h = module.getHeight(client, false);
			int x = module.screenX(window.getWidth(), w);
			int y = module.screenY(window.getHeight(), h);
			module.render(client, x, y, false);
		}
	}

	/**
	 * Mantém o elemento dentro da tela, mesmo se a janela ou a escala mudarem.
	 *
	 * @param value posição salva
	 * @param max   posição máxima
	 * @return posição ajustada
	 */
	public static int clamp(int value, int max) {
		return Math.max(0, Math.min(value, Math.max(0, max)));
	}
}
