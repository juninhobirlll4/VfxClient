package com.vfxclient.core.module.general;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.NumberSetting;

/**
 * Estilo global da HUD do VfxClient: modo compacto (só o essencial) ou avançado (mais
 * informações), cores, cantos e espaçamento. Vale para todos os elementos da HUD.
 */
public class HudStyleModule extends Module {

	/** Modo compacto: valores curtos, sem rótulos. */
	public static final int COMPACT = 0;
	/** Modo avançado: rótulos e informações extras. */
	public static final int ADVANCED = 1;

	private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Modo", COMPACT, "Compacto", "Avançado"));
	private final ColorSetting textColor = add(new ColorSetting("textColor", "Cor do texto", 0xFFF2F2F2));
	private final ColorSetting accentColor = addAdvanced(new ColorSetting("accentColor", "Cor de destaque", 0xFFB48CFF));
	private final ColorSetting backgroundColor = add(new ColorSetting("backgroundColor", "Cor do fundo", 0x590E0A16));
	private final BooleanSetting rounded = add(new BooleanSetting("rounded", "Cantos arredondados", true));
	private final NumberSetting padding = addAdvanced(new NumberSetting("padding", "Espaço interno", 3, 1, 8, 1, " px"));
	private final BooleanSetting shadow = addAdvanced(new BooleanSetting("shadow", "Sombra no texto", false));

	public HudStyleModule() {
		super("hudStyle", "Estilo da HUD", "Compacto ou avançado, cores e cantos", Category.GENERAL, true);
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	/** @return {@code true} no modo avançado */
	public boolean advanced() {
		return mode.get() == ADVANCED;
	}

	/** @return cor padrão do texto (ARGB) */
	public int textColor() {
		return textColor.argb();
	}

	/** @return cor de destaque (rótulos, cor da marca) em ARGB */
	public int accentColor() {
		return accentColor.argb();
	}

	/** @return cor de fundo dos elementos (ARGB, com transparência) */
	public int backgroundColor() {
		return backgroundColor.argb();
	}

	/** @return raio dos cantos do fundo */
	public float radius() {
		return rounded.get() ? 3F : 0F;
	}

	/** @return espaço interno entre o texto e a borda do fundo */
	public int padding() {
		return padding.getInt();
	}

	/** @return se o texto da HUD usa sombra */
	public boolean shadow() {
		return shadow.get();
	}
}
