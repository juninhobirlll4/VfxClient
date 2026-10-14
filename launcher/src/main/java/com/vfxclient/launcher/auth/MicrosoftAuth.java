package com.vfxclient.launcher.auth;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BooleanSupplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vfxclient.launcher.util.Http;
import com.vfxclient.launcher.util.Json;

/**
 * Login com conta Microsoft pelo fluxo de "código de dispositivo":
 * o launcher mostra um código, o jogador entra em microsoft.com/link e confirma.
 *
 * <p>Etapas: Microsoft → Xbox Live → XSTS → Minecraft. Requer um Client ID registrado no
 * Azure (veja INSTRUCOES.txt). Sem ele, use conta offline.</p>
 */
public final class MicrosoftAuth {

	private static final String DEVICE_CODE = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
	private static final String TOKEN = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
	private static final String XBL = "https://user.auth.xboxlive.com/user/authenticate";
	private static final String XSTS = "https://xsts.auth.xboxlive.com/xsts/authorize";
	private static final String MC_LOGIN = "https://api.minecraftservices.com/authentication/login_with_xbox";
	private static final String SCOPE = "XboxLive.signin offline_access";

	/**
	 * Código mostrado ao jogador.
	 *
	 * @param userCode        código a digitar
	 * @param verificationUri página onde digitar
	 * @param deviceCode      código interno para consultar
	 * @param interval        segundos entre consultas
	 * @param expiresIn       segundos até expirar
	 */
	public record DeviceCode(String userCode, String verificationUri, String deviceCode, int interval, int expiresIn) {
	}

	private final String clientId;

	/**
	 * @param clientId Client ID do app registrado no Azure
	 */
	public MicrosoftAuth(String clientId) {
		if (clientId == null || clientId.isBlank()) {
			throw new IllegalArgumentException("Configure o Client ID da Microsoft em Configurações (veja INSTRUCOES.txt).");
		}
		this.clientId = clientId.trim();
	}

	/**
	 * Pede um código de dispositivo.
	 *
	 * @return código para mostrar ao jogador
	 * @throws IOException se a Microsoft recusar (ex.: Client ID inválido)
	 */
	public DeviceCode requestCode() throws IOException {
		JsonObject json = postForm(DEVICE_CODE, Map.of("client_id", clientId, "scope", SCOPE));
		return new DeviceCode(Json.string(json, "user_code"), Json.string(json, "verification_uri"),
				Json.string(json, "device_code"), json.get("interval").getAsInt(), json.get("expires_in").getAsInt());
	}

	/**
	 * Espera o jogador confirmar o código e completa o login até o Minecraft.
	 *
	 * @param code      código pedido antes
	 * @param cancelled retorna {@code true} se o jogador desistiu
	 * @return conta Microsoft pronta para jogar
	 * @throws IOException se expirar, for recusado ou a conta não tiver o Minecraft
	 */
	public Account waitForLogin(DeviceCode code, BooleanSupplier cancelled) throws IOException {
		long deadline = System.currentTimeMillis() + code.expiresIn() * 1000L;
		int interval = Math.max(1, code.interval());
		while (System.currentTimeMillis() < deadline) {
			if (cancelled.getAsBoolean()) {
				throw new IOException("Login cancelado");
			}
			sleep(interval);
			try {
				JsonObject token = postForm(TOKEN, Map.of(
						"grant_type", "urn:ietf:params:oauth:grant-type:device_code",
						"client_id", clientId,
						"device_code", code.deviceCode()));
				return finish(token);
			} catch (Http.HttpStatusException e) {
				String error = errorCode(e.body());
				if ("authorization_pending".equals(error)) {
					continue;
				}
				if ("slow_down".equals(error)) {
					interval += 5;
					continue;
				}
				if ("expired_token".equals(error)) {
					throw new IOException("O código expirou. Tente de novo.");
				}
				if ("authorization_declined".equals(error)) {
					throw new IOException("O login foi recusado na página da Microsoft.");
				}
				throw e;
			}
		}

		throw new IOException("O código expirou. Tente de novo.");
	}

	/**
	 * Renova o token de uma conta Microsoft (sem pedir login de novo).
	 *
	 * @param account conta salva
	 * @return conta atualizada
	 * @throws IOException se o token de renovação não valer mais
	 */
	public Account refresh(Account account) throws IOException {
		JsonObject token = postForm(TOKEN, Map.of(
				"grant_type", "refresh_token",
				"client_id", clientId,
				"refresh_token", account.refreshToken,
				"scope", SCOPE));
		return finish(token);
	}

	private Account finish(JsonObject microsoftToken) throws IOException {
		String msAccess = Json.string(microsoftToken, "access_token");
		String refresh = Json.string(microsoftToken, "refresh_token");

		JsonObject xblBody = new JsonObject();
		JsonObject xblProps = new JsonObject();
		xblProps.addProperty("AuthMethod", "RPS");
		xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
		xblProps.addProperty("RpsTicket", "d=" + msAccess);
		xblBody.add("Properties", xblProps);
		xblBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
		xblBody.addProperty("TokenType", "JWT");
		JsonObject xbl = postJson(XBL, xblBody, null);
		String xblToken = Json.string(xbl, "Token");
		String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

		JsonObject xstsBody = new JsonObject();
		JsonObject xstsProps = new JsonObject();
		xstsProps.addProperty("SandboxId", "RETAIL");
		JsonArray tokens = new JsonArray();
		tokens.add(xblToken);
		xstsProps.add("UserTokens", tokens);
		xstsBody.add("Properties", xstsProps);
		xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
		xstsBody.addProperty("TokenType", "JWT");
		JsonObject xsts;
		try {
			xsts = postJson(XSTS, xstsBody, null);
		} catch (Http.HttpStatusException e) {
			throw new IOException(xstsMessage(e.body()), e);
		}

		String xstsToken = Json.string(xsts, "Token");

		JsonObject mcBody = new JsonObject();
		mcBody.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
		JsonObject mc;
		try {
			mc = postJson(MC_LOGIN, mcBody, null);
		} catch (Http.HttpStatusException e) {
			if (e.status() == 403) {
				throw new IOException("A Mojang recusou o login deste aplicativo. Apps novos precisam ser aprovados pela Mojang "
						+ "para usar a API do Minecraft (veja INSTRUCOES.txt). Use conta offline enquanto isso.", e);
			}
			throw e;
		}

		String mcToken = Json.string(mc, "access_token");
		long expires = System.currentTimeMillis() + mc.get("expires_in").getAsLong() * 1000L;

		Account account = new Account();
		account.type = Account.Type.MICROSOFT;
		account.accessToken = mcToken;
		account.refreshToken = refresh;
		account.expiresAt = expires;
		try {
			MinecraftServices.fillProfile(account);
		} catch (Http.HttpStatusException e) {
			if (e.status() == 404) {
				throw new IOException("Esta conta Microsoft não tem o Minecraft: Java Edition.", e);
			}
			throw e;
		}

		return account;
	}

	private static String xstsMessage(String body) {
		if (body != null && body.contains("2148916233")) {
			return "Esta conta Microsoft ainda não tem perfil Xbox. Entre uma vez em xbox.com e tente de novo.";
		}
		if (body != null && body.contains("2148916238")) {
			return "Conta de menor de idade: um adulto precisa adicioná-la a uma Família Microsoft.";
		}
		if (body != null && (body.contains("2148916235") || body.contains("2148916236") || body.contains("2148916237"))) {
			return "O Xbox Live não está disponível ou exige verificação de idade neste país.";
		}
		return "O Xbox Live recusou o login.";
	}

	private static String errorCode(String body) {
		try {
			return Json.string(JsonParser.parseString(body).getAsJsonObject(), "error");
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static JsonObject postForm(String url, Map<String, String> form) throws IOException {
		StringJoiner joiner = new StringJoiner("&");
		for (Map.Entry<String, String> entry : form.entrySet()) {
			joiner.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
		}
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("User-Agent", Http.USER_AGENT)
				.POST(HttpRequest.BodyPublishers.ofString(joiner.toString()))
				.build();
		return JsonParser.parseString(Http.send(request, HttpResponse.BodyHandlers.ofString()).body()).getAsJsonObject();
	}

	static JsonObject postJson(String url, JsonObject body, String bearer) throws IOException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("User-Agent", Http.USER_AGENT)
				.POST(HttpRequest.BodyPublishers.ofString(body.toString()));
		if (bearer != null) {
			builder.header("Authorization", "Bearer " + bearer);
		}
		String response = Http.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body();
		return response == null || response.isBlank() ? new JsonObject() : JsonParser.parseString(response).getAsJsonObject();
	}

	private static void sleep(int seconds) throws IOException {
		try {
			Thread.sleep(seconds * 1000L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrompido", e);
		}
	}
}
