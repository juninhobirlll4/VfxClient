package com.vfxclient.core.module.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.TextSetting;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/**
 * Melhorias do chat: horário em cada mensagem, mensagens repetidas agrupadas em
 * uma linha com contador ("(x3)"), histórico maior (o jogo guarda só 100 linhas), filtro
 * anti-spam e Ctrl + clique para copiar uma mensagem.
 */
public class ChatModule extends Module {

	/** Faixa de ids usada para as mensagens deste módulo (o jogo usa 0 para mensagens comuns). */
	private static final int ID_BASE = 0x56460000;
	private static final int ID_RANGE = 0x8000;

	private final BooleanSetting timestamps = add(new BooleanSetting("timestamps", "Horário nas mensagens", true));
	private final BooleanSetting stack = add(new BooleanSetting("stack", "Agrupar mensagens repetidas", true));
	private final BooleanSetting antiSpam = add(new BooleanSetting("antiSpam", "Anti-spam: esconder a mesma mensagem repetida em 3 s", true));
	private final TextSetting blocked = add(new TextSetting("blocked", "Esconder mensagens com (separe por vírgula)", "", 200));
	private final BooleanSetting copyClick = add(new BooleanSetting("copyClick", "Ctrl + clique copia a mensagem", true));
	private final NumberSetting history = add(new NumberSetting("history", "Linhas guardadas no histórico", 500, 100, 1000, 50, ""));

	private String lastRaw;
	private long lastRawAt;
	private int lastId;
	private int repeat;
	private int nextId;

	public ChatModule() {
		super("chat", "Chat", "Horário e mensagens agrupadas", Category.UTILITY, true);
	}

	/** @return quantas linhas o chat guarda (100 com o módulo desligado, como no jogo) */
	public int historySize() {
		return isEnabled() ? history.getInt() : 100;
	}

	/**
	 * Decide como uma mensagem nova deve entrar no chat.
	 *
	 * @param message mensagem original do jogo/servidor
	 * @return a mensagem pronta e o id a usar (id igual ao anterior = substitui a linha anterior)
	 */
	public Result process(Text message) {
		String raw = message.asFormattedString();
		if (isBlocked(message)) {
			return null;
		}

		long now = System.currentTimeMillis();
		if (antiSpam.get() && raw.equals(lastRaw) && now - lastRawAt < 3000L) {
			lastRawAt = now;
			return null;
		}

		lastRawAt = now;
		int id;
		if (stack.get() && raw.equals(lastRaw)) {
			repeat++;
			id = lastId;
		} else {
			repeat = 1;
			nextId = (nextId + 1) % ID_RANGE;
			id = ID_BASE + nextId;
			lastRaw = raw;
			lastId = id;
		}

		Text result = timestamps.get()
				? new LiteralText("§7[" + new SimpleDateFormat("HH:mm").format(new Date()) + "] §r")
				: new LiteralText("");
		result.append(message.copy());
		if (repeat > 1) {
			result.append(new LiteralText(" §7(x" + repeat + ")"));
		}

		return new Result(result, id);
	}

	private boolean isBlocked(Text message) {
		String words = blocked.get().trim();
		if (words.isEmpty()) {
			return false;
		}
		String plain = message.asUnformattedString().replaceAll("§.", "").toLowerCase();
		for (String word : words.split(",")) {
			String w = word.trim().toLowerCase();
			if (!w.isEmpty() && plain.contains(w)) {
				return true;
			}
		}
		return false;
	}

	/** @return se Ctrl + clique copia mensagens */
	public boolean copyOnClick() {
		return isEnabled() && copyClick.get();
	}

	/** Mensagem processada e o id da linha no chat. */
	public static final class Result {
		/** Texto final. */
		public final Text text;
		/** Id da linha (diferente de 0). */
		public final int id;

		private Result(Text text, int id) {
			this.text = text;
			this.id = id;
		}
	}
}
