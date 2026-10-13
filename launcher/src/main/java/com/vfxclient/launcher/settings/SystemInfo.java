package com.vfxclient.launcher.settings;

import java.lang.management.ManagementFactory;

/** RAM e processador do computador, e a RAM recomendada para o jogo. */
public final class SystemInfo {

	private SystemInfo() {
	}

	private static com.sun.management.OperatingSystemMXBean os() {
		return (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	}

	/** @return RAM total do computador, em MB */
	public static int totalRamMb() {
		return (int) (os().getTotalMemorySize() / 1048576L);
	}

	/** @return RAM livre agora, em MB */
	public static int freeRamMb() {
		return (int) (os().getFreeMemorySize() / 1048576L);
	}

	/** @return núcleos lógicos */
	public static int cores() {
		return Runtime.getRuntime().availableProcessors();
	}

	/**
	 * RAM recomendada para o Minecraft 1.8.9 com o VfxClient: o suficiente para rodar liso,
	 * sem roubar memória do Windows (mais RAM que o necessário só deixa as pausas do GC maiores).
	 *
	 * @return MB recomendados
	 */
	public static int recommendedRamMb() {
		int total = totalRamMb();
		if (total >= 16000) {
			return 4096;
		}
		if (total >= 8000) {
			return 3072;
		}
		if (total >= 6000) {
			return 2048;
		}
		return 1536;
	}

	/** @return máximo seguro: o total menos 2 GB para o Windows (mínimo 1 GB) */
	public static int maxSafeRamMb() {
		return Math.max(1024, totalRamMb() - 2048);
	}
}
