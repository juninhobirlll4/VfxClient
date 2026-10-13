package com.vfxclient.launcher.optimization;

import java.util.ArrayList;
import java.util.List;

import com.vfxclient.launcher.profiles.Profile;

/**
 * Argumentos da JVM do jogo: memória e coletor de lixo ajustados para menos travadas.
 *
 * <p>G1 com pausas curtas é o que o launcher oficial usa; somamos {@code DisableExplicitGC}
 * (alguns mods chamam {@code System.gc()} e causam engasgos) e um heap inicial menor que o
 * máximo para não reservar memória à toa.</p>
 */
public final class JvmFlags {

	private JvmFlags() {
	}

	/**
	 * @param profile perfil (RAM e argumentos extras)
	 * @return argumentos na ordem
	 */
	public static List<String> forProfile(Profile profile) {
		int max = Math.max(512, profile.ramMb);
		int initial = Math.max(512, Math.min(max, max / 2));
		List<String> args = new ArrayList<>();
		args.add("-Xmx" + max + "M");
		args.add("-Xms" + initial + "M");
		args.add("-XX:+UseG1GC");
		args.add("-XX:+UnlockExperimentalVMOptions");
		args.add("-XX:G1NewSizePercent=20");
		args.add("-XX:G1ReservePercent=20");
		args.add("-XX:MaxGCPauseMillis=50");
		args.add("-XX:G1HeapRegionSize=32M");
		args.add("-XX:+DisableExplicitGC");
		args.add("-Dfile.encoding=UTF-8");
		if (profile.extraJvmArgs != null && !profile.extraJvmArgs.isBlank()) {
			for (String extra : profile.extraJvmArgs.trim().split("\\s+")) {
				args.add(extra);
			}
		}
		return args;
	}
}
