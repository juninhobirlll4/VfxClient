package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import net.minecraft.item.ItemStack;

/**
 * Tooltips com rolagem: no inventário, a rodinha do mouse move a descrição do item para cima
 * e para baixo (com Shift, para os lados). Útil em itens com descrição enorme.
 */
public class ScrollTooltipsModule extends Module {

	private static final int STEP = 12;

	private ItemStack current;
	private int offsetX;
	private int offsetY;

	public ScrollTooltipsModule() {
		super("scrollTooltips", "Tooltips com rolagem", "Role descrições longas com a rodinha", Category.UTILITY, true);
	}

	/**
	 * Chamado quando o jogo vai mostrar o tooltip de um item; outro item zera a rolagem.
	 *
	 * @param stack item sob o mouse
	 */
	public void onItemTooltip(ItemStack stack) {
		if (stack != current) {
			current = stack;
			offsetX = 0;
			offsetY = 0;
		}
	}

	/**
	 * @param wheel valor da rodinha (positivo = para cima)
	 * @param shift Shift apertado (rola para os lados)
	 */
	public void scroll(int wheel, boolean shift) {
		if (!isEnabled() || current == null || wheel == 0) {
			return;
		}
		int delta = wheel > 0 ? STEP : -STEP;
		if (shift) {
			offsetX += delta;
		} else {
			offsetY += delta;
		}
	}

	/** @return deslocamento horizontal do tooltip */
	public int offsetX() {
		return isEnabled() ? offsetX : 0;
	}

	/** @return deslocamento vertical do tooltip */
	public int offsetY() {
		return isEnabled() ? offsetY : 0;
	}

	/** Mouse saiu de cima dos itens: esquece o item e a rolagem. */
	public void reset() {
		current = null;
		offsetX = 0;
		offsetY = 0;
	}
}