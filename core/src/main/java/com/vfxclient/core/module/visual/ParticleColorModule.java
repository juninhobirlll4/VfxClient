package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import net.minecraft.client.particle.Particle;

/** Cor das partículas de golpe: crítico e crítico de encantamento (afiada) na cor que você quiser. */
public class ParticleColorModule extends Module {

	/** Ids das partículas no 1.8.9. */
	public static final int CRIT = 9;
	public static final int CRIT_MAGIC = 10;

	private final BooleanSetting crit = add(new BooleanSetting("crit", "Mudar crítico", true));
	private final ColorSetting critColor = add(new ColorSetting("critColor", "Cor do crítico", 0xFFFF5FA2));
	private final BooleanSetting magic = add(new BooleanSetting("magic", "Mudar crítico de encantamento", true));
	private final ColorSetting magicColor = add(new ColorSetting("magicColor", "Cor do encantamento", 0xFFB48CFF));

	public ParticleColorModule() {
		super("particleColor", "Cor das partículas", "Crítico e encantamento na sua cor", Category.VISUAL, false);
	}

	/**
	 * Pinta a partícula recém-criada, se for de golpe.
	 *
	 * @param id       id da partícula
	 * @param particle partícula
	 */
	public void apply(int id, Particle particle) {
		if (!isEnabled() || particle == null) {
			return;
		}
		int argb;
		if (id == CRIT && crit.get()) {
			argb = critColor.argb();
		} else if (id == CRIT_MAGIC && magic.get()) {
			argb = magicColor.argb();
		} else {
			return;
		}
		// Varia um pouco o brilho de cada partícula, como no jogo.
		float shade = 0.75F + (float) Math.random() * 0.25F;
		particle.setColor(((argb >> 16) & 0xFF) / 255F * shade, ((argb >> 8) & 0xFF) / 255F * shade, (argb & 0xFF) / 255F * shade);
	}
}