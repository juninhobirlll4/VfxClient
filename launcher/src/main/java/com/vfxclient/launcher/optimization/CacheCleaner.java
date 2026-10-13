package com.vfxclient.launcher.optimization;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.vfxclient.launcher.LauncherPaths;

/**
 * Limpeza de cache: downloads temporários, nativos extraídos (recriados a cada início),
 * logs com mais de 7 dias e relatórios de crash com mais de 30 dias.
 *
 * <p>Nunca apaga mundos, capturas de tela, configurações, contas nem bibliotecas.</p>
 */
public final class CacheCleaner {

	/**
	 * @param files quantos arquivos foram apagados
	 * @param bytes quanto espaço foi liberado
	 */
	public record Result(int files, long bytes) {
		/** @return texto como {@code 42 arquivos, 18,3 MB liberados} */
		public String describe() {
			return files + " arquivos, " + String.format("%.1f MB", bytes / 1048576.0) + " liberados";
		}
	}

	private CacheCleaner() {
	}

	/** @return o que foi limpo */
	public static Result clean() {
		long[] totals = new long[2];
		deleteTree(LauncherPaths.cache(), totals, Duration.ZERO);
		if (Files.isDirectory(LauncherPaths.versions())) {
			try (DirectoryStream<Path> versions = Files.newDirectoryStream(LauncherPaths.versions())) {
				for (Path version : versions) {
					deleteTree(version.resolve("natives"), totals, Duration.ZERO);
				}
			} catch (IOException ignored) {
				// Sem permissão: pula.
			}
		}
		if (Files.isDirectory(LauncherPaths.instances())) {
			try (DirectoryStream<Path> instances = Files.newDirectoryStream(LauncherPaths.instances())) {
				for (Path instance : instances) {
					deleteTree(instance.resolve("logs"), totals, Duration.ofDays(7));
					deleteTree(instance.resolve("crash-reports"), totals, Duration.ofDays(30));
				}
			} catch (IOException ignored) {
				// Sem permissão: pula.
			}
		}
		return new Result((int) totals[0], totals[1]);
	}

	/** Apaga arquivos mais velhos que {@code minAge} dentro da pasta (a pasta em si fica). */
	private static void deleteTree(Path dir, long[] totals, Duration minAge) {
		if (!Files.isDirectory(dir)) {
			return;
		}
		Instant limit = Instant.now().minus(minAge);
		try (Stream<Path> walk = Files.walk(dir)) {
			List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
			for (Path path : paths) {
				if (path.equals(dir)) {
					continue;
				}
				try {
					if (Files.isRegularFile(path)) {
						FileTime modified = Files.getLastModifiedTime(path);
						if (modified.toInstant().isAfter(limit) && !minAge.isZero()) {
							continue;
						}
						long size = Files.size(path);
						Files.delete(path);
						totals[0]++;
						totals[1] += size;
					} else if (Files.isDirectory(path)) {
						try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
							if (!children.iterator().hasNext()) {
								Files.delete(path);
							}
						}
					}
				} catch (IOException ignored) {
					// Arquivo em uso (jogo aberto): fica para a próxima.
				}
			}
		} catch (IOException ignored) {
			// Pasta inacessível: pula.
		}
	}
}
