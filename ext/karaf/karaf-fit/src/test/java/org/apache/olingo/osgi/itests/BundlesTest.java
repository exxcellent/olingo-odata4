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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the core Olingo bundles reach {@code Active} state inside a
 * Karaf Docker container after the {@code olingo-server} and
 * {@code olingo-client} features are installed.
 */
@Disabled("karaf image 4.4.14 is not working with Java 21, setup currently broken")
public class BundlesTest extends OlingoOSGiTestSupport {

    @Test
    void bundlesActive() throws Exception {
        assertBundleStarted("org.apache.olingo.odata-server-api");
        assertBundleStarted("org.apache.olingo.odata-server-core");
        assertBundleStarted("org.apache.olingo.odata-client-api");
        assertBundleStarted("org.apache.olingo.odata-client-core");
    }
}
