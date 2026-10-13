package com.vfxclient.launcher.cli;

import java.util.Arrays;
import java.util.List;

import com.vfxclient.launcher.BuildInfo;
import com.vfxclient.launcher.auth.Account;
import com.vfxclient.launcher.auth.AccountStore;
import com.vfxclient.launcher.auth.OfflineAuth;
import com.vfxclient.launcher.game.GameLauncher;
import com.vfxclient.launcher.game.VersionManifest;
import com.vfxclient.launcher.optimization.CacheCleaner;
import com.vfxclient.launcher.optimization.Diagnostics;
import com.vfxclient.launcher.profiles.Profile;
import com.vfxclient.launcher.profiles.ProfileStore;
import com.vfxclient.launcher.settings.LauncherSettings;

/**
 * Modo linha de comando, útil para testar e para atalhos:
 *
 * <pre>
 * VfxClient.exe --cli launch [--profile ID] [--offline NICK] [--wait]
 * VfxClient.exe --cli diagnose
 * VfxClient.exe --cli clean-cache
 * VfxClient.exe --cli versions
 * </pre>
 */
public final class Cli {

	private Cli() {
	}

	/**
	 * @param args argumentos depois de {@code --cli}
	 * @return código de saída
	 */
	public static int run(String[] args) {
		List<String> list = Arrays.asList(args);
		String command = list.isEmpty() ? "help" : list.get(0);
		try {
			switch (command) {
				case "launch":
					return launch(list);
				case "diagnose":
					Diagnostics.Report report = Diagnostics.run(null);
					System.out.println(report.summary());
					System.out.println("Espaço livre: " + report.freeDiskMb() + " MB");
					System.out.println("Preset recomendado: " + report.presetLabel());
					report.warnings().forEach(w -> System.out.println("Aviso: " + w));
					return 0;
				case "clean-cache":
					System.out.println("Limpeza: " + CacheCleaner.clean().describe());
					return 0;
				case "versions":
					VersionManifest.releases().stream().limit(20).forEach(System.out::println);
					return 0;
				default:
					System.out.println("VfxClient " + BuildInfo.load().clientVersion() + " — comandos: launch, diagnose, clean-cache, versions");
					return 0;
			}
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			e.printStackTrace();
			return 1;
		}
	}

	private static int launch(List<String> args) throws Exception {
		ProfileStore profiles = new ProfileStore();
		profiles.load();
		AccountStore accounts = new AccountStore();
		accounts.load();
		LauncherSettings settings = LauncherSettings.load();

		Profile profile = profiles.find(option(args, "--profile", ProfileStore.DEFAULT_ID));
		if (profile == null) {
			throw new IllegalArgumentException("Perfil não encontrado");
		}
		Account account = accounts.selected();
		String offline = option(args, "--offline", null);
		if (offline != null) {
			account = OfflineAuth.create(offline);
		}
		GameLauncher.Session session = GameLauncher.launch(profile, account, accounts, settings,
				(stage, fraction, detail) -> System.out.println("[" + (fraction < 0 ? "..." : Math.round(fraction * 100) + "%") + "] "
						+ stage + (detail == null ? "" : " - " + detail)));
		System.out.println("Log do jogo: " + session.log());
		if (args.contains("--wait")) {
			int code = session.process().waitFor();
			System.out.println("O jogo fechou com código " + code);
			return code;
		}
		return 0;
	}

	private static String option(List<String> args, String name, String fallback) {
		int index = args.indexOf(name);
		return index >= 0 && index + 1 < args.size() ? args.get(index + 1) : fallback;
	}
}
