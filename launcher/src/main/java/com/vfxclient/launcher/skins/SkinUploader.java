package com.vfxclient.launcher.skins;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;

import com.vfxclient.launcher.BuildInfo;
import com.vfxclient.launcher.auth.Account;
import com.vfxclient.launcher.util.Http;

/**
 * Envia a skin de uma conta offline para o servidor do VfxClient, para quem usa o client ver.
 *
 * <p>Cada conta tem uma chave secreta ({@link Account#skinKey}), criada no primeiro envio. O
 * servidor guarda só o hash dela: quem manda primeiro "reivindica" o nick, e só a mesma chave
 * troca a skin depois.</p>
 */
public final class SkinUploader {

	/** Resultado do envio. */
	public enum Result {
		/** Enviada: os amigos com o VfxClient já veem. */
		OK,
		/** O nick já pertence a outro PC/launcher (outra chave). */
		NICK_TAKEN,
		/** Trocas muito seguidas; tente em alguns segundos. */
		TOO_FAST,
		/** O servidor recusou o arquivo (tamanho ou formato). */
		REJECTED,
		/** Sem internet ou servidor fora do ar. */
		OFFLINE
	}

	private static final int HTTP_FORBIDDEN = 403;
	private static final int HTTP_TOO_MANY = 429;

	private SkinUploader() {
	}

	/**
	 * @param account conta offline dona da skin
	 * @param skin    arquivo PNG da skin
	 * @param slim    braço fino
	 * @return o que aconteceu (nunca lança erro de rede)
	 */
	public static Result upload(Account account, Path skin, boolean slim) {
		String base = BuildInfo.load().servicesUrl();
		if (base.isEmpty() || account.type != Account.Type.OFFLINE) {
			return Result.OFFLINE;
		}

		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/skin/" + account.username + "?model=" + (slim ? "slim" : "default")))
					.timeout(Duration.ofSeconds(20))
					.header("User-Agent", Http.USER_AGENT)
					.header("X-Vfx-Key", keyFor(account))
					.PUT(HttpRequest.BodyPublishers.ofFile(skin))
					.build();
			HttpResponse<String> response = Http.client().send(request, HttpResponse.BodyHandlers.ofString());
			return resultFor(response.statusCode());
		} catch (IOException e) {
			return Result.OFFLINE;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Result.OFFLINE;
		}
	}

	private static Result resultFor(int status) {
		if (status / 100 == 2) {
			return Result.OK;
		}
		if (status == HTTP_FORBIDDEN) {
			return Result.NICK_TAKEN;
		}
		if (status == HTTP_TOO_MANY) {
			return Result.TOO_FAST;
		}
		return status / 100 == 4 ? Result.REJECTED : Result.OFFLINE;
	}

	/** Chave secreta da conta; cria uma no primeiro uso (quem grava a conta guarda no disco). */
	private static String keyFor(Account account) {
		if (account.skinKey == null || account.skinKey.length() != 64) {
			byte[] bytes = new byte[32];
			new SecureRandom().nextBytes(bytes);
			StringBuilder hex = new StringBuilder(64);
			for (byte b : bytes) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			account.skinKey = hex.toString();
		}
		return account.skinKey;
	}
}
