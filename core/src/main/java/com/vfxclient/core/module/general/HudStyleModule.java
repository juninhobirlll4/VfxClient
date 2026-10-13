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
 * <p>Padrão (v0.7): visual plano, fundo preto translúcido (35%), cantos retos, texto branco
 * com sombra. Dá para ligar cantos arredondados e valores na cor de destaque.</p>
 */
public class HudStyleModule extends Module {

	/** Modo compacto: valores curtos, sem rótulos. */
	public static final int COMPACT = 0;
	/** Modo avançado: rótulos e informações extras. */
	public static final int ADVANCED = 1;

	private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Modo", COMPACT, "Compacto", "Avançado"));
	private final ColorSetting textColor = add(new ColorSetting("textColor", "Cor do texto", 0xFFFFFFFF));
	private final ColorSetting accentColor = add(new ColorSetting("accentColor", "Cor principal (destaque)", 0xFFB48CFF));
	private final BooleanSetting highlightValues = add(new BooleanSetting("highlightValues", "Valores na cor principal", false));
	private final NumberSetting secondaryShade = addAdvanced(new NumberSetting("secondaryShade", "Cor secundária (mais escura)", 20, 0, 70, 5, "%"));
	private final ColorSetting backgroundColor = add(new ColorSetting("backgroundColor", "Cor do fundo", 0x59000000));
	private final NumberSetting radius = add(new NumberSetting("radius", "Arredondamento dos cantos", 0, 0, 10, 1, " px"));
	private final NumberSetting padding = addAdvanced(new NumberSetting("padding", "Espaço interno", 4, 1, 8, 1, " px"));
	private final BooleanSetting shadow = addAdvanced(new BooleanSetting("shadow", "Sombra no texto", true));
	private final ChoiceSetting refresh = addAdvanced(new ChoiceSetting("refresh", "Atualização do texto (cache)", 1,
			"Todo quadro", "20 vezes por segundo", "10 vezes por segundo"));

	/** Versão do visual padrão; configs mais antigas são trocadas para o visual novo uma vez. */
	private static final int STYLE_VERSION = 2;
	private boolean migrate;

	public HudStyleModule() {
		super("hudStyle", "Estilo da HUD", "Compacto ou avançado, cores e cantos", Category.GENERAL, true);
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	@Override
	public com.google.gson.JsonObject toJson() {
		com.google.gson.JsonObject json = super.toJson();
		json.addProperty("styleVersion", STYLE_VERSION);
		return json;
	}

	@Override
	public void fromJson(com.google.gson.JsonObject json) {
		super.fromJson(json);
		int saved = readInt(json, "styleVersion", 1);
		if (saved < STYLE_VERSION) {
			migrate = true;
		}
	}

	@Override
	public void onTick(net.minecraft.client.MinecraftClient client) {
		if (migrate) {
			migrate = false;
			applyFlatLook();
		}
	}

	/** Troca as cores e formatos da HUD para o visual plano padrão (mantém posições e o que está ligado). */
	public void applyFlatLook() {
		String[] own = {"textColor", "highlightValues", "backgroundColor", "radius", "padding", "shadow"};
		for (String id : own) {
			getSetting(id).reset();
		}
		com.vfxclient.core.module.ModuleManager modules = com.vfxclient.core.VfxCore.get().getModules();
		resetSettings(modules.get("keystrokes"), "style", "bgColor", "textColor", "borderColor", "pressedColor", "pressedTextColor", "gap");
		resetSettings(modules.get("fps"), "rangeColor");
		resetSettings(modules.get("ping"), "qualityColor");
		markDirty();
	}

	private static void resetSettings(Module module, String... ids) {
		if (module == null) {
			return;
		}
		for (String id : ids) {
			com.vfxclient.core.module.setting.Setting<?> setting = module.getSetting(id);
			if (setting != null) {
				setting.reset();
			}
		}
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

	/** @return intervalo do cache do texto da HUD, em milissegundos (0 = sem cache) */
	public long cacheMillis() {
		return refresh.get() == 0 ? 0L : refresh.get() == 1 ? 50L : 100L;
	}

	/** @return se o texto da HUD usa sombra */
	public boolean shadow() {
		return shadow.get();
	}
}
