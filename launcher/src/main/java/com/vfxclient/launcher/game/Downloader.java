package com.vfxclient.launcher.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Hashing;
import com.vfxclient.launcher.util.Http;

/**
 * Baixa vários arquivos em paralelo, pulando os que já existem com o hash certo.
 *
 * <p>Cada arquivo vai primeiro para um temporário e só é movido para o lugar depois de
 * conferido; assim um download interrompido nunca deixa arquivo quebrado.</p>
 */
public final class Downloader {

	private static final int THREADS = 8;
	private static final int RETRIES = 3;

	/**
	 * Um arquivo a baixar.
	 *
	 * @param url  origem
	 * @param file destino
	 * @param sha1 hash esperado (pode ser {@code null})
	 * @param size tamanho esperado (0 = desconhecido)
	 */
	public record Task(String url, Path file, String sha1, long size) {
	}

	private Downloader() {
	}

	/**
	 * Verifica e baixa o que falta.
	 *
	 * @param stage    nome da etapa para o progresso
	 * @param tasks    arquivos
	 * @param progress onde reportar o andamento
	 * @throws IOException se algum arquivo falhar depois das tentativas
	 */
	public static void run(String stage, Collection<Task> tasks, Progress progress) throws IOException {
		// Remove duplicados (mesmo destino), comuns entre bibliotecas de perfis diferentes.
		Map<Path, Task> unique = new LinkedHashMap<>();
		for (Task task : tasks) {
			unique.putIfAbsent(task.file(), task);
		}

		progress.update("Verificando " + stage.toLowerCase(), 0, unique.size() + " arquivos");
		List<Task> missing = new ArrayList<>();
		for (Task task : unique.values()) {
			if (!Hashing.matches(task.file(), task.sha1(), task.size())) {
				missing.add(task);
			}
		}
		if (missing.isEmpty()) {
			progress.update(stage, 1, "Tudo em dia (" + unique.size() + " arquivos)");
			return;
		}

		long totalBytes = 0;
		for (Task task : missing) {
			totalBytes += Math.max(0, task.size());
		}
		AtomicInteger done = new AtomicInteger();
		AtomicLong bytes = new AtomicLong();
		long total = totalBytes;
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (Task task : missing) {
				futures.add(pool.submit(() -> {
					downloadWithRetry(task);
					bytes.addAndGet(Math.max(0, task.size()));
					int count = done.incrementAndGet();
					double fraction = total > 0 ? bytes.get() / (double) total : count / (double) missing.size();
					progress.update(stage, fraction, count + "/" + missing.size() + " arquivos · " + megabytes(bytes.get()) + " de " + megabytes(total));
					return null;
				}));
			}
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (java.util.concurrent.ExecutionException e) {
					Throwable cause = e.getCause();
					throw cause instanceof IOException io ? io : new IOException(cause);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Download interrompido", e);
				}
			}
		} finally {
			pool.shutdownNow();
		}
	}

	private static void downloadWithRetry(Task task) throws IOException {
		IOException last = null;
		for (int attempt = 1; attempt <= RETRIES; attempt++) {
			try {
				download(task);
				return;
			} catch (IOException e) {
				last = e;
			}
		}
		throw new IOException("Não foi possível baixar " + task.url() + " (" + last.getMessage() + ")", last);
	}

	private static void download(Task task) throws IOException {
		Files.createDirectories(task.file().getParent());
		Files.createDirectories(LauncherPaths.cache());
		Path temp = Files.createTempFile(LauncherPaths.cache(), "dl-", ".part");
		try {
			Http.download(task.url(), temp);
			if (!Hashing.matches(temp, task.sha1(), task.size())) {
				throw new IOException("arquivo veio corrompido (hash diferente)");
			}
			Files.move(temp, task.file(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	/**
	 * @param bytes quantidade de bytes
	 * @return texto como {@code 12,3 MB}
	 */
	public static String megabytes(long bytes) {
		return String.format("%.1f MB", bytes / 1048576.0);
	}
}
