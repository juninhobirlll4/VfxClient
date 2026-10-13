package com.vfxclient.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vfxclient.core.module.general.ConfigProfilesModule;
import com.vfxclient.core.module.general.HudStyleModule;
import com.vfxclient.core.module.general.LocalSkinModule;
import com.vfxclient.core.module.general.MenuModule;
import com.vfxclient.core.module.general.TitleScreenModule;
import com.vfxclient.core.module.hud.ArmorModule;
import com.vfxclient.core.module.hud.ClockModule;
import com.vfxclient.core.module.hud.CoordinatesModule;
import com.vfxclient.core.module.hud.CpsModule;
import com.vfxclient.core.module.hud.CustomTextModule;
import com.vfxclient.core.module.hud.FpsModule;
import com.vfxclient.core.module.hud.KeystrokesModule;
import com.vfxclient.core.module.hud.PingModule;
import com.vfxclient.core.module.hud.PotionEffectsModule;
import com.vfxclient.core.module.performance.BlockEntityDistanceModule;
import com.vfxclient.core.module.performance.EntityCullingModule;
import com.vfxclient.core.module.performance.EntityDistanceModule;
import com.vfxclient.core.module.performance.PerformanceProfileModule;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import com.vfxclient.core.module.performance.FpsLimiterModule;
import com.vfxclient.core.module.performance.NoCloudsModule;
import com.vfxclient.core.module.performance.ParticleLimitModule;
import com.vfxclient.core.module.performance.VanillaOptionsModule;
import com.vfxclient.core.module.util.BlockOutlineModule;
import com.vfxclient.core.module.util.ChatModule;
import com.vfxclient.core.module.util.CrosshairModule;
import com.vfxclient.core.module.util.HitColorModule;
import com.vfxclient.core.module.util.OwnNametagModule;
import com.vfxclient.core.module.util.ScoreboardModule;
import com.vfxclient.core.module.util.TimeChangerModule;
import com.vfxclient.core.module.util.ToggleSprintModule;
import com.vfxclient.core.module.util.WeatherChangerModule;
import com.vfxclient.core.module.visual.FullbrightModule;
import com.vfxclient.core.module.visual.LowFireModule;
import com.vfxclient.core.module.visual.NoHurtCamModule;
import com.vfxclient.core.module.visual.ZoomModule;
import net.minecraft.client.MinecraftClient;

/** Guarda todos os módulos do client, na ordem em que aparecem na Click GUI. */
public final class ModuleManager {

	private final List<Module> modules = new ArrayList<Module>();
	private final Map<Class<?>, Module> byType = new HashMap<Class<?>, Module>();

	/** Registra os módulos que vêm com o client. */
	public void registerDefaults() {
		// Geral
		register(new MenuModule());
		register(new TitleScreenModule());
		register(new HudStyleModule());
		register(new ConfigProfilesModule());
		register(new LocalSkinModule());
		// HUD
		register(new FpsModule());
		register(new PingModule());
		register(new CoordinatesModule());
		register(new ClockModule());
		register(new CpsModule());
		register(new KeystrokesModule());
		register(new ArmorModule());
		register(new PotionEffectsModule());
		register(new CustomTextModule());
		// Utilidades
		register(new ToggleSprintModule());
		register(new CrosshairModule());
		register(new BlockOutlineModule());
		register(new HitColorModule());
		register(new ChatModule());
		register(new ScoreboardModule());
		register(new TimeChangerModule());
		register(new WeatherChangerModule());
		register(new OwnNametagModule());
		// Visual
		register(new ZoomModule());
		register(new FullbrightModule());
		register(new LowFireModule());
		register(new NoHurtCamModule());
		// Desempenho
		register(new PerformanceProfileModule());
		register(new VanillaOptionsModule());
		register(new RenderTweaksModule());
		register(new EntityCullingModule());
		register(new EntityDistanceModule());
		register(new BlockEntityDistanceModule());
		register(new ParticleLimitModule());
		register(new NoCloudsModule());
		register(new FpsLimiterModule());
	}

	/**
	 * Procura um módulo pela classe.
	 *
	 * @param type classe do módulo
	 * @param <T>  tipo do módulo
	 * @return o módulo (nunca {@code null} para módulos registrados em {@link #registerDefaults()})
	 */
	public <T extends Module> T get(Class<T> type) {
		// Os Mixins chamam isto em todo frame: guardamos o resultado num mapa.
		Module cached = byType.get(type);
		if (cached != null) {
			return type.cast(cached);
		}
		for (Module module : modules) {
			if (type.isInstance(module)) {
				byType.put(type, module);
				return type.cast(module);
			}
		}
		throw new IllegalArgumentException("Módulo não registrado: " + type.getSimpleName());
	}

	/**
	 * Adiciona um módulo.
	 *
	 * @param module módulo novo (o id precisa ser único)
	 */
	public void register(Module module) {
		if (get(module.getId()) != null) {
			throw new IllegalArgumentException("Módulo duplicado: " + module.getId());
		}
		modules.add(module);
	}

	/**
	 * Procura um módulo pelo id.
	 *
	 * @param id chave do módulo
	 * @return o módulo ou {@code null}
	 */
	public Module get(String id) {
		for (Module module : modules) {
			if (module.getId().equals(id)) {
				return module;
			}
		}
		return null;
	}

	/** @return todos os módulos (somente leitura) */
	public List<Module> all() {
		return Collections.unmodifiableList(modules);
	}

	/**
	 * @param category categoria
	 * @return módulos da categoria, na ordem de registro
	 */
	public List<Module> byCategory(Category category) {
		List<Module> result = new ArrayList<Module>();
		for (Module module : modules) {
			if (module.getCategory() == category) {
				result.add(module);
			}
		}
		return result;
	}

	/** @return todos os módulos de HUD */
	public List<HudModule> hudModules() {
		List<HudModule> result = new ArrayList<HudModule>();
		for (Module module : modules) {
			if (module instanceof HudModule) {
				result.add((HudModule) module);
			}
		}
		return result;
	}

	/**
	 * Repassa o tick do cliente para os módulos ligados.
	 *
	 * @param client instância do Minecraft
	 */
	public void tick(MinecraftClient client) {
		for (Module module : modules) {
			if (module.isEnabled()) {
				module.onTick(client);
			}
		}
	}
}
