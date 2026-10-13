package com.vfxclient.core.module.hud;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Coordenadas do jogador.
 *
 * <p>Compacto: uma linha só ({@code 128 64 -256 N}). Avançado: X/Y/Z, direção por extenso e chunk.</p>
 */
public class CoordinatesModule extends TextHudModule {

	/** Pontos cardeais na ordem do yaw do Minecraft (0 = sul). */
	private static final String[] SHORT = {"S", "O", "N", "L"};
	private static final String[] LONG = {"Sul (+Z)", "Oeste (-X)", "Norte (-Z)", "Leste (+X)"};

	private final BooleanSetting showDirection = add(new BooleanSetting("direction", "Mostrar direção", true));

	public CoordinatesModule() {
		super("coordinates", "Coordenadas", "Posição X, Y e Z", true, 4, 40);
	}

	@Override
	public List<String> getLines(MinecraftClient client, boolean preview) {
		if (client.player == null) {
			return preview ? sample() : Collections.<String>emptyList();
		}
		int x = MathHelper.floor(client.player.x);
		int y = MathHelper.floor(client.player.y);
		int z = MathHelper.floor(client.player.z);
		int dir = MathHelper.floor(client.player.yaw * 4.0F / 360.0F + 0.5D) & 3;
		return format(x, y, z, dir);
	}

	private List<String> format(int x, int y, int z, int dir) {
		if (!advanced()) {
			String line = x + " " + y + " " + z;
			return Collections.singletonList(showDirection.get() ? line + "  " + SHORT[dir] : line);
		}
		String first = "X " + x + "  Y " + y + "  Z " + z;
		String second = "§7Chunk " + (x >> 4) + ", " + (z >> 4);
		if (showDirection.get()) {
			second = "§7" + LONG[dir] + "  ·  Chunk " + (x >> 4) + ", " + (z >> 4);
		}
		return Arrays.asList(first, second);
	}

	private List<String> sample() {
		return format(128, 64, -256, 2);
	}

	@Override
	public String getSampleText() {
		return sample().get(0);
	}
}
