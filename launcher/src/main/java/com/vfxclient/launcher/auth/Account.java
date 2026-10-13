package com.vfxclient.launcher.auth;

/**
 * Conta usada para jogar.
 *
 * <p>Os campos são públicos e simples porque a classe é gravada direto em
 * {@code accounts.json} pelo Gson.</p>
 */
public final class Account {

	/** Tipo de conta. */
	public enum Type {
		/** Conta original (Microsoft): entra em qualquer servidor e tem skin oficial. */
		MICROSOFT,
		/** Conta offline: só nick; entra em servidores não-premium; skin local. */
		OFFLINE
	}

	/** Tipo da conta. */
	public Type type;
	/** Nick no jogo. */
	public String username;
	/** UUID do jogador sem traços. */
	public String uuid;
	/** Token do Minecraft (Microsoft) ou {@code "0"} (offline). */
	public String accessToken;
	/** Token de renovação da Microsoft. */
	public String refreshToken;
	/** Quando o token do Minecraft vence (epoch em ms). */
	public long expiresAt;
	/** Endereço da skin oficial (Microsoft). */
	public String skinUrl;
	/** Modelo da skin: {@code classic} ou {@code slim}. */
	public String skinModel = "classic";
	/** Xbox user id (enviado ao jogo nas versões novas). */
	public String xuid;

	/** @return {@code msa} ou {@code legacy}, como o jogo espera em {@code --userType} */
	public String userType() {
		return type == Type.MICROSOFT ? "msa" : "legacy";
	}

	/** @return se o token do Minecraft ainda vale por pelo menos 5 minutos */
	public boolean tokenValid() {
		return type == Type.OFFLINE || System.currentTimeMillis() < expiresAt - 5 * 60_000L;
	}

	/** @return texto para listas, ex.: {@code Steve (Microsoft)} */
	public String label() {
		return username + (type == Type.MICROSOFT ? " (Microsoft)" : " (offline)");
	}

	@Override
	public String toString() {
		return label();
	}
}
