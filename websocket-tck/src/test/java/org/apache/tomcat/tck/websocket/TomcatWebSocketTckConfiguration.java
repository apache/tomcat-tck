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

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.apache.tomcat.websocket.MessagePart;
import org.apache.tomcat.websocket.Transformation;
import org.apache.tomcat.websocket.TransformationBuilder;
import org.apache.tomcat.websocket.TransformationFactory;
import org.apache.tomcat.websocket.TransformationResult;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import jakarta.websocket.Extension;
import jakarta.websocket.Extension.Parameter;

import org.jboss.arquillian.container.tomcat.embedded.EmbeddedContextConfig;
import org.jboss.arquillian.container.tomcat.embedded.Tomcat10EmbeddedContainer;

public class TomcatWebSocketTckConfiguration implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(WebSocketObserver.class);
    }

    public static class WebSocketObserver {

        @Inject
        private Instance<ProtocolMetaData> protocolMetadata;


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


        public void fixFailedDeploymentTest(@Observes final AfterDeploy afterDeploy) {
            Tomcat10EmbeddedContainer container = (Tomcat10EmbeddedContainer) afterDeploy.getDeployableContainer();
            try {
                // Obtain reference to Tomcat instance
                Field tomcatField = Tomcat10EmbeddedContainer.class.getDeclaredField("tomcat");
                tomcatField.setAccessible(true);
                Tomcat tomcat = (Tomcat) tomcatField.get(container);

                // There should be a single context
                Container[] contexts = tomcat.getHost().findChildren();
                if (contexts.length == 1) {
                    Container context = contexts[0];
                    if (LifecycleState.STOPPED == context.getState()) {
                        /*
                         * A LifecycleState of STOPPED means that the Context failed to deploy.
                         *
                         * To clean up resources associated with a failed deployment, stop() is called automatically if
                         * start() fails. One of the consequences of this is that all the Servlet mappings are removed
                         * from the Context.
                         *
                         * Because there are no servlet mappings, the HTTPContext generated in
                         * Tomcat10EmbeddedContainer.deploy() will not have any Servlet mappings. The
                         * URLResourceProvider uses the Servlet mappings to provide the context path for the URL that
                         * the client uses for the tests.
                         *
                         * For the WebSocket tests, the client needs to use the context path for every request
                         * regardless of whether the web application deployed successfully or not. Therefore, if the web
                         * application failed to deploy we manually add a default Servlet to the HTTPContext for the
                         * failed web application so that URLResourceProvider provides the correct URL to the client.
                         *
                         * This might not be the "right" way to do this with Arquillian. If not, advice from Arquillian
                         * experts on the right way to do this would be appreciated (and a PR to fix it would be
                         * fantastic!).
                         */
                        ProtocolMetaData metaData = protocolMetadata.get();
                        if (metaData != null) {
                            if (metaData.hasContext(HTTPContext.class)) {
                                HTTPContext httpContext = metaData.getContexts(HTTPContext.class).iterator().next();
                                // Confirm that there are no Servlets configured
                                if (httpContext.getServlets().size() == 0) {
                                    httpContext.add(new Servlet("default", context.getName()));
                                }
                            }
                        }
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }


        public void configureTestClients(@SuppressWarnings("unused") @Observes final BeforeClass event) {
            // Configure the NO-OP extensions required by the TCK
            TransformationFactory.getInstance().registerExtension("ext1", new NoOpTransformationBuilder("ext1"));
            TransformationFactory.getInstance().registerExtension("ext2", new NoOpTransformationBuilder("ext2"));
            TransformationFactory.getInstance().registerExtension("firstExtName", new NoOpTransformationBuilder("firstExtName"));
            TransformationFactory.getInstance().registerExtension("secondExtName", new NoOpTransformationBuilder("secondExtName"));
            TransformationFactory.getInstance().registerExtension("thirdExtName", new NoOpTransformationBuilder("thirdExtName"));
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


    static class NoOpTransformationBuilder implements TransformationBuilder {

        private final String name;

        NoOpTransformationBuilder(String name) {
            this.name = name;
        }

        @Override
        public Transformation build(List<List<Parameter>> preferences, boolean isServer) {
            return new Transformation() {

                private Transformation next;

                @Override
                public boolean validateRsvBits(int i) {
                    if (next == null) {
                        return true;
                    } else {
                        return next.validateRsvBits(i);
                    }
                }

                @Override
                public boolean validateRsv(int rsv, byte opCode) {
                    if (next == null) {
                        return true;
                    } else {
                        return next.validateRsv(rsv, opCode);
                    }
                }

                @Override
                public void setNext(Transformation t) {
                    if (next == null) {
                        this.next = t;
                    } else {
                        next.setNext(t);
                    }
                }

                @Override
                public List<MessagePart> sendMessagePart(List<MessagePart> messageParts) throws IOException {
                    return next.sendMessagePart(messageParts);
                }

                @Override
                public TransformationResult getMoreData(byte opCode, boolean fin, int rsv, ByteBuffer dest) throws IOException {
                    return next.getMoreData(opCode, fin, rsv, dest);
                }

                @Override
                public Extension getExtensionResponse() {
                    return new Extension() {

                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public List<Parameter> getParameters() {
                            return Collections.emptyList();
                        }
                    };
                }

                @Override
                public void close() {
                    next.close();
                }
            };
        }
    }
}
