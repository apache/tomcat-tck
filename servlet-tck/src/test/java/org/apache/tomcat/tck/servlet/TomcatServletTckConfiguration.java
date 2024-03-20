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

import java.lang.reflect.Field;
import java.util.Locale;

import org.apache.catalina.Container;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
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
                Connector connector = tomcat.getConnector();

                // Add trailer headers used in TCK to allow list
                connector.setProperty("allowedTrailerHeaders", "myTrailer,myTrailer2");

                // Add expected users
                tomcat.addUser("j2ee", "j2ee");
                tomcat.addRole("j2ee", "Administrator");
                tomcat.addRole("j2ee", "Employee");
                tomcat.addUser("javajoe", "javajoe");
                tomcat.addRole("javajoe", "VP");
                tomcat.addRole("javajoe", "Manager");

                // Update Arquillian configuration with port being used by Tomcat
                int localPort = connector.getLocalPort();
                Field configurationField = Tomcat10EmbeddedContainer.class.getDeclaredField("configuration");
                configurationField.setAccessible(true);
                Object configuration = configurationField.get(container);
                Field portField = container.getConfigurationClass().getDeclaredField("bindHttpPort");
                portField.setAccessible(true);
                portField.set(configuration, Integer.valueOf(localPort));

	            if ("https".equals(System.getProperty("arquillian.launch"))) {
	            	// Need to enabled HTTPS
	            }
            } catch (Exception e) {
                throw new RuntimeException(e);
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
}
