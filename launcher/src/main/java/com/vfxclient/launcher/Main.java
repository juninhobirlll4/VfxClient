package com.vfxclient.launcher;

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
		VfxLauncherApp.launchApp(args);
	}
}
