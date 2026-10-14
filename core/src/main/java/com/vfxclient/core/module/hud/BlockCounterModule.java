package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

/** Contador de blocos (para bridge): quantos blocos você tem no inventário, perto da mira. */
public class BlockCounterModule extends TextHudModule {

	private final BooleanSetting onlyHolding = add(new BooleanSetting("onlyHolding", "Só com bloco na mão", true));

	public BlockCounterModule() {
		super("blockCounter", "Contador de blocos", "Quantos blocos você tem (bridge)", false, 0, 14);
		setDefaultAnchor(4); // centro da tela, um pouco abaixo da mira
	}

	@Override
	public String getText(MinecraftClient client) {
		if (client.player == null) {
			return null;
		}
		ItemStack held = client.player.getMainHandStack();
		if (onlyHolding.get() && (held == null || !(held.getItem() instanceof BlockItem))) {
			return null;
		}
		int total = 0;
		for (ItemStack stack : client.player.inventory.main) {
			if (stack != null && stack.getItem() instanceof BlockItem) {
				total += stack.count;
			}
		}
		return format(total);
	}

	private String format(int total) {
		return VALUE + String.valueOf(total) + TEXT + (total == 1 ? " bloco" : " blocos");
	}

	@Override
	public String getSampleText() {
		return format(64);
	}
}