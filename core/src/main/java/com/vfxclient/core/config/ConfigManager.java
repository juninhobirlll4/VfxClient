package com.vfxclient.core.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.ModuleManager;
import com.vfxclient.core.module.general.MenuModule;

/**
 * Lê e grava as configurações em perfis: {@code .minecraft/vfxclient/config/profiles/<nome>.json}.
 *
 * <ul>
 *   <li>Cada perfil guarda o estado de todos os módulos (posições, cores, opções).</li>
 *   <li>O perfil ativo fica em {@code config/active-profile.txt}; o launcher pode escolher outro
 *       com {@code -Dvfx.profile=<nome>}.</li>
 *   <li>Mudanças feitas no jogo são salvas em até 1 segundo (com debounce).</li>
 *   <li>Edições manuais no arquivo são recarregadas sozinhas (hot-reload).</li>
 *   <li>Arquivo corrompido não derruba o jogo: é renomeado para {@code .broken} e o padrão é usado.</li>
 * </ul>
 */
public final class ConfigManager {

	/** Nome do perfil criado na primeira execução. */
	public static final String DEFAULT_PROFILE = "Padrao";

	private static final int FORMAT_VERSION = 2;
	private static final int SAVE_DELAY_TICKS = 20;
	private static final int RELOAD_CHECK_TICKS = 20;

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final ModuleManager modules;
	private final Path configDir;
	private final Path profilesDir;
	private final Path activeFile;

	private String activeProfile = DEFAULT_PROFILE;
	private boolean dirty;
	private int ticksSinceDirty;
	private int ticksSinceCheck;
	private long lastKnownModified = -1L;

	/**
	 * @param modules   módulos cujo estado será salvo
	 * @param configDir pasta {@code .minecraft/vfxclient/config}
	 */
	public ConfigManager(ModuleManager modules, Path configDir) {
		this.modules = modules;
		this.configDir = configDir;
		this.profilesDir = configDir.resolve("profiles");
		this.activeFile = configDir.resolve("active-profile.txt");
	}

	/** Descobre o perfil ativo e o carrega (criando-o com os valores padrão se preciso). */
	public void load() {
		migrateLegacyFile();
		String requested = System.getProperty("vfx.profile");
		if (requested != null && !sanitize(requested).isEmpty()) {
			activeProfile = sanitize(requested);
		} else {
			activeProfile = readActiveName();
		}
		writeActiveName();
		loadFile(file());
	}

	private void loadFile(Path file) {
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
			backupBroken(file);
			save();
		}
	}

	private void apply(JsonObject root) {
		// Configs da v0.1.0 guardavam a tecla do menu na raiz; hoje ela fica no módulo "menu".
		if (root.has("clickGuiKey") && root.get("clickGuiKey").isJsonPrimitive()) {
			modules.get(MenuModule.class).setKey(root.get("clickGuiKey").getAsInt());
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

	/** Grava o estado atual no perfil ativo, imediatamente. */
	public void save() {
		JsonObject root = new JsonObject();
		root.addProperty("formatVersion", FORMAT_VERSION);
		root.addProperty("profile", activeProfile);
		JsonObject saved = new JsonObject();
		for (Module module : modules.all()) {
			saved.add(module.getId(), module.toJson());
		}
		root.add("modules", saved);

		Path file = file();
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

	// ------------------------------------------------------------------
	// Perfis
	// ------------------------------------------------------------------

	/** @return nome do perfil em uso */
	public String getActiveProfile() {
		return activeProfile;
	}

	/** @return nomes dos perfis existentes, em ordem alfabética (o ativo sempre incluso) */
	public List<String> listProfiles() {
		List<String> names = new ArrayList<String>();
		if (Files.isDirectory(profilesDir)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDir, "*.json")) {
				for (Path path : stream) {
					String name = path.getFileName().toString();
					names.add(name.substring(0, name.length() - ".json".length()));
				}
			} catch (IOException e) {
				VfxCore.LOGGER.warn("Não foi possível listar os perfis", e);
			}
		}
		if (!names.contains(activeProfile)) {
			names.add(activeProfile);
		}
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	/**
	 * Salva o perfil atual e troca para outro (se não existir, ele nasce como cópia do atual).
	 *
	 * @param name nome do perfil
	 * @return nome efetivo (já limpo) ou {@code null} se o nome é inválido
	 */
	public String switchTo(String name) {
		String clean = sanitize(name);
		if (clean.isEmpty()) {
			return null;
		}
		save();
		activeProfile = clean;
		writeActiveName();
		loadFile(file());
		return clean;
	}

	/**
	 * Cria um perfil novo copiando as opções atuais e passa a usá-lo.
	 *
	 * @param name nome do perfil
	 * @return nome efetivo ou {@code null} se inválido
	 */
	public String createFromCurrent(String name) {
		String clean = sanitize(name);
		if (clean.isEmpty()) {
			return null;
		}
		save();
		activeProfile = clean;
		writeActiveName();
		save();
		return clean;
	}

	/**
	 * Apaga um perfil. O perfil em uso não pode ser apagado.
	 *
	 * @param name nome do perfil
	 * @return {@code true} se apagou
	 */
	public boolean delete(String name) {
		String clean = sanitize(name);
		if (clean.isEmpty() || clean.equals(activeProfile)) {
			return false;
		}
		try {
			return Files.deleteIfExists(profilesDir.resolve(clean + ".json"));
		} catch (IOException e) {
			VfxCore.LOGGER.warn("Não foi possível apagar o perfil {}", clean, e);
			return false;
		}
	}

	/**
	 * Deixa um nome de perfil seguro para virar nome de arquivo.
	 *
	 * @param name nome digitado
	 * @return nome com só letras, números, espaço, hífen e sublinhado (até 24 caracteres)
	 */
	public static String sanitize(String name) {
		if (name == null) {
			return "";
		}
		String clean = name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
		return clean.length() > 24 ? clean.substring(0, 24).trim() : clean;
	}

	// ------------------------------------------------------------------
	// Ciclo de vida
	// ------------------------------------------------------------------

	/** Agenda um salvamento (vários avisos seguidos viram uma única gravação). */
	public void markDirty() {
		dirty = true;
		ticksSinceDirty = 0;
	}

	/** Volta todos os módulos ao padrão e salva no perfil ativo. */
	public void resetAll() {
		for (Module module : modules.all()) {
			module.resetToDefaults();
		}
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
		Path file = file();
		if (dirty || !Files.exists(file)) {
			return;
		}
		try {
			long modified = Files.getLastModifiedTime(file).toMillis();
			if (modified != lastKnownModified) {
				VfxCore.LOGGER.info("Config alterada fora do jogo; recarregando.");
				loadFile(file);
			}
		} catch (IOException e) {
			VfxCore.LOGGER.warn("Não foi possível verificar a config", e);
		}
	}

	private Path file() {
		return profilesDir.resolve(activeProfile + ".json");
	}

	private String readActiveName() {
		try {
			if (Files.exists(activeFile)) {
				String name = sanitize(new String(Files.readAllBytes(activeFile), StandardCharsets.UTF_8));
				if (!name.isEmpty()) {
					return name;
				}
			}
		} catch (IOException e) {
			VfxCore.LOGGER.warn("Não foi possível ler o perfil ativo", e);
		}
		return DEFAULT_PROFILE;
	}

	private void writeActiveName() {
		try {
			Files.createDirectories(configDir);
			Files.write(activeFile, activeProfile.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			VfxCore.LOGGER.warn("Não foi possível gravar o perfil ativo", e);
		}
	}

	/** A v0.4 guardava tudo em {@code config/modules.json}: vira o perfil padrão. */
	private void migrateLegacyFile() {
		Path legacy = configDir.resolve("modules.json");
		Path target = profilesDir.resolve(DEFAULT_PROFILE + ".json");
		if (Files.exists(legacy) && !Files.exists(target)) {
			try {
				Files.createDirectories(profilesDir);
				Files.move(legacy, target);
				VfxCore.LOGGER.info("Config antiga movida para o perfil {}", DEFAULT_PROFILE);
			} catch (IOException e) {
				VfxCore.LOGGER.warn("Não foi possível migrar a config antiga", e);
			}
		}
	}

	private static void backupBroken(Path file) {
		try {
			Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			VfxCore.LOGGER.warn("Não foi possível guardar a config corrompida", e);
		}
	}
}
