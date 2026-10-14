package com.vfxclient.launcher.game;

import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.util.Json;

/**
 * Regras de bibliotecas e argumentos dos JSONs de versão (por sistema operacional e recursos).
 *
 * <p>O launcher não liga nenhum "feature" opcional (modo demo, resolução personalizada,
 * quick play), então regras que dependem deles nunca se aplicam.</p>
 */
public final class Rules {

	private Rules() {
	}

	/** @return nome do sistema no formato da Mojang: {@code windows}, {@code osx} ou {@code linux} */
	public static String osName() {
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			return "windows";
		}
		if (os.contains("mac")) {
			return "osx";
		}
		return "linux";
	}

	/** @return {@code 64} ou {@code 32}, usado em classificadores como {@code natives-windows-${arch}} */
	public static String archBits() {
		String arch = System.getProperty("os.arch");
		return arch.contains("64") ? "64" : "32";
	}

	/**
	 * @param rules lista de regras (pode ser {@code null})
	 * @return se o item vale para este computador
	 */
	public static boolean allows(JsonArray rules) {
		if (rules == null || rules.isEmpty()) {
			return true;
		}
		boolean allowed = false;
		for (JsonElement element : rules) {
			JsonObject rule = element.getAsJsonObject();
			if (matches(rule)) {
				allowed = "allow".equals(Json.string(rule, "action"));
			}
		}
		return allowed;
	}

	private static boolean matches(JsonObject rule) {
		if (rule.has("features")) {
			// Recursos opcionais (demo, resolução customizada...) ficam desligados.
			return false;
		}

		JsonObject os = Json.object(rule, "os");
		if (os == null) {
			return true;
		}

		String name = Json.string(os, "name");
		if (name != null && !name.equals(osName())) {
			return false;
		}

		String arch = Json.string(os, "arch");
		if (arch != null) {
			boolean x86 = !System.getProperty("os.arch").contains("64");
			if (arch.equals("x86") != x86) {
				return false;
			}
		}

		return true;
	}
}
