package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.ParticleLimitModule;
import com.vfxclient.core.module.visual.ParticleColorModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Descarta parte das partículas novas quando o limite está ligado; pinta as partículas de golpe. */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

	@Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
	private void vfx$limitParticles(Particle particle, CallbackInfo ci) {
		ParticleLimitModule module = VfxCore.module(ParticleLimitModule.class);
		if (module != null && module.shouldDrop(particle)) {
			ci.cancel();
		}
	}

	@Redirect(method = "addParticle(IDDDDDD[I)Lnet/minecraft/client/particle/Particle;",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/particle/ParticleFactory;createParticle(ILnet/minecraft/world/World;DDDDDD[I)Lnet/minecraft/client/particle/Particle;"))
	private Particle vfx$particleColor(ParticleFactory factory, int id, World world, double x, double y, double z,
			double vx, double vy, double vz, int[] args) {
		Particle particle = factory.createParticle(id, world, x, y, z, vx, vy, vz, args);
		ParticleColorModule module = VfxCore.module(ParticleColorModule.class);
		if (module != null) {
			module.apply(id, particle);
		}
		return particle;
	}
}