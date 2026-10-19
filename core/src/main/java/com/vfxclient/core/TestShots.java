package com.vfxclient.core;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

import com.vfxclient.core.emote.Emote;
import com.vfxclient.core.emote.Emotes;
import com.vfxclient.core.gui.ClickGuiScreen;
import com.vfxclient.core.gui.CrosshairEditorScreen;
import com.vfxclient.core.gui.EmoteWheelScreen;
import com.vfxclient.core.gui.VfxTitleScreen;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.Setting;
import com.vfxclient.core.module.social.EmotesModule;
import com.vfxclient.core.module.util.CrosshairDrawing;
import com.vfxclient.core.module.util.CrosshairModule;
import com.vfxclient.core.module.visual.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerInfo;
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
 *   <li>{@code -Dvfx.openMenu=mira}: editor da mira; {@code -Dvfx.crosshair=Alvo}: usa essa predefinição como mira;</li>
 *   <li>{@code -Dvfx.enable=serverIp,texturePack} e {@code -Dvfx.fakeServer=Nome@endereço}: liga módulos e finge um servidor da lista;</li>
 *   <li>{@code -Dvfx.set=modulo.opcao=valor,...}: muda opções de módulos;</li>
 *   <li>{@code -Dvfx.zoomHeld=true} e {@code -Dvfx.zoomScroll=n}: zoom como se a tecla estivesse segurada, com n cliques de rodinha;</li>
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
		if (shotTicks == 30) {
			testZoomScroll();
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
		applyTestCrosshair();
		applyTestHud(client);
		if ("mira".equals(menuMode)) {
			client.setScreen(new CrosshairEditorScreen(VfxCore.get().getModules().get(CrosshairModule.class), null));
		} else if ("pausa".equals(menuMode)) {
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

	/** {@code -Dvfx.crosshair=<predefinição>}: usa a predefinição como mira (veja CrosshairDrawing.PRESETS). */
	private static void applyTestCrosshair() {
		String preset = System.getProperty("vfx.crosshair");
		if (preset != null) {
			CrosshairModule module = VfxCore.get().getModules().get(CrosshairModule.class);
			module.setDrawing(CrosshairDrawing.preset(preset));
			module.useDrawing();
		}
	}

	/**
	 * {@code -Dvfx.enable=id,id}: liga esses módulos ({@code -Dvfx.disable} desliga); {@code -Dvfx.fakeServer=Nome@endereço}: finge que você
	 * entrou por esse servidor da lista (com o ícone do VfxClient), para ver o HUD do servidor.
	 */
	private static void applyTestHud(MinecraftClient client) {
		String enable = System.getProperty("vfx.enable");
		if (enable != null) {
			for (String id : enable.split(",")) {
				VfxCore.get().getModules().get(id.trim()).setEnabled(true);
			}
		}

		String disable = System.getProperty("vfx.disable");
		if (disable != null) {
			for (String id : disable.split(",")) {
				VfxCore.get().getModules().get(id.trim()).setEnabled(false);
			}
		}

		applyTestSettings();

		String fake = System.getProperty("vfx.fakeServer");
		if (fake != null) {
			String[] parts = fake.split("@", 2);
			ServerInfo info = new ServerInfo(parts[0], parts.length > 1 ? parts[1] : parts[0], false);
			info.setIcon(testIcon());
			client.setCurrentServerEntry(info);
		}
	}

	/** {@code -Dvfx.set=modulo.opcao=valor,...}: muda opções (booleanas ou numéricas) de módulos. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void applyTestSettings() {
		String set = System.getProperty("vfx.set");
		if (set == null) {
			return;
		}
		for (String entry : set.split(",")) {
			String[] parts = entry.split("[.=]");
			Setting setting = VfxCore.get().getModules().get(parts[0]).getSetting(parts[1]);
			if (setting instanceof BooleanSetting) {
				setting.set(Boolean.valueOf(parts[2]));
			} else if (setting instanceof NumberSetting) {
				setting.set(Double.valueOf(parts[2]));
			}
		}
	}

	/** O ícone do VfxClient (reduzido) em base64, como um servidor mandaria. */
	private static String testIcon() {
		try (InputStream in = TestShots.class.getResourceAsStream("/assets/vfx/icon.png")) {
			BufferedImage image = ImageIO.read(in);
			BufferedImage small = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
			small.createGraphics().drawImage(image, 0, 0, 64, 64, null);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(small, "png", out);
			return Base64.getEncoder().encodeToString(out.toByteArray());
		} catch (IOException e) {
			return null;
		}
	}

	/** {@code -Dvfx.zoomScroll=n}: gira a rodinha n cliques (negativo = para baixo) antes do print. */
	private static void testZoomScroll() {
		int clicks = Integer.getInteger("vfx.zoomScroll", 0);
		ZoomModule zoom = VfxCore.get().getModules().get(ZoomModule.class);
		for (int i = 0; i < Math.abs(clicks); i++) {
			zoom.consumeScroll(clicks > 0 ? 120 : -120);
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
