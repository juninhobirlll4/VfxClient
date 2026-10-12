package com.vfxclient.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.setting.Setting;
import net.minecraft.client.MinecraftClient;

/**
 * Funcionalidade do client que pode ser ligada e desligada pela Click GUI.
 *
 * <p>Para criar um módulo: estenda esta classe (ou {@link HudModule}), declare as
 * opções com {@link #add(Setting)} no construtor e registre em
 * {@link ModuleManager#registerDefaults()}.</p>
 */
public abstract class Module {

	private final String id;
	private final String name;
	private final String description;
	private final Category category;
	private final boolean enabledByDefault;
	private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
	private boolean enabled;

	/**
	 * @param id               chave do JSON (minúsculas, sem espaços)
	 * @param name             nome exibido
	 * @param description      descrição curta exibida no cartão
	 * @param category         categoria da Click GUI
	 * @param enabledByDefault se começa ligado numa instalação nova
	 */
	protected Module(String id, String name, String description, Category category, boolean enabledByDefault) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.enabledByDefault = enabledByDefault;
		this.enabled = enabledByDefault;
	}

	/**
	 * Registra uma opção do módulo.
	 *
	 * @param setting opção
	 * @param <S>     tipo da opção
	 * @return a própria opção, para guardar em um campo
	 */
	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	/** Chamado quando o módulo é ligado. */
	protected void onEnable() {
	}

	/** Chamado quando o módulo é desligado. */
	protected void onDisable() {
	}

	/**
	 * Chamado a cada tick do cliente enquanto o módulo está ligado.
	 *
	 * @param client instância do Minecraft
	 */
	public void onTick(MinecraftClient client) {
	}

	/**
	 * Liga ou desliga o módulo e agenda o salvamento da config.
	 *
	 * @param enabled novo estado
	 */
	public void setEnabled(boolean enabled) {
		if (isAlwaysOn()) {
			enabled = true;
		}
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
		markDirty();
	}

	/** Chamado pela Click GUI quando a tela de opções deste módulo abre (ex.: para ler valores do jogo). */
	public void onSettingsOpened() {
	}

	/**
	 * Chamado pela Click GUI depois que o jogador muda uma opção deste módulo.
	 *
	 * @param setting opção alterada
	 */
	public void onSettingChanged(Setting<?> setting) {
	}

	/**
	 * Procura uma opção pelo id.
	 *
	 * @param id chave do JSON
	 * @return a opção ou {@code null}
	 */
	public Setting<?> getSetting(String id) {
		for (Setting<?> setting : settings) {
			if (setting.getId().equals(id)) {
				return setting;
			}
		}
		return null;
	}

	/** @return {@code true} para módulos que não podem ser desligados (sem chave na GUI) */
	public boolean isAlwaysOn() {
		return false;
	}

	/** Inverte o estado do módulo. */
	public void toggle() {
		setEnabled(!enabled);
	}

	/** Volta estado e opções ao padrão. */
	public void resetToDefaults() {
		for (Setting<?> setting : settings) {
			setting.reset();
		}
		setEnabled(enabledByDefault);
		markDirty();
	}

	/** Avisa a config de que algo mudou e precisa ser salvo. */
	protected void markDirty() {
		VfxCore core = VfxCore.get();
		if (core != null && core.getConfig() != null) {
			core.getConfig().markDirty();
		}
	}

	/**
	 * Serializa o módulo para o arquivo de config.
	 *
	 * @return objeto JSON com estado e opções
	 */
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("enabled", enabled);
		JsonObject values = new JsonObject();
		for (Setting<?> setting : settings) {
			values.add(setting.getId(), setting.toJson());
		}
		json.add("settings", values);
		return json;
	}

	/**
	 * Lê o módulo do arquivo de config. Campos ausentes mantêm o valor atual.
	 *
	 * @param json objeto salvo
	 */
	public void fromJson(JsonObject json) {
		if (json.has("settings") && json.get("settings").isJsonObject()) {
			JsonObject values = json.getAsJsonObject("settings");
			for (Setting<?> setting : settings) {
				if (values.has(setting.getId())) {
					setting.fromJson(values.get(setting.getId()));
				}
			}
		}
		if (json.has("enabled") && json.get("enabled").isJsonPrimitive()) {
			boolean value = json.get("enabled").getAsBoolean();
			if (value != enabled) {
				enabled = value;
				if (value) {
					onEnable();
				} else {
					onDisable();
				}
			}
		}
	}

	/** @return chave do JSON */
	public String getId() {
		return id;
	}

	/** @return nome exibido */
	public String getName() {
		return name;
	}

	/** @return descrição curta */
	public String getDescription() {
		return description;
	}

	/** @return categoria */
	public Category getCategory() {
		return category;
	}

	/** @return se está ligado */
	public boolean isEnabled() {
		return enabled;
	}

	/** @return opções do módulo (somente leitura) */
	public List<Setting<?>> getSettings() {
		return Collections.unmodifiableList(settings);
	}

	/**
	 * Utilitário para ler valores extras salvos pelas subclasses.
	 *
	 * @param json objeto
	 * @param key  chave
	 * @param fallback valor se ausente
	 * @return o inteiro salvo ou {@code fallback}
	 */
	protected static int readInt(JsonObject json, String key, int fallback) {
		JsonElement element = json.get(key);
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsInt();
		}
		return fallback;
	}
}
