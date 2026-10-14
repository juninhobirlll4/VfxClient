package com.vfxclient.launcher.game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.BuildInfo;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.auth.Account;
import com.vfxclient.launcher.auth.AccountStore;
import com.vfxclient.launcher.auth.MicrosoftAuth;
import com.vfxclient.launcher.mods.ModInstaller;
import com.vfxclient.launcher.optimization.Diagnostics;
import com.vfxclient.launcher.optimization.JvmFlags;
import com.vfxclient.launcher.profiles.Profile;
import com.vfxclient.launcher.settings.LauncherSettings;
import com.vfxclient.launcher.util.Json;

/**
 * Prepara e abre o Minecraft para um perfil: instala o Legacy Fabric (perfil VfxClient),
 * baixa/verifica versão, bibliotecas, assets e Java, instala o mod, extrai os nativos,
 * renova o login Microsoft se preciso e inicia o processo do jogo.
 */
public final class GameLauncher {

	/**
	 * Jogo em execução.
	 *
	 * @param process processo do Minecraft
	 * @param log     arquivo com a saída do jogo
	 */
	public record Session(Process process, Path log) {
	}

	private GameLauncher() {
	}

	/**
	 * @param profile  perfil a abrir
	 * @param account  conta a usar
	 * @param accounts onde salvar a conta se o token for renovado
	 * @param settings configurações do launcher (Client ID da Microsoft)
	 * @param progress andamento
	 * @return o jogo aberto
	 * @throws IOException se alguma etapa falhar (a mensagem explica qual)
	 */
	public static Session launch(Profile profile, Account account, AccountStore accounts, LauncherSettings settings,
			Progress progress) throws IOException {
		if (account == null) {
			throw new IOException("Escolha ou crie uma conta na aba Conta antes de jogar.");
		}
		Path gameDir = profile.gameDir();
		Files.createDirectories(gameDir);

		// 1. Versão: Legacy Fabric (VfxClient), Forge, OptiFine ou o Minecraft oficial.
		String versionId = switch (profile.kind) {
			case VFX -> LegacyFabric.install(profile.minecraftVersion, progress);
			case FORGE -> Forge.install(profile.minecraftVersion, progress);
			case OPTIFINE -> prepareOptiFine(profile, progress);
			default -> profile.minecraftVersion;
		};
		progress.update("Lendo a versão " + versionId, -1, null);
		VersionResolver.Resolved version = VersionResolver.resolve(versionId, progress);

		// 2. Jar do cliente e bibliotecas.
		List<Downloader.Task> tasks = new ArrayList<>();
		JsonObject client = Json.object(Json.object(version.json(), "downloads"), "client");
		Path clientJar = LauncherPaths.versions().resolve(version.jarId()).resolve(version.jarId() + ".jar");
		if (client != null) {
			tasks.add(new Downloader.Task(Json.string(client, "url"), clientJar, Json.string(client, "sha1"), client.get("size").getAsLong()));
		}
		LibraryInstaller.Plan libraries = LibraryInstaller.plan(version);
		tasks.addAll(libraries.downloads());
		Downloader.run("Bibliotecas", tasks, progress);

		// 3. Assets e Java.
		AssetInstaller.Assets assets = AssetInstaller.install(version, progress);
		Path java = JavaRuntimeInstaller.install(version, progress);

		// 4. Nativos (DLLs do LWJGL), recriados a cada início.
		progress.update("Extraindo arquivos nativos", -1, null);
		Path natives = LauncherPaths.versions().resolve(version.id()).resolve("natives");
		deleteTree(natives);
		LibraryInstaller.extractNatives(libraries.nativeJars(), natives);

		// 5. Mod do VfxClient e skin local.
		if (profile.isVfx()) {
			progress.update("Instalando o VfxClient", -1, null);
			ModInstaller.install(gameDir);
			Path skin = account.type == Account.Type.OFFLINE ? localSkin(account) : null;
			ModInstaller.installLocalSkin(gameDir, skin);
			if (profile.kind == Profile.Kind.VFX) {
				if (profile.optifineWithVfx) {
					OptiFine.autoDetect();
				}
				boolean withOptifine = OptiFine.syncFabricMods(gameDir, profile.optifineWithVfx);
				if (profile.optifineWithVfx && !withOptifine) {
					progress.update("OptiFine não encontrado", -1, "o VfxClient vai abrir sem o OptiFine");
				}
			}
		}

		if (profile.kind == Profile.Kind.FORGE) {
			OptiFine.syncForgeMod(gameDir, profile.optifineWithForge);
		}

		// 6. Login Microsoft em dia.
		if (account.type == Account.Type.MICROSOFT && !account.tokenValid()) {
			progress.update("Renovando o login da Microsoft", -1, null);
			if (!settings.microsoftEnabled()) {
				throw new IOException("O login Microsoft venceu e não há Client ID configurado para renovar.");
			}
			Account refreshed = new MicrosoftAuth(settings.microsoftClientId).refresh(account);
			copy(refreshed, account);
			accounts.put(account);
			accounts.save();
		}

		// 7. Linha de comando e início.
		progress.update("Iniciando o Minecraft", -1, null);
		List<String> command = buildCommand(profile, account, version, java, clientJar, libraries.classpath(), natives, assets, gameDir);
		Path log = gameDir.resolve("logs").resolve("launcher-output.log");
		Files.createDirectories(log.getParent());
		ProcessBuilder builder = new ProcessBuilder(command)
				.directory(gameDir.toFile())
				.redirectErrorStream(true)
				.redirectOutput(log.toFile());
		Process process = builder.start();
		progress.update("Jogo aberto", 1, profile.name);
		return new Session(process, log);
	}

	/** O OptiFine precisa do jar do 1.8.9 e do Java 8 prontos antes de montar a própria versão. */
	private static String prepareOptiFine(Profile profile, Progress progress) throws IOException {
		OptiFine.autoDetect();
		VersionResolver.Resolved vanilla = VersionResolver.resolve(profile.minecraftVersion, progress);
		JsonObject client = Json.object(Json.object(vanilla.json(), "downloads"), "client");
		Path clientJar = LauncherPaths.versions().resolve(vanilla.jarId()).resolve(vanilla.jarId() + ".jar");
		Downloader.run("Minecraft " + vanilla.id(), List.of(new Downloader.Task(Json.string(client, "url"), clientJar,
				Json.string(client, "sha1"), client.get("size").getAsLong())), progress);
		Path java = JavaRuntimeInstaller.install(vanilla, progress);
		return OptiFine.install(vanilla, java, progress);
	}

	/**
	 * @param account conta offline
	 * @return caminho onde o launcher guarda a skin local dessa conta
	 */
	public static Path localSkin(Account account) {
		return LauncherPaths.launcher().resolve("skins").resolve(account.uuid + ".png");
	}

	static List<String> buildCommand(Profile profile, Account account, VersionResolver.Resolved version, Path java,
			Path clientJar, List<Path> classpathLibs, Path natives, AssetInstaller.Assets assets, Path gameDir) {
		JsonObject json = version.json();
		List<String> classpath = new ArrayList<>();
		for (Path lib : classpathLibs) {
			classpath.add(lib.toString());
		}
		classpath.add(clientJar.toString());

		Map<String, String> vars = new HashMap<>();
		vars.put("auth_player_name", account.username);
		vars.put("version_name", version.id());
		vars.put("game_directory", gameDir.toString());
		vars.put("assets_root", assets.root().toString());
		vars.put("game_assets", assets.root().toString());
		vars.put("assets_index_name", assets.indexName());
		vars.put("auth_uuid", account.uuid);
		vars.put("auth_access_token", account.accessToken);
		vars.put("auth_session", "token:" + account.accessToken + ":" + account.uuid);
		vars.put("auth_xuid", account.xuid == null ? "0" : account.xuid);
		vars.put("clientid", "0");
		vars.put("user_properties", "{}");
		vars.put("user_type", account.userType());
		vars.put("version_type", "VfxClient");
		vars.put("natives_directory", natives.toString());
		vars.put("launcher_name", "VfxClient");
		vars.put("launcher_version", BuildInfo.load().clientVersion());
		vars.put("classpath", String.join(File.pathSeparator, classpath));
		vars.put("classpath_separator", File.pathSeparator);
		vars.put("library_directory", LauncherPaths.libraries().toString());

		List<String> command = new ArrayList<>();
		command.add(java.toString());
		command.addAll(JvmFlags.forProfile(profile));
		command.add("-Dminecraft.launcher.brand=VfxClient");
		command.add("-Dminecraft.launcher.version=" + BuildInfo.load().clientVersion());
		if (profile.isVfx()) {
			command.add("-Dvfx.profile=" + profile.configProfile);
			String preset = presetFor(profile);
			if (preset != null) {
				command.add("-Dvfx.preset=" + preset);
			}
		}

		JsonObject arguments = Json.object(json, "arguments");
		if (arguments != null && arguments.has("jvm")) {
			command.addAll(expand(arguments.getAsJsonArray("jvm"), vars));
		} else {
			// Modos de teste do mod (prints automáticos), repassados se o launcher foi aberto com eles.
			for (String key : new String[] {"vfx.titleShot", "vfx.openMenu", "vfx.emote", "vfx.emoteRelay"}) {
				String value = System.getProperty(key);
				if (value != null) {
					command.add("-D" + key + "=" + value);
				}
			}
			command.add("-Djava.library.path=" + natives);
			command.add("-cp");
			command.add(vars.get("classpath"));
		}
		command.add(Json.string(json, "mainClass"));

		String legacyArgs = Json.string(json, "minecraftArguments");
		if (legacyArgs != null) {
			for (String part : legacyArgs.split(" ")) {
				command.add(substitute(part, vars));
			}
		}
		if (arguments != null && arguments.has("game")) {
			command.addAll(expand(arguments.getAsJsonArray("game"), vars));
		}
		// Entrar direto num servidor (atalho/CLI: --join host:porta).
		String join = System.getProperty("vfx.join");
		if (join != null && !join.isBlank()) {
			String[] hostPort = join.trim().split(":", 2);
			command.add("--server");
			command.add(hostPort[0]);
			command.add("--port");
			command.add(hostPort.length > 1 ? hostPort[1] : "25565");
		}
		return command;
	}

	/** Preset enviado ao VfxClient: o escolhido no perfil, ou o do diagnóstico no modo automático. */
	private static String presetFor(Profile profile) {
		switch (profile.preset) {
			case "low":
			case "balanced":
			case "high":
				return profile.preset;
			case "auto":
				return profile.autoOptimize ? Diagnostics.run(profile).recommendedPreset() : null;
			default:
				return null;
		}
	}

	private static List<String> expand(JsonArray args, Map<String, String> vars) {
		List<String> out = new ArrayList<>();
		for (JsonElement element : args) {
			if (element.isJsonPrimitive()) {
				out.add(substitute(element.getAsString(), vars));
				continue;
			}
			JsonObject conditional = element.getAsJsonObject();
			if (!Rules.allows(conditional.getAsJsonArray("rules"))) {
				continue;
			}
			JsonElement value = conditional.get("value");
			if (value.isJsonArray()) {
				for (JsonElement v : value.getAsJsonArray()) {
					out.add(substitute(v.getAsString(), vars));
				}
			} else {
				out.add(substitute(value.getAsString(), vars));
			}
		}

		return out;
	}

	private static String substitute(String text, Map<String, String> vars) {
		String result = text;
		for (Map.Entry<String, String> entry : vars.entrySet()) {
			result = result.replace("${" + entry.getKey() + "}", entry.getValue());
		}
		return result;
	}

	private static void copy(Account from, Account to) {
		to.username = from.username;
		to.uuid = from.uuid;
		to.accessToken = from.accessToken;
		to.refreshToken = from.refreshToken;
		to.expiresAt = from.expiresAt;
		to.skinUrl = from.skinUrl;
		to.skinModel = from.skinModel;
	}

	private static void deleteTree(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			return;
		}
		try (var walk = Files.walk(dir)) {
			for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}
}
