package com.vfxclient.core;

import com.vfxclient.core.emote.Emote;
import com.vfxclient.core.emote.Emotes;
import com.vfxclient.core.gui.ClickGuiScreen;
import com.vfxclient.core.gui.EmoteWheelScreen;
import com.vfxclient.core.gui.VfxTitleScreen;
import com.vfxclient.core.module.social.EmotesModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.ScreenshotUtils;

/**
 * Modos de teste para revisar o visual sem mexer no teclado e no mouse: abrem uma tela, o
 * próprio jogo salva um print em {@code screenshots/} e fecha.
 *
 * <ul>
 *   <li>{@code -Dvfx.titleShot=true}: tela inicial ({@code vfx-teste-inicio.png});</li>
 *   <li>{@code -Dvfx.titleShot=servidores|opcoes|video|controles|mundos|novomundo}: abre essa tela a partir da
 *       inicial ({@code vfx-teste-inicio.png});</li>
 *   <li>{@code -Dvfx.openMenu=pausa}: menu de pausa, já num mundo ({@code vfx-teste.png});</li>
 *   <li>{@code -Dvfx.openMenu=emotes}: roda de emotes ({@code vfx-teste.png});</li>
 *       com {@code -Dvfx.wheelPage=n} e {@code -Dvfx.wheelHover=n} a roda já abre nessa página, com esse emote apontado;</li>
 *   <li>{@code -Dvfx.openMenu=mundo}: o mundo sem nenhuma tela aberta ({@code vfx-teste.png});</li>
 *   <li>{@code -Dvfx.emote=<id>} junto com o openMenu: faz esse emote antes do print;</li>
 *   <li>{@code -Dvfx.emoteSheet=true}: um print de cada emote ({@code emote-<id>.png}), para revisar as animações;</li>
 *   <li>{@code -Dvfx.tab=true}: a lista de jogadores (Tab) fica aberta no print;</li>
 *   <li>{@code -Dvfx.remoteSelf=true}: você também recebe a skin do servidor do VfxClient (para testar).</li>
 *   <li>{@code -Dvfx.openMenu=<id do módulo>}: opções do módulo no menu ({@code vfx-teste.png}).</li>
 * </ul>
 */
final class TestShots {

	private static final int WAIT_TICKS = 60;
	private static final int SHOT_DELAY = 40;

	private final String titleMode = System.getProperty("vfx.titleShot");
	private String menuMode = System.getProperty("vfx.openMenu");
	private int titleTicks = WAIT_TICKS;
	private int menuTicks = WAIT_TICKS;
	private int shotTicks = -1;
	private String shotName;
	private int sheetStep = -1;
	private int sheetWait = WAIT_TICKS;

	/** Chamado a cada tick do cliente. */
	void tick(MinecraftClient client) {
		if (titleMode != null) {
			tickTitleMode(client);
		}
		if (menuMode != null) {
			tickMenuMode(client);
		}
		if (Boolean.getBoolean("vfx.emoteSheet")) {
			tickEmoteSheet(client);
		}
		if (Boolean.getBoolean("vfx.tab") && client.world != null) {
			KeyBinding.setKeyPressed(client.options.playerListKey.getCode(), true);
		}
		if (shotTicks > 0 && --shotTicks == 0) {
			saveShot(client, shotName);
			client.scheduleStop();
		}
	}

	private void tickTitleMode(MinecraftClient client) {
		if (shotTicks >= 0 || !(client.currentScreen instanceof VfxTitleScreen) || --titleTicks > 0) {
			return;
		}
		Screen target = titleTarget(client.currentScreen, client);
		if (target != null) {
			client.setScreen(target);
		}
		shoot("vfx-teste-inicio.png");
	}

	/** @return a tela a abrir a partir da inicial, ou {@code null} para fotografar a própria inicial */
	private Screen titleTarget(Screen title, MinecraftClient client) {
		if ("servidores".equals(titleMode)) {
			return new MultiplayerScreen(title);
		}
		if ("opcoes".equals(titleMode)) {
			return new SettingsScreen(title, client.options);
		}
		if ("video".equals(titleMode)) {
			return new VideoOptionsScreen(title, client.options);
		}
		if ("controles".equals(titleMode)) {
			return new ControlsOptionsScreen(title, client.options);
		}
		if ("mundos".equals(titleMode)) {
			return new SelectWorldScreen(title);
		}
		if ("novomundo".equals(titleMode)) {
			return new CreateWorldScreen(title);
		}
		return null;
	}

	private void tickMenuMode(MinecraftClient client) {
		boolean ready = client.world != null && client.player != null
				&& (client.currentScreen == null || client.currentScreen instanceof GameMenuScreen);
		if (!ready || --menuTicks > 0) {
			return;
		}
		if ("pausa".equals(menuMode)) {
			client.setScreen(new GameMenuScreen());
		} else if ("emotes".equals(menuMode)) {
			client.setScreen(new EmoteWheelScreen(VfxCore.get().getModules().get(EmotesModule.class), 0));
		} else if (!"mundo".equals(menuMode)) {
			client.setScreen(new ClickGuiScreen().withSettings(VfxCore.get().getModules().get(menuMode)));
		}
		playTestEmote();
		menuMode = null;
		shoot("vfx-teste.png");
	}

	/**
	 * {@code -Dvfx.emoteSheet=true}: faz cada emote (no momento do ícone), um por vez, e salva um print
	 * de cada ({@code emote-<id>.png}); depois fecha o jogo. Serve para revisar todas as animações.
	 */
	private void tickEmoteSheet(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			return;
		}
		if (client.currentScreen != null) {
			// Sem foco na janela o jogo abre o menu de pausa sozinho; no teste isso só atrapalha.
			client.setScreen(null);
			return;
		}
		if (--sheetWait > 0) {
			return;
		}
		EmotesModule module = VfxCore.get().getModules().get(EmotesModule.class);

		// Primeiro quadro de cada emote: o print é do emote anterior, já desenhado.
		if (sheetStep >= 0 && sheetStep < Emotes.ALL.size()) {
			saveShot(client, "emote-" + Emotes.ALL.get(sheetStep).id() + ".png");
		}
		sheetStep++;
		if (sheetStep >= Emotes.ALL.size()) {
			client.scheduleStop();
			return;
		}
		Emote emote = Emotes.ALL.get(sheetStep);
		// Espera 12 ticks (0,6 s) até o print, então começa 0,6 s antes do instante do ícone.
		module.play(emote, Math.max(0F, emote.previewSeconds() - 0.6F));
		sheetWait = 12;
	}

	/** {@code -Dvfx.emote=<id>}: faz o emote assim que entra no mundo (o print sai no meio dele). */
	private static void playTestEmote() {
		Emote emote = Emotes.byId(System.getProperty("vfx.emote", ""));
		if (emote != null) {
			VfxCore.get().getModules().get(EmotesModule.class).play(emote);
		}
	}

	private static void saveShot(MinecraftClient client, String name) {
		ScreenshotUtils.saveScreenshot(client.runDirectory, name,
				client.getFramebuffer().viewportWidth, client.getFramebuffer().viewportHeight, client.getFramebuffer());
		VfxCore.LOGGER.info("Print de teste salvo: screenshots/{}", name);
	}

	private void shoot(String name) {
		shotName = name;
		shotTicks = SHOT_DELAY;
	}
}
