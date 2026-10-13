package com.vfxclient.core.module.util;

import java.util.Locale;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Auto GG: manda uma mensagem (ex.: "gg") quando o chat mostra o fim de uma partida.
 *
 * <p>Só funciona em servidores (nunca no mundo local), manda no máximo uma vez a cada 15 s
 * e as frases que indicam o fim da partida são configuráveis (separadas por vírgula).</p>
 */
public class AutoGgModule extends Module {

	private static final long COOLDOWN_MS = 15000L;

	private final TextSetting message = add(new TextSetting("message", "Mensagem", "gg", 64));
	private final TextSetting triggers = add(new TextSetting("triggers", "Frases de fim de partida (vírgula)",
			"Winner:,1st Killer,Vencedor,Você venceu,VICTORY,VITÓRIA,Fim de jogo", 200));
	private final NumberSetting delay = add(new NumberSetting("delay", "Espera antes de mandar", 1, 0, 5, 0.5, " s"));

	private long sendAt = -1;
	private long lastSent;

	public AutoGgModule() {
		super("autoGg", "Auto GG", "Manda \"gg\" no fim da partida", Category.UTILITY, false);
	}

	/**
	 * Chamado para cada mensagem nova do chat.
	 *
	 * @param text mensagem recebida
	 */
	public void onChat(Text text) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!isEnabled() || client.isInSingleplayer() || sendAt >= 0 || System.currentTimeMillis() - lastSent < COOLDOWN_MS) {
			return;
		}
		String plain = text.asUnformattedString().replaceAll("§.", "").toLowerCase(Locale.ROOT);
		for (String trigger : triggers.get().split(",")) {
			String t = trigger.trim().toLowerCase(Locale.ROOT);
			if (!t.isEmpty() && plain.contains(t)) {
				sendAt = System.currentTimeMillis() + (long) (delay.get() * 1000);
				return;
			}
		}
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (sendAt < 0 || System.currentTimeMillis() < sendAt) {
			return;
		}
		sendAt = -1;
		String text = message.get().trim();
		if (client.player != null && !text.isEmpty()) {
			client.player.sendChatMessage(text);
			lastSent = System.currentTimeMillis();
		}
	}
}
