package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.general.LocalSkinModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aplica a skin local (escolhida no launcher) no próprio jogador. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class SkinMixin {

	@Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
	private void vfx$localSkin(CallbackInfoReturnable<Identifier> cir) {
		LocalSkinModule module = VfxCore.module(LocalSkinModule.class);
		if (module == null) {
			return;
		}
		Identifier texture = module.textureFor(((AbstractClientPlayerEntity) (Object) this).getUuid());
		if (texture != null) {
			cir.setReturnValue(texture);
		}
	}

	@Inject(method = "getModel()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
	private void vfx$localModel(CallbackInfoReturnable<String> cir) {
		LocalSkinModule module = VfxCore.module(LocalSkinModule.class);
		if (module != null && module.textureFor(((AbstractClientPlayerEntity) (Object) this).getUuid()) != null) {
			cir.setReturnValue(module.modelName());
		}
	}
}
