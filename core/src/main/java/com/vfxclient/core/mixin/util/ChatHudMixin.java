package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.AutoGgModule;
import com.vfxclient.core.module.util.ChatModule;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Horário, agrupamento de mensagens repetidas, histórico maior e Auto GG.
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
		AutoGgModule autoGg = VfxCore.module(AutoGgModule.class);
		if (autoGg != null) {
			autoGg.onChat(message);
		}
		ChatModule module = VfxCore.module(ChatModule.class);
		if (module == null || !module.isEnabled()) {
			return;
		}
		ChatModule.Result result = module.process(message);
		ci.cancel();
		addMessage(result.text, result.id);
	}

	@ModifyConstant(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", constant = @Constant(intValue = 100))
	private int vfx$historySize(int vanilla) {
		ChatModule module = VfxCore.module(ChatModule.class);
		return module == null ? vanilla : module.historySize();
	}
}