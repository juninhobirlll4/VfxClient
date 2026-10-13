package com.vfxclient.launcher.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Cliente HTTP compartilhado do launcher. */
public final class Http {

	/** Identificação do launcher nas requisições. */
	public static final String USER_AGENT = "VfxClient-Launcher";

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(15))
			.build();

	private Http() {
	}

	/** @return o cliente HTTP */
	public static HttpClient client() {
		return CLIENT;
	}

	/**
	 * Requisição GET básica.
	 *
	 * @param url endereço
	 * @return construtor da requisição já com User-Agent e timeout
	 */
	public static HttpRequest.Builder get(String url) {
		return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).header("User-Agent", USER_AGENT).GET();
	}

	/**
	 * Baixa um texto.
	 *
	 * @param url endereço
	 * @return corpo da resposta
	 * @throws IOException se falhar ou o status não for 2xx
	 */
	public static String getString(String url) throws IOException {
		return send(get(url).build(), HttpResponse.BodyHandlers.ofString()).body();
	}

	/**
	 * Baixa um JSON.
	 *
	 * @param url endereço
	 * @return objeto JSON
	 * @throws IOException se falhar
	 */
	public static JsonObject getJson(String url) throws IOException {
		JsonElement element = JsonParser.parseString(getString(url));
		if (!element.isJsonObject()) {
			throw new IOException("Resposta não é um objeto JSON: " + url);
		}
		return element.getAsJsonObject();
	}

	/**
	 * Baixa um arquivo direto para o disco.
	 *
	 * @param url  endereço
	 * @param file destino
	 * @throws IOException se falhar
	 */
	public static void download(String url, Path file) throws IOException {
		send(get(url).build(), HttpResponse.BodyHandlers.ofFile(file));
	}

	/**
	 * Envia uma requisição e confere o status.
	 *
	 * @param request requisição
	 * @param handler como ler o corpo
	 * @param <T>     tipo do corpo
	 * @return resposta com status 2xx
	 * @throws IOException se falhar ou o status não for 2xx
	 */
	public static <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
		try {
			HttpResponse<T> response = CLIENT.send(request, handler);
			if (response.statusCode() / 100 != 2) {
				throw new HttpStatusException(response.statusCode(), request.uri().toString(), String.valueOf(response.body()));
			}
			return response;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrompido", e);
		}
	}

	/** Erro de status HTTP (com o corpo da resposta para diagnóstico). */
	public static final class HttpStatusException extends IOException {
		private final int status;
		private final String body;

		HttpStatusException(int status, String url, String body) {
			super("HTTP " + status + " em " + url);
			this.status = status;
			this.body = body;
		}

		/** @return código HTTP */
		public int status() {
			return status;
		}

		/** @return corpo da resposta */
		public String body() {
			return body;
		}
	}
}
