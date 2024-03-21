/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.tck.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Locale;

import javax.net.ssl.X509TrustManager;

import org.apache.catalina.Container;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.IOTools;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;

import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.container.tomcat.embedded.Tomcat10EmbeddedContainer;

public class TomcatServletTckConfiguration implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(ServletObserver.class);
    }

    public static class ServletObserver {

        public void configureTomcat(@Observes final AfterStart afterStart) {
            Tomcat10EmbeddedContainer container = (Tomcat10EmbeddedContainer) afterStart.getDeployableContainer();
            try {
            	// Obtain reference to Tomcat instance
                Field tomcatField = Tomcat10EmbeddedContainer.class.getDeclaredField("tomcat");
                tomcatField.setAccessible(true);
                Tomcat tomcat = (Tomcat) tomcatField.get(container);
                Connector connectorHttp = tomcat.getConnector();
                int localPort;

	            if ("http".equals(System.getProperty("arquillian.launch"))) {
	            	// HTTP used for all tests apart from CLIENT-CERT

	                // Add trailer headers used in TCK to allow list
	                connectorHttp.setProperty("allowedTrailerHeaders", "myTrailer,myTrailer2");
	                localPort = connectorHttp.getLocalPort();

	                // Add expected users
	                tomcat.addUser("j2ee", "j2ee");
	                tomcat.addRole("j2ee", "Administrator");
	                tomcat.addRole("j2ee", "Employee");
	                tomcat.addUser("javajoe", "javajoe");
	                tomcat.addRole("javajoe", "VP");
	                tomcat.addRole("javajoe", "Manager");
	            } else {
	            	// Need to enabled HTTPS - only used for client-cert tests
	            	Connector connectorHttps = new Connector();
	            	connectorHttps.setPort(0);
	            	connectorHttps.setSecure(true);
	            	connectorHttps.setProperty("SSLEnabled", "true");

	                SSLHostConfig sslHostConfig = new SSLHostConfig();
	                SSLHostConfigCertificate certificateConfig = new SSLHostConfigCertificate(sslHostConfig, Type.UNDEFINED);
	                sslHostConfig.addCertificate(certificateConfig);
	                connectorHttps.addSslHostConfig(sslHostConfig);

	                // Can't use TLSv1.3 else certificate authentication won't work
	                sslHostConfig.setSslProtocol("TLS");
	                sslHostConfig.setProtocols("TLSv1.2");

	                // Server trust store needs to contain server root and client certificate
	                KeyStore trustStore = KeyStore.getInstance("JKS");
	                trustStore.load(null, null);
	                importKeyStore("/ca.jks", trustStore);
	                importKeyStore("/certificates/clientcert.jks", trustStore);
	                sslHostConfig.setTrustStore(trustStore);

	                // Server certificate
	                certificateConfig.setCertificateKeystoreFile(
	                		this.getClass().getResource("/localhost-rsa.jks").toExternalForm());

	                tomcat.getService().addConnector(connectorHttps);
	            	localPort = connectorHttps.getLocalPort();

	            	// Configure the client
	            	// Copy the client certificate from the TCK JAR
	            	File clientCertCopy = new File("target/clientcert.jks");
	            	if (!clientCertCopy.exists()) {
		            	try (InputStream clientCertInputStream =
		            			this.getClass().getResourceAsStream("/certificates/clientcert.jks");
		            			OutputStream clientCertOutputStream = new FileOutputStream(clientCertCopy)
		            			) {
		            		IOTools.flow(clientCertInputStream, clientCertOutputStream);
		            	}
	            	}
	            	System.setProperty("javax.net.ssl.keyStore", clientCertCopy.getAbsolutePath());
	            	System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
	            	URL trustStoreUrl = this.getClass().getResource("/ca.jks");
	            	System.setProperty("javax.net.ssl.trustStore", trustStoreUrl.getFile());

	            	// Create the user
	            	tomcat.addUser("CN=CTS, OU=Java Software, O=Sun Microsystems Inc., L=Burlington, ST=MA, C=US", "must-be-non-null");
	            	tomcat.addRole("CN=CTS, OU=Java Software, O=Sun Microsystems Inc., L=Burlington, ST=MA, C=US", "Administrator");
	            }

                // Update Arquillian configuration with port being used by Tomcat
                Field configurationField = Tomcat10EmbeddedContainer.class.getDeclaredField("configuration");
                configurationField.setAccessible(true);
                Object configuration = configurationField.get(container);
                Field portField = container.getConfigurationClass().getDeclaredField("bindHttpPort");
                portField.setAccessible(true);
                portField.set(configuration, Integer.valueOf(localPort));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        private void importKeyStore(String resourcePath, KeyStore destination) throws Exception {
            try (InputStream caInputStream = this.getClass().getResourceAsStream(resourcePath)) {
            	KeyStore caStore = KeyStore.getInstance("JKS");
            	caStore.load(caInputStream, "changeit".toCharArray());
            	Enumeration<String> aliases = caStore.aliases();
            	while (aliases.hasMoreElements()) {
            		String alias = aliases.nextElement();
            		Certificate certificate = caStore.getCertificate(alias);
            		destination.setCertificateEntry(alias, certificate);
            	}
            }
        }


        public void configureContext(@Observes final AfterDeploy afterDeploy) {
            Tomcat10EmbeddedContainer container = (Tomcat10EmbeddedContainer) afterDeploy.getDeployableContainer();
            try {
            	// Obtain reference to Tomcat instance
                Field tomcatField = Tomcat10EmbeddedContainer.class.getDeclaredField("tomcat");
                tomcatField.setAccessible(true);
                Tomcat tomcat = (Tomcat) tomcatField.get(container);

                // Obtain reference to web application(s)
                Container contexts[] = tomcat.getHost().findChildren();
                for (Container context : contexts) {

                	// Configure expected encoding mapping unless application has defined one explicitly
                	StandardContext stdContext = (StandardContext) context;
                	if (stdContext.getCharsetMapper().getCharset(Locale.forLanguageTag("ja")) == null) {
                		stdContext.addLocaleEncodingMappingParameter("ja", "Shift_JIS");
                	}

                	// Enable cross-context dispatches
                	stdContext.setCrossContext(true);

                	// Subset of STRICT_SERVLET_COMPLIANCE required by TCK
                	stdContext.setAlwaysAccessSession(true);
                	stdContext.setContextGetResourceRequiresSlash(true);
                	stdContext.setUseRelativeRedirects(false);
                	stdContext.getManager().setSessionActivityCheck(true);
                	stdContext.getManager().setSessionLastAccessAtStart(true);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static class TrustAllCerts implements X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs,
                String authType) {
            // NOOP - Trust everything
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs,
                String authType) {
            // NOOP - Trust everything
        }
    }
}
