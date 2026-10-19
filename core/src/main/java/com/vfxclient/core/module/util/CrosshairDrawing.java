package com.vfxclient.core.module.util;

import java.util.Arrays;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.DrawableHelper;

/**
 * Mira desenhada pelo jogador: uma grade de 17x17 quadradinhos, guardada como texto (uma letra
 * por quadradinho, linha por linha, de cima para baixo).
 *
 * <p>Cada letra é uma cor da {@link #PALETTE}: {@code 0} vazio, {@code 1} a cor da mira, {@code 2}
 * a cor do contorno e as outras são cores fixas.</p>
 */
public final class CrosshairDrawing {

	/** Lado da grade, em quadradinhos (ímpar, para ter um quadradinho exatamente no centro). */
	public static final int SIDE = 17;
	/** Quantidade de quadradinhos. */
	public static final int CELLS = SIDE * SIDE;
	/** Índice do quadradinho do meio, nas duas direções. */
	public static final int CENTER = SIDE / 2;

	/** Letra de cada cor, na ordem do editor. */
	public static final String CODES = "0123456789ab";
	/** Nome de cada cor ({@code null} em {@code 0}, que é "vazio"). */
	public static final String[] NAMES = {null, "Cor da mira", "Contorno", "Branco", "Preto", "Vermelho", "Verde",
		"Azul", "Amarelo", "Ciano", "Magenta", "Laranja"};

	private static final int[] FIXED = {
		0, 0, 0, 0xFFFFFFFF, 0xFF000000, 0xFFFF3B3B, 0xFF3BFF6A, 0xFF3B8BFF, 0xFFFFE53B, 0xFF3BFFF0, 0xFFFF3BE6, 0xFFFF9A3B
	};

	/** Nomes das predefinições do editor. */
	public static final String[] PRESETS = {"Cruz", "Ponto", "Círculo", "T", "Pontilhada", "Alvo"};

	private CrosshairDrawing() {
	}

	/** @return texto de um desenho vazio */
	public static String blank() {
		return repeat('0', CELLS);
	}

	/** @return o texto com exatamente {@link #CELLS} letras válidas (o que faltar ou sobrar vira vazio) */
	public static String normalize(String cells) {
		char[] grid = new char[CELLS];
		for (int i = 0; i < CELLS; i++) {
			char c = cells != null && i < cells.length() ? cells.charAt(i) : '0';
			grid[i] = CODES.indexOf(c) >= 0 ? c : '0';
		}
		return new String(grid);
	}

	/**
	 * @param code letra do quadradinho
	 * @param main cor da mira
	 * @param outline cor do contorno
	 * @return a cor ARGB, ou 0 se estiver vazio
	 */
	public static int argb(char code, int main, int outline) {
		int index = CODES.indexOf(code);
		if (index <= 0) {
			return 0;
		}
		return index == 1 ? main : index == 2 ? outline : FIXED[index];
	}

	/**
	 * Desenha o desenho com o quadradinho do meio no centro da mira.
	 *
	 * @param cells   texto do desenho
	 * @param cx      centro (x)
	 * @param cy      centro (y)
	 * @param scale   lado de cada quadradinho, em pixels da tela
	 * @param main    cor da mira
	 * @param outline cor do contorno
	 */
	public static void draw(String cells, int cx, int cy, int scale, IntSupplier main, IntSupplier outline) {
		int mainColor = main.getAsInt();
		int outlineColor = outline.getAsInt();
		int left = cx - CENTER * scale;
		int top = cy - CENTER * scale;

		for (int row = 0; row < SIDE; row++) {
			int column = 0;
			while (column < SIDE) {
				char code = cells.charAt(row * SIDE + column);
				int end = column + 1;
				while (end < SIDE && cells.charAt(row * SIDE + end) == code) {
					end++;
				}
				int color = argb(code, mainColor, outlineColor);
				if (color != 0) {
					DrawableHelper.fill(left + column * scale, top + row * scale, left + end * scale, top + (row + 1) * scale, color);
				}
				column = end;
			}
		}
	}

	/**
	 * @param name nome de {@link #PRESETS}
	 * @return o desenho pronto (com contorno)
	 */
	public static String preset(String name) {
		char[] grid = blank().toCharArray();
		switch (name) {
			case "Cruz":
				arms(grid, 3, 7, true, true, true, true);
				break;
			case "Ponto":
				block(grid, CENTER - 1, CENTER - 1, 3);
				break;
			case "Círculo":
				ring(grid, 6);
				break;
			case "T":
				arms(grid, 3, 7, false, true, true, true);
				break;
			case "Pontilhada":
				for (int d : new int[] {3, 6}) {
					set(grid, CENTER, CENTER - d, '1');
					set(grid, CENTER, CENTER + d, '1');
					set(grid, CENTER - d, CENTER, '1');
					set(grid, CENTER + d, CENTER, '1');
				}
				set(grid, CENTER, CENTER, '1');
				break;
			case "Alvo":
				ring(grid, 6);
				set(grid, CENTER, CENTER, '1');
				break;
			default:
				break;
		}
		return new String(withOutline(grid));
	}

	/** Quatro braços (cima, baixo, esquerda, direita) a partir de {@code from} até {@code to} do centro. */
	private static void arms(char[] grid, int from, int to, boolean up, boolean down, boolean left, boolean right) {
		for (int d = from; d <= to; d++) {
			if (up) {
				set(grid, CENTER, CENTER - d, '1');
			}
			if (down) {
				set(grid, CENTER, CENTER + d, '1');
			}
			if (left) {
				set(grid, CENTER - d, CENTER, '1');
			}
			if (right) {
				set(grid, CENTER + d, CENTER, '1');
			}
		}
	}

	private static void block(char[] grid, int x, int y, int side) {
		for (int dy = 0; dy < side; dy++) {
			for (int dx = 0; dx < side; dx++) {
				set(grid, x + dx, y + dy, '1');
			}
		}
	}

	private static void ring(char[] grid, int radius) {
		for (int y = 0; y < SIDE; y++) {
			for (int x = 0; x < SIDE; x++) {
				double distance = Math.hypot(x - CENTER, y - CENTER);
				if (Math.abs(distance - radius) < 0.6) {
					set(grid, x, y, '1');
				}
			}
		}
	}

	/** Põe a cor do contorno em volta de tudo que está pintado (nos quadradinhos vazios vizinhos). */
	private static char[] withOutline(char[] grid) {
		char[] result = grid.clone();
		for (int y = 0; y < SIDE; y++) {
			for (int x = 0; x < SIDE; x++) {
				if (grid[y * SIDE + x] != '0') {
					continue;
				}
				if (touchesPainted(grid, x, y)) {
					result[y * SIDE + x] = '2';
				}
			}
		}
		return result;
	}

	private static boolean touchesPainted(char[] grid, int x, int y) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				int nx = x + dx;
				int ny = y + dy;
				if (nx >= 0 && ny >= 0 && nx < SIDE && ny < SIDE && grid[ny * SIDE + nx] == '1') {
					return true;
				}
			}
		}
		return false;
	}

	private static void set(char[] grid, int x, int y, char code) {
		if (x >= 0 && y >= 0 && x < SIDE && y < SIDE) {
			grid[y * SIDE + x] = code;
		}
	}

	private static String repeat(char c, int count) {
		char[] chars = new char[count];
		Arrays.fill(chars, c);
		return new String(chars);
	}
}
