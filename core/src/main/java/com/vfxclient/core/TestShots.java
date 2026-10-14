package com.vfxclient.core;

import com.vfxclient.core.gui.ClickGuiScreen;
import com.vfxclient.core.gui.VfxTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
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

	/** Chamado a cada tick do cliente. */
	void tick(MinecraftClient client) {
		if (titleMode != null) {
			tickTitleMode(client);
		}
		if (menuMode != null) {
			tickMenuMode(client);
		}
		if (shotTicks > 0 && --shotTicks == 0) {
			ScreenshotUtils.saveScreenshot(client.runDirectory, shotName,
					client.getFramebuffer().viewportWidth, client.getFramebuffer().viewportHeight, client.getFramebuffer());
			VfxCore.LOGGER.info("Print de teste salvo: screenshots/{}", shotName);
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
		} else {
			client.setScreen(new ClickGuiScreen().withSettings(VfxCore.get().getModules().get(menuMode)));
		}
		menuMode = null;
		shoot("vfx-teste.png");
	}

	private void shoot(String name) {
		shotName = name;
		shotTicks = SHOT_DELAY;
	}
}
