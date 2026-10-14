package com.vfxclient.core;

import com.vfxclient.core.command.VfxCommands;
import com.vfxclient.core.config.ConfigManager;
import com.vfxclient.core.gui.ClickGuiScreen;
import com.vfxclient.core.hud.ClickTracker;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.ModuleManager;
import com.vfxclient.core.module.general.MenuModule;
import com.vfxclient.core.module.general.ToggleNotificationModule;
import com.vfxclient.core.module.performance.FpsLimiterModule;
import com.vfxclient.core.module.social.EmotesModule;
import com.vfxclient.core.module.util.AutoTextModule;
import com.vfxclient.core.module.util.ToggleSprintModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ponto de entrada do VfxClient dentro do Minecraft.
 *
 * <p>Executado pelo Legacy Fabric quando o jogo está pronto para carregar mods.
 * Aqui só registramos módulos e lemos a config; nada de renderização, porque o
 * contexto OpenGL e as texturas ainda podem não existir.</p>
 */
public final class VfxCore implements ClientModInitializer {

	/** Identificador do mod; também é o namespace dos assets ({@code assets/vfx/...}). */
	public static final String MOD_ID = "vfx";

	/** Nome exibido do client. */
	public static final String NAME = "VfxClient";

	/** Logger compartilhado do client. */
	public static final Logger LOGGER = LogManager.getLogger(NAME);

	private static VfxCore instance;

	private ModuleManager modules;
	private ConfigManager config;
	private String version = "dev";
	private boolean openClickGuiPending;
	private long worldJoinedAt;
	private final TestShots testShots = new TestShots();

	/**
	 * Instância ativa do client.
	 *
	 * @return o núcleo do VfxClient (nunca {@code null} depois da inicialização)
	 */
	public static VfxCore get() {
		return instance;
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		version = FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
				.orElse("dev");

		modules = new ModuleManager();
		modules.registerDefaults();

		config = new ConfigManager(modules, VfxPaths.config());
		config.load();

		LOGGER.info("{} {} carregado com {} módulos. Pasta de dados: {}",
				NAME, version, modules.all().size(), VfxPaths.root());
	}

	/**
	 * Chamado no fim de cada tick do cliente (20 vezes por segundo).
	 *
	 * @param client instância do Minecraft
	 */
	public void onClientTick(MinecraftClient client) {
		trackSession(client);
		if (openClickGuiPending) {
			openClickGuiPending = false;
			if (client.currentScreen == null) {
				client.setScreen(new ClickGuiScreen());
			}
		}
		testShots.tick(client);
		VfxCommands.tick(client);
		modules.tick(client);
		config.tick();
	}

	/**
	 * Chamado quando uma tecla é pressionada durante o jogo (sem tela aberta).
	 *
	 * <p>O menu não abre aqui: o Minecraft ainda repassa este mesmo evento de tecla
	 * para a tela recém-aberta, que o trataria como "fechar". Por isso só marcamos
	 * o pedido e abrimos no fim do tick, depois que o teclado já foi processado.</p>
	 *
	 * @param keyCode código LWJGL da tecla
	 */
	public void onKeyPressed(int keyCode) {
		ClickTracker.onPress(keyCode);
		FpsLimiterModule.markInput();
		modules.get(ToggleSprintModule.class).onKeyPressed(keyCode);
		modules.get(AutoTextModule.class).onKeyPressed(keyCode);
		modules.get(EmotesModule.class).onKeyPressed(keyCode);
		if (keyCode != 0 && MinecraftClient.getInstance().currentScreen == null) {
			// Teclas de atalho dos módulos: liga/desliga e mostra o aviso na tela.
			for (Module module : modules.all()) {
				if (module.toggleKey() == keyCode) {
					module.toggle();
					modules.get(ToggleNotificationModule.class).notify(module);
				}
			}
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen == null && keyCode == modules.get(MenuModule.class).getKey()) {
			openClickGuiPending = true;
		}
	}

	/** Marca quando o jogador entrou no mundo/servidor atual (tempo de sessão do menu de pausa). */
	private void trackSession(MinecraftClient client) {
		if (client.world == null) {
			worldJoinedAt = 0;
		} else if (worldJoinedAt == 0) {
			worldJoinedAt = System.currentTimeMillis();
		}
	}

	/** @return há quanto tempo o jogador está no mundo/servidor atual, em milissegundos */
	public long sessionMillis() {
		return worldJoinedAt == 0 ? 0 : System.currentTimeMillis() - worldJoinedAt;
	}

	/**
	 * Atalho para os Mixins pegarem um módulo sem se preocupar com a inicialização.
	 *
	 * @param type classe do módulo
	 * @param <T>  tipo do módulo
	 * @return o módulo, ou {@code null} se o client ainda não carregou
	 */
	public static <T extends Module> T module(Class<T> type) {
		VfxCore core = instance;
		return core == null || core.modules == null ? null : core.modules.get(type);
	}

	/** @return gerenciador de módulos */
	public ModuleManager getModules() {
		return modules;
	}

	/** @return gerenciador de configuração */
	public ConfigManager getConfig() {
		return config;
	}

	/** @return versão do client, ex.: {@code 0.1.0} */
	public String getVersion() {
		return version;
	}
}
