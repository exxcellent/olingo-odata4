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

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

/**
 * Testcontainers wrapper for an Apache Karaf Docker container.
 *
 * <p>Karaf is started from the Docker image specified by the system property
 * {@value #IMAGE_PROP} (default: {@value #DEFAULT_IMAGE}). The local Maven
 * repository is mounted read-only so Karaf can resolve {@code mvn:} bundle URLs.
 *
 * <p><b>Note on Jakarta EE compatibility:</b> The default image
 * {@value #DEFAULT_IMAGE} uses {@code javax.*} namespaces (Karaf 4.x).
 * Olingo bundles on the {@code tech/build_for_jakarta_plus_java21} branch
 * import {@code jakarta.*} packages and will therefore stay in
 * {@code INSTALLED/RESOLVED} state rather than {@code ACTIVE} on Karaf 4.x.
 * Use {@code -Dkaraf.docker.image=apache/karaf:5.x} (or a custom Jakarta-based
 * image) when full bundle activation is required.
 */
public class KarafContainer extends GenericContainer<KarafContainer> {

    /** Exposed Karaf SSH port (used for console commands via {@code bin/client}). */
    public static final int SSH_PORT = 8101;
    /** Exposed Karaf HTTP port. */
    public static final int HTTP_PORT = 8181;

    /** System property to override the Karaf Docker image. */
    public static final String IMAGE_PROP = "karaf.docker.image";
    private static final String DEFAULT_IMAGE = "apache/karaf:4.4.12";

    /**
     * Karaf console client path inside the container.
     * Invoked in batch mode ({@code -b}) via a shell pipeline so that JLine never
     * tries to set up an interactive terminal — which would fail with "Closed" when
     * {@code execInContainer} runs without a TTY.
     */
    private static final String KARAF_CLIENT_PATH = "/opt/apache-karaf/bin/client";

    public KarafContainer() {
        this(System.getProperty(IMAGE_PROP, DEFAULT_IMAGE));
    }

    /**
     * Default {@code users.properties} content that enables the {@code karaf/karaf}
     * credentials.  The {@code apache/karaf} Docker image ships with the default user
     * commented out as a security measure; we re-enable it for test-only containers.
     */
    private static final String USERS_PROPERTIES =
        "karaf = karaf,_g_:admingroup\n" +
        "_g_\\:admingroup = group,admin,manager,viewer,systembundles,ssh\n";

    public KarafContainer(String image) {
        super(image);
        withExposedPorts(SSH_PORT, HTTP_PORT);
        // Enable karaf/karaf credentials before Karaf reads the file on startup
        withCopyToContainer(
            Transferable.of(USERS_PROPERTIES),
            "/opt/apache-karaf/etc/users.properties");
        // Wait until Karaf's initial feature set is fully deployed.
        // "Deployment finished" is logged by the features deployer thread and appears
        // reliably in every Karaf startup (unlike the interactive banner, which is
        // absent in server/Docker mode, and unlike JMX-related WARNs that only appear
        // when java.rmi.server.hostname is already set from a previous container).
        waitingFor(Wait.forLogMessage(".*Deployment finished\\..*", 1)
            .withStartupTimeout(Duration.ofMinutes(5)));
    }

    /**
     * Execute a Karaf console command and return its stdout.
     *
     * <p>The command is piped via {@code printf} into {@code bin/client -b} (batch mode).
     * Batch mode suppresses JLine's interactive-terminal setup, which would otherwise
     * fail with "Closed" when {@code execInContainer} runs without a PTY.
     *
     * @throws IllegalStateException if the command exits non-zero
     */
    public String execKarafCommand(String command) throws Exception {
        // Single-quote-escape the command so the shell doesn't interpret special chars.
        String escaped = "'" + command.replace("'", "'\\''") + "'";
        String shellCmd = "printf '%s\\n' " + escaped +
            " | " + KARAF_CLIENT_PATH + " -b -u karaf -p karaf -r 10";
        var result = execInContainer("/bin/sh", "-c", shellCmd);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException(
                "Karaf command failed [" + command + "]: exit=" + result.getExitCode()
                + "\nstdout=" + result.getStdout()
                + "\nstderr=" + result.getStderr());
        }
        return result.getStdout();
    }

    /**
     * Add a feature repository and install the named features.
     *
     * @param repoUrl      PAX URL (e.g. {@code mvn:groupId/artifactId/version/xml/features})
     * @param featureNames feature names to install from the repository
     */
    public void installFeatures(String repoUrl, String... featureNames) throws Exception {
        execKarafCommand("feature:repo-add " + repoUrl);
        for (String name : featureNames) {
            execKarafCommand("feature:install " + name);
        }
    }

    /**
     * Install a single bundle from a PAX URL and start it immediately.
     *
     * @param bundleUrl PAX URL (e.g. {@code mvn:groupId/artifactId/version})
     */
    public void installBundle(String bundleUrl) throws Exception {
        execKarafCommand("bundle:install -s " + bundleUrl);
    }

    /**
     * @return {@code true} if a bundle with the given OSGi symbolic name is in {@code Active} state
     */
    public boolean isBundleActive(String symbolicName) throws Exception {
        String output = execKarafCommand("bundle:list -s -t 0");
        for (String line : output.split("\n")) {
            if (line.contains(symbolicName) && line.contains("Active")) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the HTTP base URL using the container's dynamically mapped port,
     *         e.g. {@code http://localhost:32768}
     */
    public String getHttpBaseUrl() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT);
    }
}
