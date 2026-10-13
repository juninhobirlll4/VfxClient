package com.vfxclient.core.module.social;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.google.gson.JsonObject;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/**
 * Discord Rich Presence: mostra no seu perfil do Discord que você está jogando VfxClient,
 * onde (menu, mundo local ou servidor) e há quanto tempo.
 *
 * <p>Fala direto com o Discord aberto no PC (pipe local {@code discord-ipc}); nada passa por
 * servidores nossos. Precisa do "Application ID" de um aplicativo criado em
 * discord.com/developers (grátis): sem ele o módulo não faz nada.</p>
 */
public class DiscordModule extends Module {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final long UPDATE_MS = 15000L;

	private final TextSetting appId = add(new TextSetting("appId", "Application ID do Discord", "", 24));
	private final BooleanSetting showServer = add(new BooleanSetting("showServer", "Mostrar o endereço do servidor", false));

	private final long startedAt = System.currentTimeMillis() / 1000L;
	private volatile String details = "No menu";
	private Thread worker;
	private volatile boolean running;

	public DiscordModule() {
		super("discord", "Discord", "Status no Discord (precisa do Application ID)", Category.SOCIAL, false);
	}

	@Override
	protected void onEnable() {
		running = true;
		worker = new Thread(new Runnable() {
			@Override
			public void run() {
				loop();
			}
		}, "VfxClient-Discord");
		worker.setDaemon(true);
		worker.start();
	}

	@Override
	protected void onDisable() {
		running = false;
		if (worker != null) {
			worker.interrupt();
			worker = null;
		}
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (client.world == null) {
			details = "No menu";
		} else if (client.isInSingleplayer()) {
			details = "Mundo local";
		} else {
			ServerInfo server = client.getCurrentServerEntry();
			details = showServer.get() && server != null ? "Jogando em " + server.address : "Jogando em um servidor";
		}
	}

	private void loop() {
		while (running) {
			String id = appId.get().trim();
			if (!id.matches("\\d{15,22}")) {
				sleep(5000L);
				continue;
			}
			RandomAccessFile pipe = open();
			if (pipe == null) {
				sleep(20000L); // Discord fechado: tenta de novo depois
				continue;
			}
			try {
				JsonObject hello = new JsonObject();
				hello.addProperty("v", 1);
				hello.addProperty("client_id", id);
				send(pipe, 0, hello);
				read(pipe);
				String lastSent = null;
				long lastAt = 0;
				while (running && id.equals(appId.get().trim())) {
					String now = details;
					if (!now.equals(lastSent) || System.currentTimeMillis() - lastAt > UPDATE_MS * 4) {
						send(pipe, 1, activity(now));
						read(pipe);
						lastSent = now;
						lastAt = System.currentTimeMillis();
					}
					sleep(3000L);
				}
				if (!running) {
					send(pipe, 1, activity(null));
				}
			} catch (Exception e) {
				VfxCore.LOGGER.debug("Discord desconectado: {}", e.toString());
			} finally {
				try {
					pipe.close();
				} catch (Exception ignored) {
					// Já fechado.
				}
			}
		}
	}

	private JsonObject activity(String text) {
		JsonObject args = new JsonObject();
		args.addProperty("pid", pid());
		if (text != null) {
			JsonObject activity = new JsonObject();
			activity.addProperty("details", text);
			activity.addProperty("state", "VfxClient " + VfxCore.get().getVersion() + " · Minecraft 1.8.9");
			JsonObject timestamps = new JsonObject();
			timestamps.addProperty("start", startedAt);
			activity.add("timestamps", timestamps);
			args.add("activity", activity);
		}
		JsonObject command = new JsonObject();
		command.addProperty("cmd", "SET_ACTIVITY");
		command.add("args", args);
		command.addProperty("nonce", Long.toString(System.nanoTime()));
		return command;
	}

	private static RandomAccessFile open() {
		for (int i = 0; i < 10; i++) {
			try {
				return new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
			} catch (Exception ignored) {
				// Próximo número.
			}
		}
		return null;
	}

	private static void send(RandomAccessFile pipe, int opcode, JsonObject body) throws Exception {
		byte[] data = body.toString().getBytes(UTF8);
		ByteBuffer buffer = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(opcode).putInt(data.length).put(data);
		pipe.write(buffer.array());
	}

	private static void read(RandomAccessFile pipe) throws Exception {
		byte[] header = new byte[8];
		pipe.readFully(header);
		int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
		if (length > 0 && length < (1 << 20)) {
			pipe.readFully(new byte[length]);
		}
	}

	private static int pid() {
		String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		try {
			return Integer.parseInt(name.substring(0, name.indexOf('@')));
		} catch (RuntimeException e) {
			return 0;
		}
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
