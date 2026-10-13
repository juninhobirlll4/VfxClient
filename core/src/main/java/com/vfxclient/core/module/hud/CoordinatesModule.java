package com.vfxclient.core.module.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

/**
 * Coordenadas do jogador.
 *
 * <p>Compacto: uma linha só ({@code 128 64 -256 N}). Avançado: X/Y/Z, direção por extenso e chunk.
 * Opcionais: bioma e a posição equivalente no Nether/Overworld (divide ou multiplica por 8).</p>
 */
public class CoordinatesModule extends TextHudModule {

	/** Pontos cardeais na ordem do yaw do Minecraft (0 = sul). */
	private static final String[] SHORT = {"S", "O", "N", "L"};
	private static final String[] LONG = {"Sul (+Z)", "Oeste (-X)", "Norte (-Z)", "Leste (+X)"};
	private static final int NETHER = -1;
	private static final int OVERWORLD = 0;

	private final BooleanSetting showDirection = add(new BooleanSetting("direction", "Mostrar direção", true));
	private final BooleanSetting showBiome = add(new BooleanSetting("biome", "Mostrar bioma", false));
	private final BooleanSetting convert = add(new BooleanSetting("convert", "Converter Nether/Overworld", false));

	public CoordinatesModule() {
		super("coordinates", "Coordenadas", "Posição X, Y e Z", true, 4, 40);
	}

	@Override
	public List<String> getLines(MinecraftClient client, boolean preview) {
		if (client.player == null || client.world == null) {
			return preview ? format(128, 64, -256, 2, "Planície", OVERWORLD) : Collections.<String>emptyList();
		}
		int x = MathHelper.floor(client.player.x);
		int y = MathHelper.floor(client.player.y);
		int z = MathHelper.floor(client.player.z);
		int dir = MathHelper.floor(client.player.yaw * 4.0F / 360.0F + 0.5D) & 3;
		String biome = null;
		if (showBiome.get()) {
			Biome b = client.world.getBiome(new BlockPos(x, y, z));
			biome = b == null ? null : b.name;
		}
		return format(x, y, z, dir, biome, client.player.dimension);
	}

	private List<String> format(int x, int y, int z, int dir, String biome, int dimension) {
		List<String> lines = new ArrayList<String>();
		if (!advanced()) {
			String line = VALUE + (x + " " + y + " " + z) + TEXT;
			lines.add(showDirection.get() ? line + "  " + SHORT[dir] : line);
		} else {
			lines.add("X " + VALUE + x + TEXT + "  Y " + VALUE + y + TEXT + "  Z " + VALUE + z);
			String second = "§7Chunk " + (x >> 4) + ", " + (z >> 4);
			if (showDirection.get()) {
				second = "§7" + LONG[dir] + "  ·  Chunk " + (x >> 4) + ", " + (z >> 4);
			}
			lines.add(second);
		}
		if (showBiome.get() && biome != null) {
			lines.add("§7Bioma: §r" + biome);
		}
		if (convert.get() && (dimension == NETHER || dimension == OVERWORLD)) {
			boolean nether = dimension == NETHER;
			int cx = nether ? x * 8 : Math.floorDiv(x, 8);
			int cz = nether ? z * 8 : Math.floorDiv(z, 8);
			lines.add("§7" + (nether ? "Overworld" : "Nether") + ": §r" + cx + " " + cz);
		}
		return lines;
	}

	@Override
	public String getSampleText() {
		return format(128, 64, -256, 2, "Planície", OVERWORLD).get(0);
	}
}
