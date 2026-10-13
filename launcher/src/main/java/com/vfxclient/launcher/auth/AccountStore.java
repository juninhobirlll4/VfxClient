package com.vfxclient.launcher.auth;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Json;

/**
 * Contas salvas em {@code launcher/accounts.json}.
 *
 * <p>Os tokens ficam só no seu computador, nessa pasta do usuário do Windows.</p>
 */
public final class AccountStore {

	private static final Path FILE = LauncherPaths.launcher().resolve("accounts.json");

	/** Formato do arquivo. */
	private static final class Data {
		List<Account> accounts = new ArrayList<>();
		String selected;
	}

	private Data data = new Data();

	/** Lê o arquivo (se não existir, começa vazio). */
	public void load() {
		try {
			Data loaded = Json.read(FILE, Data.class);
			if (loaded != null && loaded.accounts != null) {
				data = loaded;
			}
		} catch (IOException e) {
			data = new Data();
		}
	}

	/**
	 * Grava o arquivo.
	 *
	 * @throws IOException em erro de disco
	 */
	public void save() throws IOException {
		Json.write(FILE, data);
	}

	/** @return contas salvas */
	public List<Account> all() {
		return data.accounts;
	}

	/** @return conta escolhida, ou a primeira, ou {@code null} se não houver nenhuma */
	public Account selected() {
		for (Account account : data.accounts) {
			if (account.uuid.equals(data.selected)) {
				return account;
			}
		}
		return data.accounts.isEmpty() ? null : data.accounts.get(0);
	}

	/**
	 * @param account conta a usar
	 */
	public void select(Account account) {
		data.selected = account == null ? null : account.uuid;
	}

	/**
	 * Adiciona ou atualiza (mesmo UUID) uma conta e a seleciona.
	 *
	 * @param account conta
	 */
	public void put(Account account) {
		data.accounts.removeIf(a -> a.uuid.equals(account.uuid));
		data.accounts.add(account);
		select(account);
	}

	/**
	 * @param account conta a remover
	 */
	public void remove(Account account) {
		data.accounts.removeIf(a -> a.uuid.equals(account.uuid));
		if (account.uuid.equals(data.selected)) {
			data.selected = null;
		}
	}
}
