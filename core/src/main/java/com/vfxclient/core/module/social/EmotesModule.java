package com.vfxclient.core.module.social;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.vfxclient.core.emote.Emote;
import com.vfxclient.core.emote.EmoteRelay;
import com.vfxclient.core.emote.EmoteState;
import com.vfxclient.core.emote.Emotes;
import com.vfxclient.core.gui.EmoteWheelScreen;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.KeySetting;
import com.vfxclient.core.module.setting.TextSetting;
import com.vfxclient.core.net.VfxServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.input.Keyboard;

/**
 * Emotes: a tecla (B) abre a roda, o personagem faz a animação e quem também usa o VfxClient
 * no mesmo servidor vê junto (pelo servidor de emotes, ver {@code relay/}).
 */
public class EmotesModule extends Module {

	private static final int CAMERA_FRONT = 0;
	private static final int CAMERA_BACK = 1;
	private static final int PERSPECTIVE_BACK = 1;
	private static final int PERSPECTIVE_FRONT = 2;

	private final KeySetting key = add(new KeySetting("key", "Tecla da roda de emotes", Keyboard.KEY_B));
	private final BooleanSetting showOthers = add(new BooleanSetting("showOthers", "Ver os emotes dos outros", true));
	private final BooleanSetting share = add(new BooleanSetting("share", "Mostrar os meus emotes para os outros", true));
	private final ChoiceSetting camera = add(new ChoiceSetting("camera", "Câmera durante o emote", CAMERA_FRONT,
			"De frente", "De costas", "Não mudar"));
	private final TextSetting relayUrl = addAdvanced(new TextSetting("relay", "Servidor de emotes (vazio = o do VfxClient)", "", 200));
	/** Atalhos feitos na roda, no formato {@code id=tecla,id=tecla} (códigos do LWJGL). */
	private final TextSetting shortcuts = addAdvanced(new TextSetting("shortcuts", "Atalhos dos emotes (feitos na roda)", "", 1000));

	private final EmoteRelay relay = new EmoteRelay();
	private boolean wheelPending;
	private Emote current;
	private int savedPerspective = -1;
	private int wheelPage;

	public EmotesModule() {
		super("emotes", "Emotes", "Roda de emotes; quem usa o VfxClient vê", Category.SOCIAL, true);
	}

	/** Tecla apertada no jogo (sem tela aberta). A roda abre no tick, depois do teclado. */
	public void onKeyPressed(int keyCode) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!isEnabled() || keyCode == KeySetting.NONE || client.currentScreen != null || client.world == null) {
			return;
		}
		if (keyCode == key.get()) {
			wheelPending = true;
			return;
		}

		Emote shortcut = shortcutFor(keyCode);
		if (shortcut != null) {
			play(shortcut);
		}
	}

	/** @return a página em que a roda parou da última vez (começa em 0) */
	public int wheelPage() {
		return wheelPage;
	}

	public void setWheelPage(int page) {
		wheelPage = page;
	}

	/** @return a tecla de atalho do emote, ou {@link KeySetting#NONE} */
	public int boundKey(Emote emote) {
		Integer code = parseShortcuts().get(emote.id());
		return code == null ? KeySetting.NONE : code;
	}

	/**
	 * Cria o atalho do emote; a tecla deixa de servir a qualquer outro emote.
	 *
	 * @param emote   emote
	 * @param keyCode tecla (código do LWJGL)
	 */
	public void bind(Emote emote, int keyCode) {
		Map<String, Integer> all = parseShortcuts();
		all.values().removeAll(Collections.singleton(keyCode));
		all.put(emote.id(), keyCode);
		saveShortcuts(all);
	}

	public void unbind(Emote emote) {
		Map<String, Integer> all = parseShortcuts();
		all.remove(emote.id());
		saveShortcuts(all);
	}

	private Emote shortcutFor(int keyCode) {
		for (Map.Entry<String, Integer> entry : parseShortcuts().entrySet()) {
			if (entry.getValue() == keyCode) {
				return Emotes.byId(entry.getKey());
			}
		}
		return null;
	}

	private Map<String, Integer> parseShortcuts() {
		Map<String, Integer> all = new LinkedHashMap<String, Integer>();
		// -Dvfx.shortcuts=id=tecla,...: só para os prints de teste (não mexe na configuração).
		for (String part : System.getProperty("vfx.shortcuts", shortcuts.get()).split(",")) {
			String[] pair = part.trim().split("=");
			if (pair.length != 2) {
				continue;
			}
			try {
				all.put(pair[0].trim(), Integer.parseInt(pair[1].trim()));
			} catch (NumberFormatException ignored) {
				// Entrada estragada na mão: ignora só ela.
			}
		}
		return all;
	}

	private void saveShortcuts(Map<String, Integer> all) {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<String, Integer> entry : all.entrySet()) {
			if (text.length() > 0) {
				text.append(',');
			}
			text.append(entry.getKey()).append('=').append(entry.getValue());
		}
		shortcuts.set(text.toString());
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (wheelPending) {
			wheelPending = false;
			if (client.currentScreen == null) {
				client.setScreen(new EmoteWheelScreen(this, key.get()));
			}
		}

		relay.tick(client, relayUrl(), showOthers.get());
		EmoteState.tick(client.world);

		if (current != null && client.player != null) {
			UUID me = client.player.getUuid();
			if (!EmoteState.isPlaying(me)) {
				finish(false);
			} else if (isMoving(client.player)) {
				finish(true);
			}
		}
	}

	/**
	 * Começa um emote no seu personagem (e avisa os outros, se estiver ligado).
	 *
	 * @param emote emote escolhido na roda
	 */
	public void play(Emote emote) {
		play(emote, 0F);
	}

	/**
	 * Igual a {@link #play(Emote)}, começando no meio do movimento (usado nos testes).
	 *
	 * @param startSeconds quanto do emote já passou
	 */
	public void play(Emote emote, float startSeconds) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		EmoteState.play(client.player.getUuid(), client.player.getGameProfile().getName(), emote, startSeconds);
		current = emote;
		moveCamera(client);
		if (share.get()) {
			relay.sendEmote(emote);
		}
	}

	@Override
	protected void onDisable() {
		finish(true);
		relay.disconnect();
	}

	/** @param notify avisar os outros (parou antes do fim) */
	private void finish(boolean notify) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (current != null && client.player != null) {
			EmoteState.stop(client.player.getUuid());
			if (notify && share.get()) {
				relay.sendStop();
			}
		}
		current = null;
		restoreCamera(client);
	}

	private void moveCamera(MinecraftClient client) {
		int choice = camera.get();
		if (choice != CAMERA_FRONT && choice != CAMERA_BACK) {
			return;
		}
		if (savedPerspective < 0) {
			savedPerspective = client.options.perspective;
		}
		client.options.perspective = choice == CAMERA_FRONT ? PERSPECTIVE_FRONT : PERSPECTIVE_BACK;
	}

	private void restoreCamera(MinecraftClient client) {
		if (savedPerspective >= 0) {
			client.options.perspective = savedPerspective;
			savedPerspective = -1;
		}
	}

	/** Endereço do servidor de emotes; {@code -Dvfx.emoteRelay} troca (testes com o servidor local). */
	private String relayUrl() {
		String custom = System.getProperty("vfx.emoteRelay", relayUrl.get()).trim();
		return custom.isEmpty() ? VfxServices.emotesUrl() : custom;
	}

	/** Andar, pular, agachar ou bater cancelam o emote. */
	private static boolean isMoving(ClientPlayerEntity player) {
		Input input = player.input;
		boolean walking = input != null && (input.movementForward != 0 || input.movementSideways != 0 || input.jumping || input.sneaking);
		return walking || player.handSwinging;
	}
}
