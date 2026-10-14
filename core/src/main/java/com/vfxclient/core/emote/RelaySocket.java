package com.vfxclient.core.emote;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import com.vfxclient.core.net.VfxTls;

/**
 * WebSocket mínimo (o Java 8 do jogo não traz um): conecta em {@code ws://} ou {@code wss://},
 * manda e recebe texto, responde ping e fecha. É só o que o servidor de emotes usa.
 */
final class RelaySocket implements Closeable {

	/** Avisos da conexão; chamados na thread de leitura, não na do jogo. */
	interface Listener {
		void onText(String text);

		void onClosed(String reason);
	}

	private static final String HANDSHAKE_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	private static final int CONNECT_TIMEOUT_MS = 8000;
	private static final int MAX_FRAME = 64 * 1024;

	private static final int OP_CONTINUATION = 0x0;
	private static final int OP_TEXT = 0x1;
	private static final int OP_CLOSE = 0x8;
	private static final int OP_PING = 0x9;
	private static final int OP_PONG = 0xA;

	private final SecureRandom random = new SecureRandom();
	private final Socket socket;
	private final OutputStream out;
	private volatile boolean closed;

	private RelaySocket(Socket socket) throws IOException {
		this.socket = socket;
		this.out = socket.getOutputStream();
	}

	/**
	 * Abre a conexão e começa a ler numa thread própria.
	 *
	 * @param uri      endereço {@code ws://} ou {@code wss://}
	 * @param listener quem recebe as mensagens
	 * @return a conexão aberta
	 * @throws IOException se não conectar ou o servidor recusar
	 */
	static RelaySocket connect(URI uri, Listener listener) throws IOException {
		boolean secure = "wss".equalsIgnoreCase(uri.getScheme());
		int port = uri.getPort() > 0 ? uri.getPort() : (secure ? 443 : 80);

		Socket raw = new Socket();
		raw.connect(new InetSocketAddress(uri.getHost(), port), CONNECT_TIMEOUT_MS);
		Socket socket = raw;
		if (secure) {
			SSLSocket tls = (SSLSocket) VfxTls.socketFactory().createSocket(raw, uri.getHost(), port, true);
			// O Java 8 não confere sozinho se o certificado é deste endereço.
			SSLParameters parameters = tls.getSSLParameters();
			parameters.setEndpointIdentificationAlgorithm("HTTPS");
			tls.setSSLParameters(parameters);
			socket = tls;
		}
		socket.setSoTimeout(CONNECT_TIMEOUT_MS);

		RelaySocket connection = new RelaySocket(socket);
		try {
			connection.handshake(uri, port, secure);
		} catch (IOException e) {
			connection.close();
			throw e;
		}
		socket.setSoTimeout(0);
		connection.startReading(listener);
		return connection;
	}

	private void handshake(URI uri, int port, boolean secure) throws IOException {
		byte[] nonce = new byte[16];
		random.nextBytes(nonce);
		String key = Base64.getEncoder().encodeToString(nonce);

		String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
		if (uri.getRawQuery() != null) {
			path += "?" + uri.getRawQuery();
		}
		boolean defaultPort = port == (secure ? 443 : 80);
		String host = defaultPort ? uri.getHost() : uri.getHost() + ":" + port;

		String request = "GET " + path + " HTTP/1.1\r\n"
				+ "Host: " + host + "\r\n"
				+ "Upgrade: websocket\r\n"
				+ "Connection: Upgrade\r\n"
				+ "Sec-WebSocket-Key: " + key + "\r\n"
				+ "Sec-WebSocket-Version: 13\r\n"
				+ "User-Agent: VfxClient\r\n\r\n";
		out.write(request.getBytes(StandardCharsets.US_ASCII));
		out.flush();

		InputStream in = socket.getInputStream();
		String status = readLine(in);
		if (!status.startsWith("HTTP/1.1 101")) {
			throw new IOException("Servidor de emotes recusou a conexão: " + status);
		}

		String accept = null;
		for (String line = readLine(in); !line.isEmpty(); line = readLine(in)) {
			int colon = line.indexOf(':');
			if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Sec-WebSocket-Accept")) {
				accept = line.substring(colon + 1).trim();
			}
		}
		if (!expectedAccept(key).equals(accept)) {
			throw new IOException("Resposta inválida do servidor de emotes.");
		}
	}

	private void startReading(Listener listener) {
		Thread reader = new Thread(() -> readLoop(listener), "VfxClient emotes");
		reader.setDaemon(true);
		reader.start();
	}

	private void readLoop(Listener listener) {
		String reason = "conexão encerrada";
		try {
			DataInputStream in = new DataInputStream(socket.getInputStream());
			ByteArrayOutputStream message = new ByteArrayOutputStream();
			while (!closed) {
				int first = in.readUnsignedByte();
				int second = in.readUnsignedByte();
				boolean fin = (first & 0x80) != 0;
				int opcode = first & 0x0F;
				byte[] payload = readPayload(in, second);

				if (opcode == OP_PING) {
					send(OP_PONG, payload);
				} else if (opcode == OP_CLOSE) {
					reason = "o servidor fechou a conexão";
					break;
				} else if (opcode == OP_TEXT || opcode == OP_CONTINUATION) {
					message.write(payload);
					if (fin) {
						listener.onText(new String(message.toByteArray(), StandardCharsets.UTF_8));
						message.reset();
					}
				}
			}
		} catch (IOException e) {
			reason = closed ? "fechada" : e.getMessage();
		} finally {
			close();
			listener.onClosed(reason);
		}
	}

	private static byte[] readPayload(DataInputStream in, int second) throws IOException {
		long length = second & 0x7F;
		if (length == 126) {
			length = in.readUnsignedShort();
		} else if (length == 127) {
			length = in.readLong();
		}
		if (length < 0 || length > MAX_FRAME) {
			throw new IOException("Mensagem grande demais do servidor de emotes.");
		}

		byte[] mask = null;
		if ((second & 0x80) != 0) {
			mask = new byte[4];
			in.readFully(mask);
		}
		byte[] payload = new byte[(int) length];
		in.readFully(payload);
		if (mask != null) {
			for (int i = 0; i < payload.length; i++) {
				payload[i] ^= mask[i % 4];
			}
		}
		return payload;
	}

	/** Manda um texto (JSON). */
	void sendText(String text) throws IOException {
		send(OP_TEXT, text.getBytes(StandardCharsets.UTF_8));
	}

	/** Ping para manter a conexão viva. */
	void ping() throws IOException {
		send(OP_PING, new byte[0]);
	}

	/** Quadro com máscara, como todo client de WebSocket precisa mandar. */
	private synchronized void send(int opcode, byte[] payload) throws IOException {
		if (closed) {
			throw new IOException("Conexão fechada.");
		}
		ByteArrayOutputStream frame = new ByteArrayOutputStream(payload.length + 14);
		frame.write(0x80 | opcode);
		if (payload.length < 126) {
			frame.write(0x80 | payload.length);
		} else if (payload.length <= 0xFFFF) {
			frame.write(0x80 | 126);
			frame.write(payload.length >>> 8);
			frame.write(payload.length & 0xFF);
		} else {
			throw new IOException("Mensagem grande demais.");
		}

		byte[] mask = new byte[4];
		random.nextBytes(mask);
		frame.write(mask);
		for (int i = 0; i < payload.length; i++) {
			frame.write(payload[i] ^ mask[i % 4]);
		}
		out.write(frame.toByteArray());
		out.flush();
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		try {
			socket.close();
		} catch (IOException ignored) {
			// Já estava fechando.
		}
	}

	private static String readLine(InputStream in) throws IOException {
		StringBuilder line = new StringBuilder();
		for (int c = in.read(); c != '\n'; c = in.read()) {
			if (c < 0) {
				throw new EOFException("Conexão caiu durante a abertura.");
			}
			if (c != '\r') {
				line.append((char) c);
			}
			if (line.length() > 8192) {
				throw new IOException("Resposta estranha do servidor de emotes.");
			}
		}
		return line.toString();
	}

	private static String expectedAccept(String key) throws IOException {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-1").digest((key + HANDSHAKE_GUID).getBytes(StandardCharsets.US_ASCII));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}
}
