package com.vfxclient.launcher.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

/** Animações curtas e suaves usadas em todo o launcher. */
public final class Motion {

	private static final Interpolator EASE = Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0);
	/** No modo de prints de revisão ({@code -Dvfx.snapshot}) tudo aparece direto, sem animação. */
	public static final boolean ENABLED = System.getProperty("vfx.snapshot") == null;

	private Motion() {
	}

	/**
	 * Entrada: aparece subindo um pouco.
	 *
	 * @param node    elemento
	 * @param delayMs atraso antes de começar (para entradas em sequência)
	 */
	public static void enter(Node node, int delayMs) {
		if (!ENABLED) {
			return;
		}
		node.setOpacity(0);
		node.setTranslateY(14);
		FadeTransition fade = new FadeTransition(Duration.millis(320), node);
		fade.setToValue(1);
		fade.setInterpolator(EASE);
		TranslateTransition move = new TranslateTransition(Duration.millis(320), node);
		move.setToY(0);
		move.setInterpolator(EASE);
		ParallelTransition both = new ParallelTransition(fade, move);
		both.setDelay(Duration.millis(delayMs));
		both.play();
	}

	/**
	 * Botão que cresce levemente com o mouse em cima.
	 *
	 * @param node  elemento
	 * @param scale escala no hover (ex.: 1.03)
	 */
	public static void hoverGrow(Node node, double scale) {
		ScaleTransition grow = new ScaleTransition(Duration.millis(140), node);
		node.setOnMouseEntered(e -> {
			grow.stop();
			grow.setToX(scale);
			grow.setToY(scale);
			grow.playFromStart();
		});
		node.setOnMouseExited(e -> {
			grow.stop();
			grow.setToX(1);
			grow.setToY(1);
			grow.playFromStart();
		});
	}

	/**
	 * Mostra uma mensagem que some sozinha depois de alguns segundos.
	 *
	 * @param label  onde mostrar
	 * @param text   mensagem
	 */
	public static void toast(Label label, String text) {
		label.setText(text);
		label.setOpacity(0);
		FadeTransition in = new FadeTransition(Duration.millis(200), label);
		in.setToValue(1);
		PauseTransition hold = new PauseTransition(Duration.seconds(4));
		FadeTransition out = new FadeTransition(Duration.millis(500), label);
		out.setToValue(0);
		in.setOnFinished(e -> hold.play());
		hold.setOnFinished(e -> out.play());
		in.play();
	}

	/**
	 * Barra de progresso que desliza até o valor novo em vez de pular.
	 *
	 * @param bar   barra
	 * @param value valor de 0 a 1, ou negativo para "carregando"
	 */
	public static void progress(ProgressBar bar, double value) {
		if (value < 0 || bar.getProgress() < 0 || value < bar.getProgress()) {
			bar.setProgress(value < 0 ? ProgressBar.INDETERMINATE_PROGRESS : value);
			return;
		}
		new Timeline(new KeyFrame(Duration.millis(220), new KeyValue(bar.progressProperty(), value, EASE))).play();
	}
}
