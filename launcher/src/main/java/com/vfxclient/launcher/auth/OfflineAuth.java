package com.vfxclient.launcher.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Contas offline: só o nick, com o mesmo UUID que servidores não-premium calculam. */
public final class OfflineAuth {

	private OfflineAuth() {
	}

	/**
	 * @param username nick desejado
	 * @return {@code true} se o nick é válido para o Minecraft (3 a 16 letras, números ou _)
	 */
	public static boolean validName(String username) {
		return username != null && username.matches("[A-Za-z0-9_]{3,16}");
	}

	/**
	 * Cria a conta offline.
	 *
	 * @param username nick (já validado)
	 * @return conta pronta para jogar
	 */
	public static Account create(String username) {
		Account account = new Account();
		account.type = Account.Type.OFFLINE;
		account.username = username;
		// Mesmo cálculo dos servidores offline: UUID v3 de "OfflinePlayer:<nick>".
		UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
		account.uuid = uuid.toString().replace("-", "");
		account.accessToken = "0";
		return account;
	}
}
