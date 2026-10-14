package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.general.LocalSkinModule;
import com.vfxclient.core.skin.RemoteSkins;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aplica a skin escolhida no launcher no próprio jogador e a skin dos outros jogadores que usam o
 * VfxClient (baixada do servidor do VfxClient).
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class SkinMixin {

	/**
	 * No mapeamento Yarn do 1.8.9 os nomes estão trocados: {@code getCapeId()} devolve a textura da
	 * SKIN (é a que o desenho do jogador usa) e {@code getSkinId()} devolve a da capa.
	 */
	@Inject(method = "getCapeId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
	private void vfx$localSkin(CallbackInfoReturnable<Identifier> cir) {
		LocalSkinModule module = VfxCore.module(LocalSkinModule.class);
		if (module == null) {
			return;
		}
		AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
		Identifier texture = module.textureFor(player.getUuid());
		if (texture == null && module.showOthers()) {
			texture = RemoteSkins.textureFor(player);
		}
		if (texture != null) {
			cir.setReturnValue(texture);
		}
	}

	@Inject(method = "getModel()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
	private void vfx$localModel(CallbackInfoReturnable<String> cir) {
		LocalSkinModule module = VfxCore.module(LocalSkinModule.class);
		if (module == null) {
			return;
		}
		AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
		if (module.textureFor(player.getUuid()) != null) {
			cir.setReturnValue(module.modelName());
		} else if (module.showOthers() && RemoteSkins.textureFor(player) != null) {
			cir.setReturnValue(RemoteSkins.modelFor(player));
		}
	}
}
