package com.vfxclient.core.module.general;

import java.util.List;

import com.google.gson.JsonObject;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.config.ConfigManager;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ButtonSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.TextSetting;

/**
 * Perfis de configuração: conjuntos completos de opções (HUD, cores, desempenho...) que dá
 * para trocar com um clique. Ex.: um perfil "PvP" e outro "Construção".
 *
 * <p>As opções deste módulo não são salvas dentro dos perfis (seria circular).</p>
 */
public class ConfigProfilesModule extends Module {

	private String[] cachedNames = {ConfigManager.DEFAULT_PROFILE};

	private final ChoiceSetting selected = add(new ChoiceSetting("selected", "Perfil selecionado", 0) {
		@Override
		public String[] options() {
			// Lista guardada em memória: a GUI chama isto a cada frame.
			return cachedNames;
		}
	});
	private final TextSetting newName = add(new TextSetting("newName", "Nome do perfil novo", "PvP", 24));
	private final ButtonSetting use = add(new ButtonSetting("use", "Usar o perfil selecionado", "Usar", new Runnable() {
		@Override
		public void run() {
			config().switchTo(selected.format());
			syncSelection();
		}
	}));
	private final ButtonSetting create = add(new ButtonSetting("create", "Criar perfil com as opções atuais", "Criar", new Runnable() {
		@Override
		public void run() {
			config().createFromCurrent(newName.get());
			syncSelection();
		}
	}));
	private final ButtonSetting delete = add(new ButtonSetting("delete", "Apagar o perfil selecionado", "Apagar", new Runnable() {
		@Override
		public void run() {
			config().delete(selected.format());
			syncSelection();
		}
	}));

	public ConfigProfilesModule() {
		super("configProfiles", "Perfis de config", "Troque todas as opções de uma vez", Category.GENERAL, true);
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	@Override
	public String getDescription() {
		ConfigManager config = VfxCore.get() == null ? null : config();
		return config == null ? super.getDescription() : "Em uso: " + config.getActiveProfile();
	}

	@Override
	public void onSettingsOpened() {
		syncSelection();
	}

	/** Deixa o seletor apontando para o perfil em uso. */
	private void syncSelection() {
		List<String> names = config().listProfiles();
		cachedNames = names.toArray(new String[0]);
		int index = names.indexOf(config().getActiveProfile());
		selected.set(Math.max(0, index));
	}

	@Override
	public JsonObject toJson() {
		return new JsonObject();
	}

	@Override
	public void fromJson(JsonObject json) {
	}

	private static ConfigManager config() {
		return VfxCore.get().getConfig();
	}
}
