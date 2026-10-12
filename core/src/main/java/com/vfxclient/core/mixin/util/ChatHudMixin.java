package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.ChatModule;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Horário e agrupamento de mensagens repetidas.
 *
 * <p>Toda mensagem comum (id 0) é reenviada com um id próprio; quando ela se repete,
 * o mesmo id faz o jogo apagar a linha anterior e colocar a nova com o contador.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

	@Shadow
	public abstract void addMessage(Text message, int messageId);

	@Inject(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"), cancellable = true)
	private void vfx$processMessage(Text message, int messageId, CallbackInfo ci) {
		if (messageId != 0) {
			return;
		}
		ChatModule module = VfxCore.module(ChatModule.class);
		if (module == null || !module.isEnabled()) {
			return;
		}
		ChatModule.Result result = module.process(message);
		ci.cancel();
		addMessage(result.text, result.id);
	}
}
