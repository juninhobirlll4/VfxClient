package com.vfxclient.core.module.performance;

import java.util.HashMap;
import java.util.Map;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Não desenha entidades escondidas atrás de blocos (occlusion culling).
 *
 * <p>Para cada entidade, traça linhas da câmera até alguns pontos do corpo dela; se todas
 * batem em blocos, a entidade não aparece na tela e o jogo não precisa desenhá-la. O
 * resultado fica em cache por alguns ticks para o teste não pesar mais do que economiza.</p>
 */
public class EntityCullingModule extends Module {

	private static final int RECHECK_TICKS = 4;
	private static final double ALWAYS_VISIBLE_SQ = 4 * 4;

	private final BooleanSetting keepPlayers = add(new BooleanSetting("keepPlayers", "Nunca esconder jogadores", false));

	private final Map<Integer, long[]> cache = new HashMap<Integer, long[]>();
	private World lastWorld;
	private long tick;

	public EntityCullingModule() {
		super("entityCulling", "Culling de entidades", "Esconde o que está atrás de blocos", Category.PERFORMANCE, false);
	}

	@Override
	public void onTick(MinecraftClient client) {
		tick++;
		if (client.world != lastWorld || cache.size() > 4096) {
			cache.clear();
			lastWorld = client.world;
		}
	}

	@Override
	protected void onDisable() {
		cache.clear();
	}

	/**
	 * @param entity entidade que o jogo vai desenhar
	 * @return {@code true} se ela está escondida e pode ser pulada
	 */
	public boolean shouldCull(Entity entity) {
		if (!isEnabled()) {
			return false;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null || client.world == null || entity == camera || entity.rider == camera || camera.vehicle == entity) {
			return false;
		}

		if (keepPlayers.get() && entity instanceof PlayerEntity) {
			return false;
		}

		if (entity.squaredDistanceTo(camera) < ALWAYS_VISIBLE_SQ) {
			return false;
		}

		long[] cached = cache.get(entity.getEntityId());
		if (cached != null && tick - cached[0] < RECHECK_TICKS) {
			return cached[1] == 0;
		}

		boolean visible = isVisible(client.world, camera, entity);
		cache.put(entity.getEntityId(), new long[]{tick, visible ? 1 : 0});
		return !visible;
	}

	private static boolean isVisible(World world, Entity camera, Entity entity) {
		Vec3d eye = new Vec3d(camera.x, camera.y + camera.getEyeHeight(), camera.z);
		Box box = entity.getBoundingBox();
		double midX = (box.minX + box.maxX) / 2;
		double midZ = (box.minZ + box.maxZ) / 2;
		double[] heights = {box.maxY - 0.05, (box.minY + box.maxY) / 2, box.minY + 0.1};
		for (double y : heights) {
			if (world.rayTrace(eye, new Vec3d(midX, y, midZ), false, true, false) == null) {
				return true;
			}
		}
		// Cantos do topo: pega entidades com só a lateral aparecendo.
		double[][] corners = {{box.minX, box.minZ}, {box.maxX, box.maxZ}, {box.minX, box.maxZ}, {box.maxX, box.minZ}};
		for (double[] corner : corners) {
			if (world.rayTrace(eye, new Vec3d(corner[0], box.maxY - 0.05, corner[1]), false, true, false) == null) {
				return true;
			}
		}

		return false;
	}
}
