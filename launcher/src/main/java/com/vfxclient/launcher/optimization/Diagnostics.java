package com.vfxclient.launcher.optimization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.profiles.Profile;
import com.vfxclient.launcher.settings.SystemInfo;

/**
 * Diagnóstico rápido do computador antes de abrir o jogo: RAM, processador, placa de vídeo
 * e espaço em disco. Gera avisos e o preset recomendado.
 */
public final class Diagnostics {

	/**
	 * Resultado do diagnóstico.
	 *
	 * @param cores           núcleos
	 * @param totalRamMb      RAM total
	 * @param freeRamMb       RAM livre agora
	 * @param gpus            placas de vídeo
	 * @param freeDiskMb      espaço livre na pasta do launcher
	 * @param recommendedPreset {@code low}, {@code balanced} ou {@code high}
	 * @param warnings        avisos para mostrar
	 */
	public record Report(int cores, int totalRamMb, int freeRamMb, List<String> gpus, long freeDiskMb,
			String recommendedPreset, List<String> warnings) {

		/** @return resumo em uma linha */
		public String summary() {
			return cores + " núcleos · " + gb(totalRamMb) + " de RAM (" + gb(freeRamMb) + " livre) · "
					+ (gpus.isEmpty() ? "vídeo desconhecido" : String.join(", ", gpus));
		}

		/** @return nome do preset recomendado em português */
		public String presetLabel() {
			return switch (recommendedPreset) {
				case "low" -> "Baixo";
				case "balanced" -> "Equilibrado";
				default -> "Alto";
			};
		}

		private static String gb(int mb) {
			return String.format(Locale.ROOT, "%.1f GB", mb / 1024.0);
		}
	}

	private static List<String> cachedGpus;

	private Diagnostics() {
	}

	/**
	 * @param profile perfil que vai abrir (para checar a RAM escolhida)
	 * @return relatório
	 */
	public static Report run(Profile profile) {
		int cores = SystemInfo.cores();
		int total = SystemInfo.totalRamMb();
		int free = SystemInfo.freeRamMb();
		List<String> gpus = gpus();
		long disk = freeDiskMb();
		List<String> warnings = new ArrayList<>();

		if (profile != null && profile.ramMb > SystemInfo.maxSafeRamMb()) {
			warnings.add("A RAM escolhida (" + profile.ramMb + " MB) deixa pouca memória para o Windows. Recomendado: "
					+ SystemInfo.recommendedRamMb() + " MB.");
		}
		if (profile != null && free < profile.ramMb / 2) {
			warnings.add("Pouca RAM livre agora (" + free + " MB). Feche outros programas para evitar travadas.");
		}
		if (disk >= 0 && disk < 2048) {
			warnings.add("Pouco espaço em disco (" + disk + " MB). O primeiro download precisa de uns 500 MB.");
		}
		boolean integrated = gpus.stream().map(g -> g.toLowerCase(Locale.ROOT))
				.allMatch(g -> (g.contains("intel") && !g.contains("arc")) || g.contains("basic") || g.contains("vmware"));
		if (gpus.isEmpty()) {
			integrated = false;
		}
		if (integrated) {
			warnings.add("Vídeo integrado detectado: o preset Equilibrado ou Baixo dá mais FPS.");
		}

		String preset;
		if (cores <= 2 || total < 4096) {
			preset = "low";
		} else if (cores <= 4 || total < 6144 || integrated) {
			preset = "balanced";
		} else {
			preset = "high";
		}
		return new Report(cores, total, free, gpus, disk, preset, warnings);
	}

	private static long freeDiskMb() {
		try {
			Files.createDirectories(LauncherPaths.root());
			FileStore store = Files.getFileStore(LauncherPaths.root());
			return store.getUsableSpace() / 1048576L;
		} catch (IOException e) {
			return -1;
		}
	}

	/** Lê os nomes das placas de vídeo pelo Windows (com tempo limite; resultado guardado). */
	private static synchronized List<String> gpus() {
		if (cachedGpus != null) {
			return cachedGpus;
		}
		List<String> names = new ArrayList<>();
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
			try {
				Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command",
						"Get-CimInstance Win32_VideoController | ForEach-Object { $_.Name }")
						.redirectErrorStream(true).start();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (!line.isBlank()) {
							names.add(line.trim());
						}
					}
				}
				if (!process.waitFor(8, TimeUnit.SECONDS)) {
					process.destroyForcibly();
				}
			} catch (IOException e) {
				// Sem PowerShell: segue sem o nome do vídeo.
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		cachedGpus = names;
		return names;
	}
}
