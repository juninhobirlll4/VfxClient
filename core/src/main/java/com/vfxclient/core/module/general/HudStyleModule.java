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
 *
 * <p>Padrão: fundo preto translúcido (45%), cantos arredondados, texto claro e os valores
 * na cor de destaque. Módulos secundários usam um tom mais escuro da mesma cor.</p>
 */
public class HudStyleModule extends Module {

	/** Modo compacto: valores curtos, sem rótulos. */
	public static final int COMPACT = 0;
	/** Modo avançado: rótulos e informações extras. */
	public static final int ADVANCED = 1;

	private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Modo", COMPACT, "Compacto", "Avançado"));
	private final ColorSetting textColor = add(new ColorSetting("textColor", "Cor do texto", 0xFFF2F2F2));
	private final ColorSetting accentColor = add(new ColorSetting("accentColor", "Cor principal (destaque)", 0xFFB48CFF));
	private final BooleanSetting highlightValues = add(new BooleanSetting("highlightValues", "Valores na cor principal", true));
	private final NumberSetting secondaryShade = addAdvanced(new NumberSetting("secondaryShade", "Cor secundária (mais escura)", 30, 0, 70, 5, "%"));
	private final ColorSetting backgroundColor = add(new ColorSetting("backgroundColor", "Cor do fundo", 0x73000000));
	private final NumberSetting radius = add(new NumberSetting("radius", "Arredondamento dos cantos", 6, 0, 10, 1, " px"));
	private final NumberSetting padding = addAdvanced(new NumberSetting("padding", "Espaço interno", 4, 1, 8, 1, " px"));
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

	/** @return cor principal (valores, cor da marca) em ARGB */
	public int accentColor() {
		return accentColor.argb();
	}

	/** @return cor principal escurecida, para módulos secundários */
	public int secondaryColor() {
		int argb = accentColor.argb();
		float keep = 1F - secondaryShade.get().floatValue() / 100F;
		int r = Math.round(((argb >> 16) & 0xFF) * keep);
		int g = Math.round(((argb >> 8) & 0xFF) * keep);
		int b = Math.round((argb & 0xFF) * keep);
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	/** @return se os valores (números) aparecem na cor principal */
	public boolean highlightValues() {
		return highlightValues.get();
	}

	/** @return cor de fundo dos elementos (ARGB, com transparência) */
	public int backgroundColor() {
		return backgroundColor.argb();
	}

	/** @return raio dos cantos do fundo */
	public float radius() {
		return radius.get().floatValue();
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
