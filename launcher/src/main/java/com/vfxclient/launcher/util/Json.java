package com.vfxclient.launcher.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Leitura e gravação de JSON com Gson (gravação atômica para não corromper arquivos). */
public final class Json {

	/** Gson com saída legível. */
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private Json() {
	}

	/**
	 * Lê um arquivo JSON como objeto.
	 *
	 * @param file caminho
	 * @return o objeto
	 * @throws IOException se não der para ler ou não for um objeto
	 */
	public static JsonObject readObject(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement element = JsonParser.parseReader(reader);
			if (!element.isJsonObject()) {
				throw new IOException("JSON não é um objeto: " + file);
			}
			return element.getAsJsonObject();
		} catch (RuntimeException e) {
			throw new IOException("JSON inválido: " + file, e);
		}
	}

	/**
	 * Lê um arquivo e converte para uma classe.
	 *
	 * @param file caminho
	 * @param type classe
	 * @param <T>  tipo
	 * @return o objeto, ou {@code null} se o arquivo não existir
	 * @throws IOException se o arquivo existir mas for inválido
	 */
	public static <T> T read(Path file, Class<T> type) throws IOException {
		if (!Files.exists(file)) {
			return null;
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, type);
		} catch (RuntimeException e) {
			throw new IOException("JSON inválido: " + file, e);
		}
	}

	/**
	 * Grava um objeto em JSON de forma atômica (arquivo temporário + troca).
	 *
	 * @param file  destino
	 * @param value objeto
	 * @throws IOException em erro de disco
	 */
	public static void write(Path file, Object value) throws IOException {
		Files.createDirectories(file.getParent());
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
			GSON.toJson(value, writer);
		}
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	/**
	 * @param object objeto
	 * @param key    chave
	 * @return o texto da chave, ou {@code null}
	 */
	public static String string(JsonObject object, String key) {
		return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
	}

	/**
	 * @param object objeto
	 * @param key    chave
	 * @return o objeto filho, ou {@code null}
	 */
	public static JsonObject object(JsonObject object, String key) {
		return object != null && object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : null;
	}
}
