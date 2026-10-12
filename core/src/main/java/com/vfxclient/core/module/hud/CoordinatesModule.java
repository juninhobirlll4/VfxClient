package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/** Mostra as coordenadas do jogador e, opcionalmente, a direção. */
public class CoordinatesModule extends HudModule {

	/** Pontos cardeais na ordem do yaw do Minecraft (0 = sul). */
	private static final String[] DIRECTIONS = {"S", "O", "N", "L"};

	private final BooleanSetting showDirection = add(new BooleanSetting("direction", "Mostrar direção", true));

	public CoordinatesModule() {
		super("coordinates", "Coordenadas", "Posição X, Y e Z", true, 4, 40);
	}

	@Override
	public String getText(MinecraftClient client) {
		if (client.player == null) {
			return null;
		}
		String text = "XYZ " + MathHelper.floor(client.player.x)
				+ " " + MathHelper.floor(client.player.y)
				+ " " + MathHelper.floor(client.player.z);
		if (showDirection.get()) {
			int index = MathHelper.floor(client.player.yaw * 4.0F / 360.0F + 0.5D) & 3;
			text += "  " + DIRECTIONS[index];
		}
		return text;
	}

	@Override
	public String getSampleText() {
		return "XYZ 128 64 -256  N";
	}
}
