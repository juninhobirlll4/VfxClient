package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.ParticleLimitModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Descarta parte das partículas novas quando o limite está ligado. */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

	@Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
	private void vfx$limitParticles(Particle particle, CallbackInfo ci) {
		ParticleLimitModule module = VfxCore.module(ParticleLimitModule.class);
		if (module != null && module.shouldDrop(particle)) {
			ci.cancel();
		}
	}
}
