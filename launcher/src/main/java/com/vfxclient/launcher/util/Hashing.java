package com.vfxclient.launcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Cálculo de SHA-1 para verificar arquivos baixados. */
public final class Hashing {

	private Hashing() {
	}

	/**
	 * @param file arquivo
	 * @return SHA-1 em hexadecimal minúsculo
	 * @throws IOException se não der para ler
	 */
	public static String sha1(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[64 * 1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Confere se o arquivo existe e bate com o tamanho e o hash esperados.
	 *
	 * @param file arquivo
	 * @param sha1 hash esperado ({@code null} = só confere se existe)
	 * @param size tamanho esperado ({@code <= 0} = não confere)
	 * @return {@code true} se está ok
	 */
	public static boolean matches(Path file, String sha1, long size) {
		try {
			if (!Files.isRegularFile(file)) {
				return false;
			}
			if (size > 0 && Files.size(file) != size) {
				return false;
			}
			return sha1 == null || sha1.equalsIgnoreCase(sha1(file));
		} catch (IOException e) {
			return false;
		}
	}
}
