package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;

/** Não desenha as nuvens (ganho fácil de FPS em PCs fracos). */
public class NoCloudsModule extends Module {

	public NoCloudsModule() {
		super("noClouds", "Sem nuvens", "Desliga as nuvens", Category.PERFORMANCE, false);
	}
}
