package com.vfxclient.core.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.ModuleManager;
import org.lwjgl.input.Keyboard;

/**
 * Lê e grava {@code .minecraft/vfxclient/config/modules.json}.
 *
 * <ul>
 *   <li>Mudanças feitas no jogo são salvas em até 1 segundo (com debounce).</li>
 *   <li>Edições manuais no arquivo são recarregadas sozinhas (hot-reload), sem reiniciar o jogo.</li>
 *   <li>Arquivo corrompido não derruba o jogo: é renomeado para {@code .broken} e o padrão é usado.</li>
 * </ul>
 */
public final class ConfigManager {

	private static final int FORMAT_VERSION = 1;
	private static final int SAVE_DELAY_TICKS = 20;
	private static final int RELOAD_CHECK_TICKS = 20;

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final ModuleManager modules;
	private final Path file;

	private int clickGuiKey = Keyboard.KEY_RSHIFT;
	private boolean dirty;
	private int ticksSinceDirty;
	private int ticksSinceCheck;
	private long lastKnownModified = -1L;

	/**
	 * @param modules módulos cujo estado será salvo
	 * @param file    caminho do JSON
	 */
	public ConfigManager(ModuleManager modules, Path file) {
		this.modules = modules;
		this.file = file;
	}

	/** Carrega o arquivo; se não existir, cria com os valores padrão. */
	public void load() {
		if (!Files.exists(file)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement root = new JsonParser().parse(reader);
			if (!root.isJsonObject()) {
				throw new IllegalStateException("a raiz do JSON não é um objeto");
			}
			apply(root.getAsJsonObject());
			lastKnownModified = Files.getLastModifiedTime(file).toMillis();
		} catch (Exception e) {
			VfxCore.LOGGER.error("Config inválida em {}; usando padrão.", file, e);
			backupBroken();
			save();
		}
	}

	private void apply(JsonObject root) {
		if (root.has("clickGuiKey") && root.get("clickGuiKey").isJsonPrimitive()) {
			clickGuiKey = root.get("clickGuiKey").getAsInt();
		}
		if (root.has("modules") && root.get("modules").isJsonObject()) {
			JsonObject saved = root.getAsJsonObject("modules");
			for (Module module : modules.all()) {
				if (saved.has(module.getId()) && saved.get(module.getId()).isJsonObject()) {
					module.fromJson(saved.getAsJsonObject(module.getId()));
				}
			}
		}
	}

	/** Grava o estado atual no disco imediatamente. */
	public void save() {
		JsonObject root = new JsonObject();
		root.addProperty("formatVersion", FORMAT_VERSION);
		root.addProperty("clickGuiKey", clickGuiKey);
		JsonObject saved = new JsonObject();
		for (Module module : modules.all()) {
			saved.add(module.getId(), module.toJson());
		}
		root.add("modules", saved);

		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				gson.toJson(root, writer);
			}
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			lastKnownModified = Files.getLastModifiedTime(file).toMillis();
			dirty = false;
		} catch (IOException e) {
			VfxCore.LOGGER.error("Não foi possível salvar a config em {}", file, e);
		}
	}

	/** Agenda um salvamento (vários avisos seguidos viram uma única gravação). */
	public void markDirty() {
		dirty = true;
		ticksSinceDirty = 0;
	}

	/** Volta todos os módulos ao padrão e salva. */
	public void resetAll() {
		for (Module module : modules.all()) {
			module.resetToDefaults();
		}
		clickGuiKey = Keyboard.KEY_RSHIFT;
		save();
	}

	/** Chamado a cada tick: salva pendências e detecta edições manuais no arquivo. */
	public void tick() {
		if (dirty && ++ticksSinceDirty >= SAVE_DELAY_TICKS) {
			save();
		}
		if (++ticksSinceCheck >= RELOAD_CHECK_TICKS) {
			ticksSinceCheck = 0;
			checkExternalChange();
		}
	}

	private void checkExternalChange() {
		if (dirty || !Files.exists(file)) {
			return;
		}
		try {
			long modified = Files.getLastModifiedTime(file).toMillis();
			if (modified != lastKnownModified) {
				VfxCore.LOGGER.info("Config alterada fora do jogo; recarregando.");
				load();
			}
		} catch (IOException e) {
			VfxCore.LOGGER.warn("Não foi possível verificar a config", e);
		}
	}

	private void backupBroken() {
		try {
			Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			VfxCore.LOGGER.warn("Não foi possível guardar a config corrompida", e);
		}
	}

	/** @return código LWJGL da tecla que abre a Click GUI (padrão: Right Shift) */
	public int getClickGuiKey() {
		return clickGuiKey;
	}
}
