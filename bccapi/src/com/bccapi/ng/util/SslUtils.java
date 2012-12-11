/**
 * Copyright 2011 bccapi.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bccapi.ng.util;

import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.bccapi.bitlib.util.HexUtils;

/**
 * SSL Utilities.
 */
public class SslUtils {
	
    private static final String BCCAPI_COM_SSL_THUMBPRINT = "b9:d9:0e:a2:7f:f4:79:3a:2b:54:be:40:ba:cb:56:65:56:13:0c:cc";
    private static final HostnameVerifier HOST_NAME_VERIFIER;
	private static final SSLSocketFactory SSL_SOCKET_FACTORY;
	
	static {

		// Used for disabling host name verification. This is safe because we
		// trust the BCCAPI server certificate explicitly
		HOST_NAME_VERIFIER = new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Make a trust manager that trusts the BCCAPI server certificate and
		// nothing else
		TrustManager[] trustOneCert = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType)
					throws java.security.cert.CertificateException {
				// We do not use a client side certificate
				throw new CertificateException();
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType)
					throws java.security.cert.CertificateException {
				if (BCCAPI_COM_SSL_THUMBPRINT == null
						|| BCCAPI_COM_SSL_THUMBPRINT.equals("")
						|| certs == null || certs.length == 0) {
					throw new CertificateException();
				}
				for (X509Certificate certificate : certs) {
					String sslThumbprint = generateCertificateThumbprint(certificate);
					if (BCCAPI_COM_SSL_THUMBPRINT
							.equalsIgnoreCase(sslThumbprint)) {
						return;
					}
				}
				throw new CertificateException();
			}
		} };

		// Create an SSL socket factory which trusts the BCCAPi server certificate
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustOneCert, null);
			SSL_SOCKET_FACTORY = sc.getSocketFactory();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}
	
   /**
    * Makes an URL connection to accept a server-side certificate with specific
    * thumbprint and ignore host name verification. This is useful and safe if
    * you have a client with a hard coded well-known certificate
    * 
    * @param connection
    *           The connection to configure
    * @param serverThumbprint
    *           The X509 thumbprint of the server side certificate
    */
	public static void configureTrustedCertificate(URLConnection connection,
			final String serverThumbprint) {
		if (!(connection instanceof HttpsURLConnection)) {
			return;
		}

		HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) connection;

		if (httpsUrlConnection.getHostnameVerifier() != HOST_NAME_VERIFIER) {
			httpsUrlConnection.setHostnameVerifier(HOST_NAME_VERIFIER);
		}
		if (httpsUrlConnection.getSSLSocketFactory() != SSL_SOCKET_FACTORY) {
			httpsUrlConnection.setSSLSocketFactory(SSL_SOCKET_FACTORY);
		}
   }

   /**
    * Generates an SSL thumbprint from a certificate
    * 
    * @param certificate
    *           The certificate
    * @return The thumbprint of the certificate
    */
   private static String generateCertificateThumbprint(Certificate certificate) {
      try {
         MessageDigest md;
         try {
            md = MessageDigest.getInstance("SHA-1");
         } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
         }
         byte[] encoded;

         try {
            encoded = certificate.getEncoded();
         } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
         }
         return HexUtils.toHex(md.digest(encoded), ":");
      } catch (Exception e) {
         return null;
      }
   }

}
