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
package org.apache.olingo.osgi.itests.server;

import org.apache.olingo.osgi.itests.OlingoOSGiTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the Olingo Cars OData sample service deployed as an
 * OSGi WAB inside the Karaf container.
 *
 * <p>The {@code odata-server-osgi-sample} bundle is installed in
 * {@link #installCarSample()} (runs after the base features installed by
 * {@link OlingoOSGiTestSupport#startKaraf()}). HTTP endpoints are verified
 * against the container's dynamically mapped port.
 */
@Disabled("karaf image 4.4.14 is not working with Java 21, setup currently broken")
public class CarServiceTest extends OlingoOSGiTestSupport {

    private static final String CARS_CONTEXT = "/olingo-cars/cars.svc/";
    private static final String SAMPLE_BSN = "org.apache.olingo.odata-server-osgi-sample";

    @BeforeAll
    static void installCarSample() throws Exception {
        String version = readOlingoVersion();
        karaf.installBundle("mvn:org.apache.olingo/odata-server-osgi-sample/" + version);
        // Poll until the WAB is fully deployed (max 60 s)
        for (int i = 0; i < 30; i++) {
            if (karaf.isBundleActive(SAMPLE_BSN)) {
                break;
            }
            Thread.sleep(2000);
        }
    }

    @Test
    void testServiceStarted() throws Exception {
        assertBundleStarted(SAMPLE_BSN);

        String base = karaf.getHttpBaseUrl() + CARS_CONTEXT;
        verifyContent(getContent(base));
        verifyContent(getContent(base + "$metadata"));
        verifyContent(getContent(base + "Cars"));
        verifyContent(getContent(base + "Cars(1)"));
        verifyContent(getContent(base + "Cars(1)/Price"));
    }

    private static void verifyContent(String content) {
        assertNotNull(content, "Response must not be null");
        assertTrue(content.indexOf("error") < 0,
            "Response should not contain 'error': " + content);
    }

    private static String getContent(String target) throws Exception {
        URLConnection conn = new URI(target).toURL().openConnection();
        conn.setRequestProperty("Accept", "*/*");
        try (InputStream in = conn.getInputStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString("UTF-8");
        }
    }
}
