package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.KeySetting;
import com.vfxclient.core.module.setting.TextSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Texto automático: cada atalho tem uma tecla e uma mensagem (ou comando, começando com "/").
 * Apertou a tecla durante o jogo, a mensagem vai para o chat.
 *
 * <p>Para não tomar kick por spam, cada atalho só dispara de novo depois de 1,5 s.</p>
 */
public class AutoTextModule extends Module {

	private static final int SLOTS = 6;
	private static final long COOLDOWN_MS = 1500L;

	private final KeySetting[] keys = new KeySetting[SLOTS];
	private final TextSetting[] texts = new TextSetting[SLOTS];
	private final long[] lastSent = new long[SLOTS];

	public AutoTextModule() {
		super("autoText", "Texto automático", "Uma tecla manda uma mensagem no chat", Category.UTILITY, false);
		String[] defaults = {"gg", "/l", "", "", "", ""};
		for (int i = 0; i < SLOTS; i++) {
			keys[i] = add(new KeySetting("key" + (i + 1), "Atalho " + (i + 1) + ": tecla", KeySetting.NONE));
			texts[i] = add(new TextSetting("text" + (i + 1), "Atalho " + (i + 1) + ": mensagem", defaults[i], 100));
		}
	}

	/**
	 * Chamado quando uma tecla é apertada sem nenhuma tela aberta.
	 *
	 * @param keyCode código da tecla (ou do botão do mouse)
	 */
	public void onKeyPressed(int keyCode) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!isEnabled() || keyCode == KeySetting.NONE || client.player == null || client.currentScreen != null) {
			return;
		}
		long now = System.currentTimeMillis();
		for (int i = 0; i < SLOTS; i++) {
			String text = texts[i].get().trim();
			if (keys[i].get() == keyCode && !text.isEmpty() && now - lastSent[i] >= COOLDOWN_MS) {
				lastSent[i] = now;
				client.player.sendChatMessage(text);
			}
		}
	}
}
