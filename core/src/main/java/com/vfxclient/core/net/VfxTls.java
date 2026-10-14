package com.vfxclient.core.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Conexão segura com o servidor do VfxClient (emotes e skins). O Java do jogo (8u51, de 2015) não conhece as
 * autoridades que assinam os certificados da Cloudflare hoje; por isso o client leva os
 * certificados raiz públicos em {@code assets/vfx/certs/roots.pem} e confia neles além dos do Java.
 */
public final class VfxTls {

	private static final String ROOTS = "/assets/vfx/certs/roots.pem";
	private static SSLSocketFactory factory;

	private VfxTls() {
	}

	/** @return fábrica de conexões seguras que confia nos certificados do Java e nos do client */
	public static synchronized SSLSocketFactory socketFactory() throws IOException {
		if (factory == null) {
			try {
				SSLContext context = SSLContext.getInstance("TLSv1.2");
				context.init(null, new TrustManager[] {new Combined(javaDefaults(), bundled())}, null);
				factory = context.getSocketFactory();
			} catch (GeneralSecurityException e) {
				throw new IOException("Conexão segura indisponível: " + e.getMessage(), e);
			}
		}
		return factory;
	}

	private static X509TrustManager javaDefaults() throws GeneralSecurityException {
		return trustManager(null);
	}

	private static X509TrustManager bundled() throws GeneralSecurityException, IOException {
		KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
		store.load(null, null);
		int index = 0;
		for (X509Certificate certificate : readRoots()) {
			store.setCertificateEntry("vfx-root-" + index++, certificate);
		}
		return trustManager(store);
	}

	private static X509TrustManager trustManager(KeyStore store) throws GeneralSecurityException {
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(store);
		for (TrustManager manager : tmf.getTrustManagers()) {
			if (manager instanceof X509TrustManager) {
				return (X509TrustManager) manager;
			}
		}
		throw new GeneralSecurityException("Sem verificador de certificados.");
	}

	/** Lê os blocos PEM do arquivo (os comentários entre eles são ignorados). */
	private static List<X509Certificate> readRoots() throws IOException, CertificateException {
		List<X509Certificate> roots = new ArrayList<X509Certificate>();
		InputStream in = VfxTls.class.getResourceAsStream(ROOTS);
		if (in == null) {
			return roots;
		}

		CertificateFactory certificates = CertificateFactory.getInstance("X.509");
		try (Scanner scanner = new Scanner(in, StandardCharsets.US_ASCII.name())) {
			StringBuilder base64 = null;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (line.startsWith("-----BEGIN CERTIFICATE-----")) {
					base64 = new StringBuilder();
				} else if (line.startsWith("-----END CERTIFICATE-----") && base64 != null) {
					byte[] der = Base64.getMimeDecoder().decode(base64.toString());
					roots.add((X509Certificate) certificates.generateCertificate(new ByteArrayInputStream(der)));
					base64 = null;
				} else if (base64 != null) {
					base64.append(line);
				}
			}
		}
		return roots;
	}

	/** Aceita o certificado se o Java ou os certificados do client confiarem nele. */
	private static final class Combined implements X509TrustManager {

		private final X509TrustManager java;
		private final X509TrustManager vfx;

		Combined(X509TrustManager java, X509TrustManager vfx) {
			this.java = java;
			this.vfx = vfx;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			try {
				java.checkServerTrusted(chain, authType);
			} catch (CertificateException e) {
				vfx.checkServerTrusted(chain, authType);
			}
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			java.checkClientTrusted(chain, authType);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			X509Certificate[] a = java.getAcceptedIssuers();
			X509Certificate[] b = vfx.getAcceptedIssuers();
			X509Certificate[] all = new X509Certificate[a.length + b.length];
			System.arraycopy(a, 0, all, 0, a.length);
			System.arraycopy(b, 0, all, a.length, b.length);
			return all;
		}
	}
}
