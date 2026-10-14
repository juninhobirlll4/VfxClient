package com.vfxclient.core.mixin.util;

import java.util.List;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.hud.ChatLines;
import com.vfxclient.core.module.util.AutoGgModule;
import com.vfxclient.core.module.util.ChatModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
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
public abstract class ChatHudMixin implements ChatLines {

	@Shadow
	@Final
	private List<ChatHudLine> visibleMessages;

	@Shadow
	@Final
	private List<ChatHudLine> messages;

	@Shadow
	private int scrolledLines;

	@Shadow
	public abstract boolean isChatFocused();

	@Shadow
	public abstract float getChatScale();

	@Shadow
	public abstract int getVisibleLineCount();

	@Shadow
	public abstract int getWidth();

	@Override
	public String vfx$messageAt(int rawX, int rawY) {
		if (!isChatFocused()) {
			return null;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		int factor = new Window(client).getScaleFactor();
		float scale = getChatScale();
		int x = MathHelper.floor((rawX / factor - 3) / scale);
		int y = MathHelper.floor((rawY / factor - 27) / scale);
		int lines = Math.min(getVisibleLineCount(), visibleMessages.size());
		int lineHeight = client.textRenderer.fontHeight;
		if (x < 0 || y < 0 || x > MathHelper.floor(getWidth() / scale) || y >= lineHeight * lines + lines) {
			return null;
		}

		int index = y / lineHeight + scrolledLines;
		if (index < 0 || index >= visibleMessages.size()) {
			return null;
		}

		ChatHudLine line = visibleMessages.get(index);
		Text text = line.getText();
		// Mensagem quebrada em várias linhas: pega a mensagem inteira pelo id e pelo tick.
		if (line.getId() != 0) {
			for (ChatHudLine full : messages) {
				if (full.getId() == line.getId() && full.getCreationTick() == line.getCreationTick()) {
					text = full.getText();
					break;
				}
			}
		}

		return text.asUnformattedString().replaceAll("\u00a7.", "").replaceFirst("^\\[\\d\\d:\\d\\d\\] ", "");
	}

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
		if (result != null) {
			addMessage(result.text, result.id);
		}
	}

	@ModifyConstant(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", constant = @Constant(intValue = 100))
	private int vfx$historySize(int vanilla) {
		ChatModule module = VfxCore.module(ChatModule.class);
		return module == null ? vanilla : module.historySize();
	}
}