package com.vfxclient.core.module.social;

import java.util.UUID;

import com.vfxclient.core.emote.Emote;
import com.vfxclient.core.emote.EmoteRelay;
import com.vfxclient.core.emote.EmoteState;
import com.vfxclient.core.gui.EmoteWheelScreen;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.KeySetting;
import com.vfxclient.core.module.setting.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.input.Keyboard;

/**
 * Emotes: a tecla (B) abre a roda, o personagem faz a animação e quem também usa o VfxClient
 * no mesmo servidor vê junto (pelo servidor de emotes, ver {@code relay/}).
 */
public class EmotesModule extends Module {

	/** Servidor de emotes publicado na Cloudflare (relay/README.md). Vazio = só você vê. */
	public static final String DEFAULT_RELAY = "";

	private static final int CAMERA_FRONT = 0;
	private static final int CAMERA_BACK = 1;
	private static final int PERSPECTIVE_BACK = 1;
	private static final int PERSPECTIVE_FRONT = 2;

	private final KeySetting key = add(new KeySetting("key", "Tecla da roda de emotes", Keyboard.KEY_B));
	private final BooleanSetting showOthers = add(new BooleanSetting("showOthers", "Ver os emotes dos outros", true));
	private final BooleanSetting share = add(new BooleanSetting("share", "Mostrar os meus emotes para os outros", true));
	private final ChoiceSetting camera = add(new ChoiceSetting("camera", "Câmera durante o emote", CAMERA_FRONT,
			"De frente", "De costas", "Não mudar"));
	private final TextSetting relayUrl = addAdvanced(new TextSetting("relay", "Servidor de emotes", DEFAULT_RELAY, 200));

	private final EmoteRelay relay = new EmoteRelay();
	private boolean wheelPending;
	private Emote current;
	private int savedPerspective = -1;

	public EmotesModule() {
		super("emotes", "Emotes", "Roda de emotes; quem usa o VfxClient vê", Category.SOCIAL, true);
	}

	/** Tecla apertada no jogo (sem tela aberta). A roda abre no tick, depois do teclado. */
	public void onKeyPressed(int keyCode) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (isEnabled() && keyCode != KeySetting.NONE && keyCode == key.get() && client.currentScreen == null && client.world != null) {
			wheelPending = true;
		}
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
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		EmoteState.play(client.player.getUuid(), client.player.getGameProfile().getName(), emote);
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
		return System.getProperty("vfx.emoteRelay", relayUrl.get()).trim();
	}

	/** Andar, pular, agachar ou bater cancelam o emote. */
	private static boolean isMoving(ClientPlayerEntity player) {
		Input input = player.input;
		boolean walking = input != null && (input.movementForward != 0 || input.movementSideways != 0 || input.jumping || input.sneaking);
		return walking || player.handSwinging;
	}
}
