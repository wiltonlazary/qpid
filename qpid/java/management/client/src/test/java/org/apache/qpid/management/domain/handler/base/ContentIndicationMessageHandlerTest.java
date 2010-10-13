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
package org.apache.qpid.management.domain.handler.base;

import org.apache.qpid.management.domain.model.type.Binary;
import org.apache.qpid.test.utils.QpidTestCase;

/**
 * Test case for Content indication message handler (base class).
 * 
 * @author Andrea Gazzarini
 */
public class ContentIndicationMessageHandlerTest extends QpidTestCase
{      
    /**
     * Tests the behaviour of the objectHasBeenRemoved method().
     */
    public void testObjectHasBeenRemoved() 
    {
        ContentIndicationMessageHandler mockHandler = new ContentIndicationMessageHandler()
        {
            @Override
            protected void updateDomainModel (String packageName, String className, Binary classHash, Binary objectId,
                    long timeStampOfCurrentSample, long timeObjectWasCreated, long timeObjectWasDeleted, byte[] contentData)
            {
            }
        };
        
        long deletionTimestamp = 0;
        long now = System.currentTimeMillis();
        
        assertFalse(mockHandler.objectHasBeenRemoved(deletionTimestamp, now));
        
        deletionTimestamp = now + 1000;
        assertFalse(mockHandler.objectHasBeenRemoved(deletionTimestamp, now));
        
        deletionTimestamp = now - 1000;
        assertTrue(mockHandler.objectHasBeenRemoved(deletionTimestamp, now));
    }
}