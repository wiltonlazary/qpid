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
/*
 *
 * Copyright (c) 2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.qpid;

import junit.framework.TestCase;
import org.apache.qpid.utils.JMXinfo;
import org.apache.qpid.utils.CommandLineOptionParser;
import org.apache.qpid.commands.Commandinfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.junit.After;

import javax.management.remote.JMXConnector;
import javax.management.MBeanServerConnection;

/**
 * Created by IntelliJ IDEA.
 * User: lahiru
 * Date: Jun 30, 2008
 * Time: 12:08:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestCommandLineInterpreter {

//    CommandLineInterpreter test = new CommandLineInterpreter();
    /* In this class there are only methodes which are displaying data on console so no test can be written*/
    String command = "-h " + ConnectionConstants.BROKER_HOSTNAME + " -p " + ConnectionConstants.BROKER_PORT + " info -o queue -n ping -v test";
    Connector conn= null;
    JMXConnector jmxc=null;
    MBeanServerConnection mbsc=null;
    CommandLineOptionParser parser=null;


    String [] args = null;
    @Before
    public void startup()
    {
        args = command.split(" ");
//        System.out.println(args[0]);
        conn = ConnectorFactory.getConnector(ConnectionConstants.BROKER_HOSTNAME, ConnectionConstants.BROKER_PORT);
        jmxc = conn.getConnector();
        mbsc = conn.getMBeanServerConnection();
        parser = new CommandLineOptionParser(args,args[0]);

    }
    @Test
    public void TestSetQueryString()
    {
        CommandLineInterpreter.oneshotmode(args,parser,jmxc,mbsc);
        Assert.assertEquals(args[0],"info");
        Assert.assertEquals(args[1],"-o");
        Assert.assertEquals(args[2],"queue");
        Assert.assertEquals(args[3],"-n");
        Assert.assertEquals(args[4],"ping");
        Assert.assertEquals(args[5],"-v");
        Assert.assertEquals(args[6],"test");
    }

    @After
    public void cleanup()
    {

    }
}

