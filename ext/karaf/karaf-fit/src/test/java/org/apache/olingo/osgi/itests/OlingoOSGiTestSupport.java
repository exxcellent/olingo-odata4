/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.olingo.osgi.itests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for Karaf OSGi integration tests using Testcontainers.
 *
 * <p>A {@link KarafContainer} is started once per concrete test class
 * ({@code @BeforeAll}/{@code @AfterAll}). The base features
 * {@code olingo-server} and {@code olingo-client} are installed from the
 * local Maven repository before any test method runs.
 *
 * <p>Subclasses may add a {@code @BeforeAll} method to install additional
 * bundles; JUnit 5 guarantees that superclass {@code @BeforeAll} methods
 * run before subclass ones.
 */
public abstract class OlingoOSGiTestSupport {

    /** Karaf container shared across all test methods in a concrete test class. */
    protected static KarafContainer karaf;

    @BeforeAll
    static void startKaraf() throws Exception {
        karaf = new KarafContainer();
        karaf.start();
        installOlingoFeatures();
    }

    @AfterAll
    static void stopKaraf() {
        if (karaf != null) {
            karaf.stop();
            karaf = null;
        }
    }

    private static void installOlingoFeatures() throws Exception {
        String version = readOlingoVersion();
        //TODO what is that url is targeting?
        String featuresUrl =
            "mvn:org.apache.olingo/odata-karaf-features/" + version + "/xml/features";
        karaf.installFeatures(featuresUrl, "olingo-server", "olingo-client");
    }

    /**
     * Read the Olingo project version from the Maven-filtered resource
     * {@code /karaf-fit.properties} on the test classpath.
     */
    protected static String readOlingoVersion() {
        try (InputStream in =
                 OlingoOSGiTestSupport.class.getResourceAsStream("/karaf-fit.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                    "/karaf-fit.properties not found on test classpath");
            }
            Properties p = new Properties();
            p.load(in);
            String version = p.getProperty("olingo.version");
            if (version == null || version.isBlank()) {
                throw new IllegalStateException(
                    "olingo.version not set in /karaf-fit.properties");
            }
            return version;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read /karaf-fit.properties", e);
        }
    }

    /**
     * Assert that a bundle with the given OSGi symbolic name is in {@code Active} state.
     */
    protected static void assertBundleStarted(String symbolicName) throws Exception {
        assertTrue(karaf.isBundleActive(symbolicName),
            "Bundle " + symbolicName + " should be ACTIVE in Karaf");
    }
}
