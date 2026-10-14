package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemRenderer;
import net.minecraft.client.render.entity.feature.StuckArrowsFeatureRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Jogadores longe desenhados com menos detalhe: sem capa, armadura, item na mão, flechas
 * presas e camadas da skin. O nome também pode sumir antes do padrão do jogo (64 blocos).
 * Ajuda em PvP e lobbies com muita gente.
 */
public class FarPlayersModule extends Module {

	private final NumberSetting distance = add(new NumberSetting("distance", "A partir de", 16, 6, 48, 2, " blocos"));
	private final BooleanSetting capes = add(new BooleanSetting("capes", "Sem capa", true));
	private final BooleanSetting armor = add(new BooleanSetting("armor", "Sem armadura", true));
	private final BooleanSetting heldItems = add(new BooleanSetting("heldItems", "Sem item na mão", true));
	private final BooleanSetting skinLayers = add(new BooleanSetting("skinLayers", "Sem camadas da skin (chapéu, jaqueta)", true));
	private final NumberSetting nametagDistance = add(new NumberSetting("nametagDistance", "Nomes até", 64, 8, 64, 4, " blocos"));

	public FarPlayersModule() {
		super("farPlayers", "Jogadores longe simples", "Menos detalhe nos jogadores distantes", Category.PERFORMANCE, false);
	}

	/**
	 * @param feature camada que o jogo vai desenhar por cima do modelo
	 * @param entity  dono da camada
	 * @return {@code true} se a camada deve ser pulada
	 */
	public boolean skipFeature(FeatureRenderer<?> feature, LivingEntity entity) {
		if (!isFar(entity)) {
			return false;
		}
		if (feature instanceof CapeFeatureRenderer) {
			return capes.get();
		}
		if (feature instanceof ArmorFeatureRenderer || feature instanceof HeadFeatureRenderer) {
			return armor.get();
		}
		if (feature instanceof HeldItemRenderer) {
			return heldItems.get();
		}
		return feature instanceof StuckArrowsFeatureRenderer;
	}

	/** @return se as camadas da skin (chapéu, jaqueta, mangas, calça) devem sumir */
	public boolean hideSkinLayers(PlayerEntity player) {
		return skinLayers.get() && isFar(player);
	}

	/** @return se o nome em cima do jogador deve sumir */
	public boolean hideNametag(Entity entity) {
		if (!isEnabled() || !(entity instanceof PlayerEntity)) {
			return false;
		}
		double max = nametagDistance.get();
		return max < 64 && squaredDistance(entity) > max * max;
	}

	private boolean isFar(Entity entity) {
		if (!isEnabled() || !(entity instanceof PlayerEntity)) {
			return false;
		}
		double min = distance.get();
		return squaredDistance(entity) > min * min;
	}

	private static double squaredDistance(Entity entity) {
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		return camera == null || camera == entity ? 0 : entity.squaredDistanceTo(camera);
	}
}
