package com.vfxclient.core.emote;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Quem está fazendo qual emote agora (você e os outros jogadores do VfxClient), e o desenho
 * da pose no modelo de cada um.
 */
public final class EmoteState {

	private static final float FADE_IN = 0.2F;
	private static final float FADE_OUT = 0.25F;

	/** Emote em andamento de um jogador. */
	private static final class Playing {
		final Emote emote;
		final String name;
		final long startedAt = System.nanoTime();

		Playing(Emote emote, String name) {
			this.emote = emote;
			this.name = name;
		}

		float seconds() {
			return (System.nanoTime() - startedAt) / 1_000_000_000F;
		}
	}

	private static final Map<UUID, Playing> PLAYING = new ConcurrentHashMap<UUID, Playing>();

	/** Ligado enquanto o jogo desenha o braço em primeira pessoa (que não deve dançar junto). */
	private static boolean drawingFirstPersonArm;

	private EmoteState() {
	}

	/**
	 * @param uuid  jogador
	 * @param name  nick (usado se o servidor trocar o UUID dos jogadores)
	 * @param emote emote a fazer
	 */
	public static void play(UUID uuid, String name, Emote emote) {
		PLAYING.put(uuid, new Playing(emote, name));
	}

	public static void stop(UUID uuid) {
		PLAYING.remove(uuid);
	}

	public static void clear() {
		PLAYING.clear();
	}

	/** @return se esse jogador está no meio de um emote */
	public static boolean isPlaying(UUID uuid) {
		Playing playing = PLAYING.get(uuid);
		return playing != null && playing.seconds() < playing.emote.length();
	}

	/**
	 * Tira da lista os emotes que já terminaram e alinha o corpo de quem está fazendo emote com a
	 * cabeça (parado, o corpo do 1.8.9 fica virado e o emote sairia torto). Chamado a cada tick.
	 */
	public static void tick(World world) {
		Iterator<Playing> it = PLAYING.values().iterator();
		while (it.hasNext()) {
			Playing playing = it.next();
			if (playing.seconds() >= playing.emote.length()) {
				it.remove();
			}
		}
		if (world == null || PLAYING.isEmpty()) {
			return;
		}

		for (PlayerEntity player : world.playerEntities) {
			if (find(player) != null) {
				player.bodyYaw = player.headYaw;
				player.prevBodyYaw = player.prevHeadYaw;
			}
		}
	}

	public static void setDrawingFirstPersonArm(boolean drawing) {
		drawingFirstPersonArm = drawing;
	}

	/**
	 * Põe o modelo na pose do emote do jogador, se ele estiver fazendo um. Chamado pelo mixin
	 * depois que o jogo posicionou o modelo.
	 */
	public static void applyPose(PlayerEntityModel model, PlayerEntity player) {
		if (drawingFirstPersonArm) {
			return;
		}
		Playing playing = find(player);
		if (playing == null) {
			return;
		}

		float seconds = playing.seconds();
		float length = playing.emote.length();
		if (seconds >= length) {
			return;
		}
		float blend = Math.min(1F, Math.min(seconds / FADE_IN, (length - seconds) / FADE_OUT));
		playing.emote.apply(model, seconds, blend);

		// As camadas da skin seguem os braços, pernas e corpo.
		EntityModel.copyModelPart(model.leftLeg, model.leftPants);
		EntityModel.copyModelPart(model.rightLeg, model.rightPants);
		EntityModel.copyModelPart(model.leftArm, model.leftSleeve);
		EntityModel.copyModelPart(model.rightArm, model.rightSleeve);
		EntityModel.copyModelPart(model.body, model.jacket);
		EntityModel.copyModelPart(model.head, model.hat);
	}

	private static Playing find(PlayerEntity player) {
		Playing playing = PLAYING.get(player.getUuid());
		if (playing != null) {
			return playing;
		}
		String name = player.getGameProfile().getName();
		for (Playing other : PLAYING.values()) {
			if (other.name != null && other.name.equalsIgnoreCase(name)) {
				return other;
			}
		}
		return null;
	}
}
