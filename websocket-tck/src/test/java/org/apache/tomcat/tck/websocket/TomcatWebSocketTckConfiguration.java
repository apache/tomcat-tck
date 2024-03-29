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
package org.apache.tomcat.tck.websocket;

import java.lang.reflect.Field;
import java.net.URL;

import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.container.tomcat.embedded.EmbeddedContextConfig;
import org.jboss.arquillian.container.tomcat.embedded.Tomcat10EmbeddedContainer;

public class TomcatWebSocketTckConfiguration implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(WebSocketObserver.class);
    }

    public static class WebSocketObserver {

        public void configureContext(@Observes final BeforeDeploy beforeDeploy) {
            Tomcat10EmbeddedContainer container = (Tomcat10EmbeddedContainer) beforeDeploy.getDeployableContainer();
            try {
                // Obtain reference to Tomcat instance
                Field tomcatField = Tomcat10EmbeddedContainer.class.getDeclaredField("tomcat");
                tomcatField.setAccessible(true);
                Tomcat tomcat = (Tomcat) tomcatField.get(container);
                Connector connectorHttp = tomcat.getConnector();
                int localPort;

                // Add expected users
                tomcat.addUser("j2ee", "j2ee");
                tomcat.addRole("j2ee", "staff");

                if ("https".equals(System.getProperty("arquillian.launch"))) {
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

                    // Server certificate
                    certificateConfig.setCertificateKeystoreFile(
                            this.getClass().getResource("/localhost-rsa.jks").toExternalForm());

                    tomcat.getService().addConnector(connectorHttps);
                    localPort = connectorHttps.getLocalPort();

                    // Configure the client
                    URL trustStoreUrl = this.getClass().getResource("/ca.jks");
                    System.setProperty("javax.net.ssl.trustStore", trustStoreUrl.getFile());
                } else {
                    localPort = connectorHttp.getLocalPort();
                }

                // Configure JAR scanner
                Host host = tomcat.getHost();
                host.setConfigClass(EmbeddedWebSocketContextConfig.class.getName());

                // Update Arquillian configuration with port being used by Tomcat
                Field configurationField = Tomcat10EmbeddedContainer.class.getDeclaredField("configuration");
                configurationField.setAccessible(true);
                Object configuration = configurationField.get(container);
                Field portField = container.getConfigurationClass().getDeclaredField("bindHttpPort");
                portField.setAccessible(true);
                portField.set(configuration, Integer.valueOf(localPort));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class EmbeddedWebSocketContextConfig extends EmbeddedContextConfig {

        /*
         *  Perform the necessary configuration for the WebSocket TCK to execute correctly on Tomcat. Specifically:
         *  <ul>
         *    <li>Disable class path scanning else the JarScanner will find all the WebSocket endpoints in the TCK
         *        rather than just the ones in the web application. Deploying all of them will create conflicts and
         *        trigger test failures.</li>
         *   <ul>
         */
        @Override
        protected synchronized void beforeStart() {
            StandardJarScanner jarScanner = new StandardJarScanner();
            jarScanner.setScanClassPath(false);
            context.setJarScanner(jarScanner);

            super.beforeStart();
        }
    }
}
