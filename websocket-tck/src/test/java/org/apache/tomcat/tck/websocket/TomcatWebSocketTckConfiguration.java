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

import org.apache.catalina.Host;
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
                Field hostField = Tomcat10EmbeddedContainer.class.getDeclaredField("host");
                hostField.setAccessible(true);
                Host host = (Host) hostField.get(container);
                host.setConfigClass(EmbeddedWebSocketContextConfig.class.getName());
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
