package com.vfxclient.launcher.auth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vfxclient.launcher.util.Http;
import com.vfxclient.launcher.util.Json;

/**
 * API oficial do Minecraft para contas Microsoft: perfil, troca de skin e escolha de capa.
 */
public final class MinecraftServices {

	private static final String PROFILE = "https://api.minecraftservices.com/minecraft/profile";

	/**
	 * Uma capa da conta.
	 *
	 * @param id     id da capa
	 * @param alias  nome (ex.: "Migrator")
	 * @param url    imagem
	 * @param active se está em uso
	 */
	public record Cape(String id, String alias, String url, boolean active) {
	}

	private MinecraftServices() {
	}

	/**
	 * Preenche nick, UUID e skin a partir do perfil oficial.
	 *
	 * @param account conta com token válido
	 * @throws IOException se a API falhar (404 = conta sem Minecraft)
	 */
	public static void fillProfile(Account account) throws IOException {
		JsonObject profile = getProfile(account);
		account.username = Json.string(profile, "name");
		account.uuid = Json.string(profile, "id");
		for (JsonElement element : profile.getAsJsonArray("skins")) {
			JsonObject skin = element.getAsJsonObject();
			if ("ACTIVE".equals(Json.string(skin, "state"))) {
				account.skinUrl = Json.string(skin, "url");
				account.skinModel = "SLIM".equalsIgnoreCase(Json.string(skin, "variant")) ? "slim" : "classic";
			}
		}
	}

	/**
	 * @param account conta Microsoft
	 * @return capas da conta
	 * @throws IOException se a API falhar
	 */
	public static List<Cape> capes(Account account) throws IOException {
		List<Cape> capes = new ArrayList<>();
		JsonObject profile = getProfile(account);
		if (profile.has("capes")) {
			for (JsonElement element : profile.getAsJsonArray("capes")) {
				JsonObject cape = element.getAsJsonObject();
				capes.add(new Cape(Json.string(cape, "id"), Json.string(cape, "alias"), Json.string(cape, "url"),
						"ACTIVE".equals(Json.string(cape, "state"))));
			}
		}
		return capes;
	}

	/**
	 * Envia uma skin nova para a conta.
	 *
	 * @param account conta Microsoft
	 * @param png     arquivo PNG 64x64
	 * @param slim    {@code true} para braço fino
	 * @throws IOException se a API recusar
	 */
	public static void uploadSkin(Account account, Path png, boolean slim) throws IOException {
		String boundary = "----VfxClient" + UUID.randomUUID();
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		writePart(body, boundary, "Content-Disposition: form-data; name=\"variant\"\r\n\r\n" + (slim ? "slim" : "classic"));
		body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
				+ "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		body.write(Files.readAllBytes(png));
		body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
		HttpRequest request = HttpRequest.newBuilder(URI.create(PROFILE + "/skins"))
				.timeout(Duration.ofSeconds(60))
				.header("Authorization", "Bearer " + account.accessToken)
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.header("User-Agent", Http.USER_AGENT)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
				.build();
		Http.send(request, HttpResponse.BodyHandlers.ofString());
		fillProfile(account);
	}

	/**
	 * Escolhe a capa em uso ({@code null} esconde a capa).
	 *
	 * @param account conta Microsoft
	 * @param capeId  id da capa ou {@code null}
	 * @throws IOException se a API recusar
	 */
	public static void setCape(Account account, String capeId) throws IOException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(PROFILE + "/capes/active"))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", "Bearer " + account.accessToken)
				.header("User-Agent", Http.USER_AGENT);
		if (capeId == null) {
			builder.DELETE();
		} else {
			JsonObject body = new JsonObject();
			body.addProperty("capeId", capeId);
			builder.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(body.toString()));
		}
		Http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static JsonObject getProfile(Account account) throws IOException {
		HttpRequest request = Http.get(PROFILE).header("Authorization", "Bearer " + account.accessToken).build();
		return JsonParser.parseString(Http.send(request, HttpResponse.BodyHandlers.ofString()).body()).getAsJsonObject();
	}

	private static void writePart(ByteArrayOutputStream out, String boundary, String content) throws IOException {
		out.write(("--" + boundary + "\r\n" + content + "\r\n").getBytes(StandardCharsets.UTF_8));
	}
}
