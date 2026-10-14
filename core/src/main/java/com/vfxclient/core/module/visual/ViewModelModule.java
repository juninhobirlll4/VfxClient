package com.vfxclient.core.module.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;

/** Item na mão: tamanho e posição do item que você segura (primeira pessoa). */
public class ViewModelModule extends Module {

	private final NumberSetting size = add(new NumberSetting("size", "Tamanho", 1.0, 0.4, 1.5, 0.05, "x"));
	private final NumberSetting x = add(new NumberSetting("x", "Para o lado", 0, -1, 1, 0.05, ""));
	private final NumberSetting y = add(new NumberSetting("y", "Altura", 0, -1, 1, 0.05, ""));
	private final NumberSetting z = add(new NumberSetting("z", "Distância", 0, -1, 1, 0.05, ""));

	public ViewModelModule() {
		super("viewModel", "Item na mão", "Tamanho e posição do item segurado", Category.VISUAL, false);
	}

	/** Aplica o deslocamento e a escala (chamado depois do posicionamento do jogo). */
	public void apply() {
		if (!isEnabled()) {
			return;
		}
		GlStateManager.translate(x.get().floatValue(), y.get().floatValue(), z.get().floatValue());
		float s = size.get().floatValue();
		GlStateManager.scale(s, s, s);
	}
}