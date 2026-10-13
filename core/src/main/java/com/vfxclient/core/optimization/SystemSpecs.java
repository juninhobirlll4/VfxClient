package com.vfxclient.core.optimization;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import org.lwjgl.opengl.GL11;

/**
 * Lê o hardware da máquina para decidir o preset de desempenho recomendado.
 *
 * <p>Os critérios são simples e conservadores: poucos núcleos, pouca RAM ou vídeo integrado
 * antigo indicam PC fraco.</p>
 */
public final class SystemSpecs {

	private static final long GB = 1024L * 1024L * 1024L;

	/** Recomendação: preset Baixo. */
	public static final int RECOMMEND_LOW = 1;
	/** Recomendação: preset Equilibrado. */
	public static final int RECOMMEND_BALANCED = 2;
	/** Recomendação: preset Alto. */
	public static final int RECOMMEND_HIGH = 3;

	private SystemSpecs() {
	}

	/** @return núcleos lógicos do processador */
	public static int cores() {
		return Runtime.getRuntime().availableProcessors();
	}

	/** @return RAM que o Java pode usar (definida pelo launcher), em bytes */
	public static long javaMaxMemory() {
		return Runtime.getRuntime().maxMemory();
	}

	/** @return RAM física total do computador, em bytes ({@code -1} se não der para ler) */
	public static long physicalMemory() {
		OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
		if (bean instanceof com.sun.management.OperatingSystemMXBean) {
			return ((com.sun.management.OperatingSystemMXBean) bean).getTotalPhysicalMemorySize();
		}
		return -1L;
	}

	/**
	 * Nome da placa de vídeo. Precisa ser chamado na thread do jogo (com o OpenGL pronto).
	 *
	 * @return texto do driver, ex.: {@code "Intel(R) UHD Graphics 620"}
	 */
	public static String gpu() {
		try {
			String renderer = GL11.glGetString(GL11.GL_RENDERER);
			return renderer == null ? "desconhecida" : renderer;
		} catch (RuntimeException e) {
			return "desconhecida";
		}
	}

	/** @return {@code true} se a placa parece ser um vídeo integrado antigo */
	public static boolean integratedGpu() {
		String gpu = gpu().toLowerCase();
		return (gpu.contains("intel") && !gpu.contains("arc")) || gpu.contains("gdi generic") || gpu.contains("microsoft basic");
	}

	/** @return preset recomendado ({@link #RECOMMEND_LOW}, {@link #RECOMMEND_BALANCED} ou {@link #RECOMMEND_HIGH}) */
	public static int recommendedPreset() {
		long ram = physicalMemory();
		boolean lowRam = ram > 0 && ram < 6 * GB;
		if (cores() <= 2 || (ram > 0 && ram < 4 * GB) || javaMaxMemory() < GB) {
			return RECOMMEND_LOW;
		}
		if (cores() <= 4 || lowRam || integratedGpu()) {
			return RECOMMEND_BALANCED;
		}
		return RECOMMEND_HIGH;
	}

	/** @return resumo em uma linha para mostrar ao jogador */
	public static String summary() {
		long ram = physicalMemory();
		String ramText = ram > 0 ? String.format("%.1f GB", ram / (double) GB) : "?";
		return cores() + " núcleos, " + ramText + " de RAM (jogo: "
				+ String.format("%.1f GB", javaMaxMemory() / (double) GB) + "), vídeo: " + gpu();
	}
}
