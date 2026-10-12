package com.vfxclient.core.module.performance;

import java.util.Random;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.particle.BlockParticle;
import net.minecraft.client.particle.CloudParticle;
import net.minecraft.client.particle.DamageParticle;
import net.minecraft.client.particle.EnchantGlyphParticle;
import net.minecraft.client.particle.ExplosionEmitterParticle;
import net.minecraft.client.particle.ExplosionSmokeParticle;
import net.minecraft.client.particle.FireSmokeParticle;
import net.minecraft.client.particle.FootstepParticle;
import net.minecraft.client.particle.LargeExplosionParticle;
import net.minecraft.client.particle.LargeFireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.RainSplashParticle;
import net.minecraft.client.particle.SpellParticle;
import net.minecraft.client.particle.WaterSplashParticle;

/** Reduz a quantidade de partículas e permite desligar cada tipo separadamente. */
public class ParticleLimitModule extends Module {

	private final NumberSetting amount = add(new NumberSetting("amount", "Quantidade geral", 50, 0, 100, 5, "%"));
	private final BooleanSetting explosions = add(new BooleanSetting("explosions", "Explosões", true));
	private final BooleanSetting crits = add(new BooleanSetting("crits", "Críticos e golpes", true));
	private final BooleanSetting potions = add(new BooleanSetting("potions", "Poções", true));
	private final BooleanSetting smoke = add(new BooleanSetting("smoke", "Fumaça e nuvens", true));
	private final BooleanSetting blocks = add(new BooleanSetting("blocks", "Quebra de blocos", true));
	private final BooleanSetting water = add(new BooleanSetting("water", "Chuva e respingos", true));
	private final BooleanSetting magic = add(new BooleanSetting("magic", "Portal e encantamento", true));
	private final BooleanSetting footsteps = add(new BooleanSetting("footsteps", "Pegadas", true));

	private final Random random = new Random();

	public ParticleLimitModule() {
		super("particleLimit", "Partículas", "Menos partículas, mais FPS", Category.PERFORMANCE, false);
	}

	/**
	 * @param particle partícula que o jogo vai criar
	 * @return {@code true} se ela deve ser descartada
	 */
	public boolean shouldDrop(Particle particle) {
		if (!isEnabled()) {
			return false;
		}
		if (!typeAllowed(particle)) {
			return true;
		}
		return random.nextDouble() * 100 >= amount.get();
	}

	private boolean typeAllowed(Particle p) {
		if (p instanceof LargeExplosionParticle || p instanceof ExplosionSmokeParticle || p instanceof ExplosionEmitterParticle) {
			return explosions.get();
		}
		if (p instanceof DamageParticle) {
			return crits.get();
		}
		if (p instanceof SpellParticle) {
			return potions.get();
		}
		if (p instanceof FireSmokeParticle || p instanceof LargeFireSmokeParticle || p instanceof CloudParticle) {
			return smoke.get();
		}
		if (p instanceof BlockDustParticle || p instanceof BlockParticle) {
			return blocks.get();
		}
		if (p instanceof RainSplashParticle || p instanceof WaterSplashParticle) {
			return water.get();
		}
		if (p instanceof PortalParticle || p instanceof EnchantGlyphParticle) {
			return magic.get();
		}
		if (p instanceof FootstepParticle) {
			return footsteps.get();
		}
		return true;
	}
}
