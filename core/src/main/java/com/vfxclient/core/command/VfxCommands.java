package com.vfxclient.core.command;

import java.util.Locale;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.ClickGuiScreen;
import com.vfxclient.core.gui.HudEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;

/**
 * Comandos locais do client, digitados no chat. Nunca são enviados ao servidor.
 *
 * <ul>
 *   <li>{@code /vfx} – ajuda</li>
 *   <li>{@code /vfx menu} – abre o menu</li>
 *   <li>{@code /vfx hud} – abre o editor de HUD</li>
 *   <li>{@code /vfx reset} – volta todas as opções ao padrão</li>
 * </ul>
 */
public final class VfxCommands {

	private static final String PREFIX = "§5[§dVfx§5] §f";

	/** Abertura de tela pendente: o chat ainda está fechando quando o comando chega. */
	private static Runnable pending;

	private VfxCommands() {
	}

	/**
	 * Trata uma mensagem do chat.
	 *
	 * @param message texto digitado
	 * @return {@code true} se era um comando do VfxClient (não deve ir para o servidor)
	 */
	public static boolean handle(String message) {
		String trimmed = message.trim();
		String lower = trimmed.toLowerCase(Locale.ROOT);
		if (!lower.equals("/vfx") && !lower.startsWith("/vfx ")) {
			return false;
		}

		String arg = trimmed.length() > 4 ? trimmed.substring(4).trim().toLowerCase(Locale.ROOT) : "";
		VfxCore core = VfxCore.get();
		if (core == null) {
			return false;
		}

		if (arg.equals("reset")) {
			core.getConfig().resetAll();
			say("Todas as opções voltaram ao padrão.");
		} else if (arg.equals("menu")) {
			pending = new Runnable() {
				@Override
				public void run() {
					MinecraftClient.getInstance().setScreen(new ClickGuiScreen());
				}
			};
		} else if (arg.equals("hud")) {
			pending = new Runnable() {
				@Override
				public void run() {
					MinecraftClient.getInstance().setScreen(new HudEditorScreen(null));
				}
			};
		} else {
			say("Comandos do VfxClient " + core.getVersion() + ":");
			say("§d/vfx menu§f - abre o menu");
			say("§d/vfx hud§f - edita a posição da HUD");
			say("§d/vfx reset§f - volta tudo ao padrão");
		}

		return true;
	}

	/**
	 * Executa uma tela pedida por comando, depois que o chat já fechou.
	 *
	 * @param client instância do Minecraft
	 */
	public static void tick(MinecraftClient client) {
		if (pending != null && client.currentScreen == null) {
			Runnable task = pending;
			pending = null;
			task.run();
		}
	}

	/**
	 * Escreve uma mensagem do VfxClient no chat (só para você).
	 *
	 * @param text texto (aceita códigos de cor com §)
	 */
	public static void say(String text) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(new LiteralText(PREFIX + text));
		}
	}
}
