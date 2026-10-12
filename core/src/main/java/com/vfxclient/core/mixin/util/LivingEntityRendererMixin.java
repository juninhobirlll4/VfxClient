package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.HitColorModule;
import com.vfxclient.core.module.util.OwnNametagModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cor do hit e nome próprio no F5.
 *
 * <p>{@code method_10252} é onde o jogo monta a cor do "piscar vermelho": as quatro primeiras
 * chamadas de {@code FloatBuffer.put} são vermelho, verde, azul e intensidade do dano.</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

	@Unique
	private static final String PUT = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;";
	@Unique
	private static final String METHOD = "method_10252(Lnet/minecraft/entity/LivingEntity;FZ)Z";

	@Unique
	private static float vfx$hit(int index, float vanilla) {
		HitColorModule module = VfxCore.module(HitColorModule.class);
		return module == null ? vanilla : module.component(index, vanilla);
	}

	@ModifyArg(method = METHOD, at = @At(value = "INVOKE", target = PUT, ordinal = 0))
	private float vfx$hitRed(float value) {
		return vfx$hit(0, value);
	}

	@ModifyArg(method = METHOD, at = @At(value = "INVOKE", target = PUT, ordinal = 1))
	private float vfx$hitGreen(float value) {
		return vfx$hit(1, value);
	}

	@ModifyArg(method = METHOD, at = @At(value = "INVOKE", target = PUT, ordinal = 2))
	private float vfx$hitBlue(float value) {
		return vfx$hit(2, value);
	}

	@ModifyArg(method = METHOD, at = @At(value = "INVOKE", target = PUT, ordinal = 3))
	private float vfx$hitStrength(float value) {
		return vfx$hit(3, value);
	}

	@Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
	private void vfx$ownNametag(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (cir.getReturnValue() || entity != client.player || client.options.perspective == 0 || !MinecraftClient.isHudEnabled()) {
			return;
		}
		OwnNametagModule module = VfxCore.module(OwnNametagModule.class);
		if (module != null && module.isEnabled() && !entity.isInvisible()) {
			cir.setReturnValue(true);
		}
	}
}
