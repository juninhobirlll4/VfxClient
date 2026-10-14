package com.vfxclient.launcher;

import java.util.Arrays;

import com.vfxclient.launcher.cli.Cli;

/**
 * Ponto de entrada do executável.
 *
 * <p>Esta classe propositalmente não estende {@link javafx.application.Application}:
 * quando a classe principal estende Application e o JavaFX está no classpath (e não
 * no module path), o Java recusa iniciar. Delegar para {@link VfxLauncherApp} evita isso.</p>
 */
public final class Main {

	private Main() {
	}

	/**
	 * Inicia o launcher.
	 *
	 * @param args argumentos de linha de comando, repassados ao JavaFX
	 */
	public static void main(String[] args) {
		if (args.length > 0 && args[0].equals("--cli")) {
			System.exit(Cli.run(Arrays.copyOfRange(args, 1, args.length)));
			return;
		}
		VfxLauncherApp.launchApp(args);
	}
}
