package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;

/**
 * Ajustes finos de renderização que trocam enfeite por FPS. Cada opção é independente.
 */
public class RenderTweaksModule extends Module {

	private final BooleanSetting animatedTextures = add(new BooleanSetting("animatedTextures", "Texturas animadas (todas)", true));
	private final BooleanSetting animWater = add(new BooleanSetting("animWater", "Animação da água", true));
	private final BooleanSetting animLava = add(new BooleanSetting("animLava", "Animação da lava", true));
	private final BooleanSetting animFire = add(new BooleanSetting("animFire", "Animação do fogo", true));
	private final BooleanSetting animPortal = add(new BooleanSetting("animPortal", "Animação de portais", true));
	private final BooleanSetting fog = add(new BooleanSetting("fog", "Névoa no horizonte", true));
	private final BooleanSetting vignette = add(new BooleanSetting("vignette", "Escurecer bordas da tela (vinheta)", true));
	private final BooleanSetting weather = add(new BooleanSetting("weather", "Chuva e neve", true));
	private final BooleanSetting sky = add(new BooleanSetting("sky", "Céu, sol, lua e estrelas", true));
	private final BooleanSetting glint = add(new BooleanSetting("glint", "Brilho de encantamento", true));
	private final BooleanSetting stackedItems = add(new BooleanSetting("stackedItems", "Item no chão como um só modelo", false));
	private final NumberSetting chunkBudget = add(new NumberSetting("chunkBudget", "Tempo para carregar chunks por frame", 0, 0, 10, 1, " ms"));

	public RenderTweaksModule() {
		super("renderTweaks", "Ajustes de render", "Troque enfeites por FPS", Category.PERFORMANCE, true);
	}

	/** @return se as texturas animadas devem ser atualizadas */
	public boolean animatedTextures() {
		return !isEnabled() || animatedTextures.get();
	}

	/**
	 * Animação por tipo de textura (o nome vem do atlas, ex.: {@code minecraft:blocks/water_still}).
	 *
	 * @param spriteName nome da textura animada
	 * @return se ela deve continuar animando
	 */
	public boolean animates(String spriteName) {
		if (!isEnabled() || spriteName == null) {
			return true;
		}
		if (spriteName.contains("water")) {
			return animWater.get();
		}
		if (spriteName.contains("lava")) {
			return animLava.get();
		}
		if (spriteName.contains("fire")) {
			return animFire.get();
		}
		if (spriteName.contains("portal")) {
			return animPortal.get();
		}
		return true;
	}

	/** @return se a névoa deve ser desenhada */
	public boolean fog() {
		return !isEnabled() || fog.get();
	}

	/** @return se a vinheta (bordas escuras) deve ser desenhada */
	public boolean vignette() {
		return !isEnabled() || vignette.get();
	}

	/** @return se chuva e neve devem aparecer */
	public boolean weather() {
		return !isEnabled() || weather.get();
	}

	/** @return se o céu deve ser desenhado */
	public boolean sky() {
		return !isEnabled() || sky.get();
	}

	/** @return se o brilho de encantamento deve ser desenhado */
	public boolean glint() {
		return !isEnabled() || glint.get();
	}

	/** @return se itens no chão devem ser desenhados como um só modelo */
	public boolean stackedItems() {
		return isEnabled() && stackedItems.get();
	}

	/**
	 * Limite de tempo (em nanossegundos) para enviar chunks à placa de vídeo por frame.
	 *
	 * @return {@code 0} para usar o padrão do jogo
	 */
	public long chunkBudgetNanos() {
		return isEnabled() ? chunkBudget.getInt() * 1_000_000L : 0L;
	}
}
