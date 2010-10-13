/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.registry;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.qpid.server.util.InternalBrokerBaseCase;

/**
 * QPID-1390 : Test to validate that the AuthenticationManger can successfully unregister any new SASL providers when
 * The ApplicationRegistry is closed.
 *
 * This should be expanded as QPID-1399 is implemented.
 */
public class ApplicationRegistryShutdownTest extends InternalBrokerBaseCase
{
    List<Provider> _before;

    @Override
    public void setUp() throws Exception
    {
        // Get default providers
        _before = Arrays.asList(Security.getProviders());

        //Startup the new broker and register the new providers
        super.setUp();
    }

    /**
     * QPID-1399 : Ensure that the Authentiction manager unregisters any SASL providers created during
     * ApplicationRegistry initialisation.
     */
    public void testAuthenticationMangerCleansUp() throws Exception
    {
        // Get the providers after initialisation
        List<Provider> after = Arrays.asList(Security.getProviders());

        // Find the additions
        List<Provider> additions = new ArrayList<Provider>();
        for (Provider provider : after)
        {
            if (!_before.contains(provider))
            {
                additions.add(provider);
            }
        }

        assertFalse("No new SASL mechanisms added by initialisation.", additions.isEmpty());

        //Close the registry which will perform the close the AuthenticationManager
        getRegistry().close();

        //Validate that the SASL plugins have been removed.
        List<Provider> closed = Arrays.asList(Security.getProviders());

        assertEquals("No providers unregistered", _before.size(), closed.size());

        //Ensure that the additions are not still present after close().
        for (Provider provider : closed)
        {
            assertFalse("Added provider not unregistered: " + provider.getName(), additions.contains(provider));
        }
    }
}
