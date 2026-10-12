package com.vfxclient.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vfxclient.core.module.hud.ClockModule;
import com.vfxclient.core.module.hud.CoordinatesModule;
import com.vfxclient.core.module.hud.FpsModule;
import com.vfxclient.core.module.hud.PingModule;
import com.vfxclient.core.module.visual.FullbrightModule;
import net.minecraft.client.MinecraftClient;

/** Guarda todos os módulos do client, na ordem em que aparecem na Click GUI. */
public final class ModuleManager {

	private final List<Module> modules = new ArrayList<Module>();

	/** Registra os módulos que vêm com o client. */
	public void registerDefaults() {
		register(new FpsModule());
		register(new PingModule());
		register(new CoordinatesModule());
		register(new ClockModule());
		register(new FullbrightModule());
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
