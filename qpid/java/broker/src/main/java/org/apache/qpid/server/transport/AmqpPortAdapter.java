/*
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
package org.apache.qpid.server.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.qpid.server.configuration.ServerConfiguration;
import org.apache.qpid.server.logging.actors.CurrentActor;
import org.apache.qpid.server.logging.messages.BrokerMessages;
import org.apache.qpid.server.model.Broker;
import org.apache.qpid.server.model.Protocol;
import org.apache.qpid.server.model.Transport;
import org.apache.qpid.server.model.adapter.PortAdapter;
import org.apache.qpid.server.protocol.AmqpProtocolVersion;
import org.apache.qpid.server.protocol.MultiVersionProtocolEngineFactory;
import org.apache.qpid.server.registry.IApplicationRegistry;
import org.apache.qpid.ssl.SSLContextFactory;
import org.apache.qpid.transport.NetworkTransportConfiguration;
import org.apache.qpid.transport.network.IncomingNetworkTransport;

public class AmqpPortAdapter extends PortAdapter
{
    private final IApplicationRegistry _appRegistry;
    private IncomingNetworkTransport _transport;

    public AmqpPortAdapter(UUID id, Broker broker, Map<String, Object> attributes, IApplicationRegistry appRegistry)
    {
        super(id, broker, attributes);
        _appRegistry = appRegistry;
    }

    @Override
    protected void onActivate()
    {
        Collection<Transport> transports = getTransports();
        Set<AmqpProtocolVersion> supported = convertFromModelProtocolsToAmqp(getProtocols());

        SSLContext sslContext = null;
        if (transports.contains(Transport.SSL))
        {
            sslContext = createSslContext();
        }

        AmqpProtocolVersion defaultSupportedProtocolReply = getDefaultAmqpSupportedReply();
        InetSocketAddress bindingSocketAddress = new InetSocketAddress(getBindingAddress(), getPort());

        final NetworkTransportConfiguration settings = new ServerNetworkTransportConfiguration(
                bindingSocketAddress, isTcpNoDelay(),
                getSendBufferSize(), getReceiveBufferSize(),
                isNeedClientAuth(), isWantClientAuth());

        _transport = org.apache.qpid.transport.network.Transport.getIncomingTransportInstance();
        final MultiVersionProtocolEngineFactory protocolEngineFactory = new MultiVersionProtocolEngineFactory(
                _appRegistry, supported, defaultSupportedProtocolReply);

        _transport.accept(settings, protocolEngineFactory, sslContext);
        CurrentActor.get().message(BrokerMessages.LISTENING(getTransports().toString(), getPort()));
    }

    @Override
    protected void onStop()
    {
        if (_transport != null)
        {
            CurrentActor.get().message(BrokerMessages.SHUTTING_DOWN(getTransports().toString(), getPort()));
            _transport.close();
        }
    }

    private Set<AmqpProtocolVersion> convertFromModelProtocolsToAmqp(Collection<Protocol> modelProtocols)
    {
        Set<AmqpProtocolVersion> amqpProtocols = new HashSet<AmqpProtocolVersion>();
        for (Protocol protocol : modelProtocols)
        {
            amqpProtocols.add(protocol.toAmqpProtocolVersion());
        }
        return amqpProtocols;
    }


    /** This will be delegated to the KeyStore and TrustStore model objects */
    private SSLContext createSslContext()
    {
        ServerConfiguration configuration = _appRegistry.getConfiguration();
        final String keystorePath = configuration.getConnectorKeyStorePath();
        final String keystorePassword = configuration.getConnectorKeyStorePassword();
        final String keystoreType = configuration.getConnectorKeyStoreType();
        final String keyManagerFactoryAlgorithm = configuration.getConnectorKeyManagerFactoryAlgorithm();
        final SSLContext sslContext;
        try
        {
            if(configuration.getConnectorTrustStorePath() != null)
            {
                sslContext = SSLContextFactory.buildClientContext(configuration.getConnectorTrustStorePath(),
                        configuration.getConnectorTrustStorePassword(),
                        configuration.getConnectorTrustStoreType(),
                        configuration.getConnectorTrustManagerFactoryAlgorithm(),
                        keystorePath,
                        keystorePassword, keystoreType, keyManagerFactoryAlgorithm,
                        configuration.getCertAlias());
            }
            else
            {
                sslContext = SSLContextFactory.buildServerContext(keystorePath, keystorePassword, keystoreType, keyManagerFactoryAlgorithm);
            }
        }
        catch (GeneralSecurityException e)
        {
            throw new RuntimeException("Unable to create SSLContext for key or trust store", e);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create SSLContext - unable to load key/trust store", e);
        }
        return sslContext;
    }

    /** This will be refactored later into AmqpPort model */
    private AmqpProtocolVersion getDefaultAmqpSupportedReply()
    {
        ServerConfiguration configuration = _appRegistry.getConfiguration();
        return configuration.getDefaultSupportedProtocolReply();
    }


    class ServerNetworkTransportConfiguration implements NetworkTransportConfiguration
    {
        private final InetSocketAddress _bindingSocketAddress;
        private final Boolean _tcpNoDelay;
        private final Integer _sendBufferSize;
        private final Integer _receiveBufferSize;
        private final boolean _needClientAuth;
        private final boolean _wantClientAuth;

        public ServerNetworkTransportConfiguration(
                InetSocketAddress bindingSocketAddress, boolean tcpNoDelay,
                int sendBufferSize, int receiveBufferSize,
                boolean needClientAuth, boolean wantClientAuth)
        {
            _bindingSocketAddress = bindingSocketAddress;
            _tcpNoDelay = tcpNoDelay;
            _sendBufferSize = sendBufferSize;
            _receiveBufferSize = receiveBufferSize;
            _needClientAuth = needClientAuth;
            _wantClientAuth = wantClientAuth;
        }

        @Override
        public boolean wantClientAuth()
        {
            return _wantClientAuth;
        }

        @Override
        public boolean needClientAuth()
        {
            return _needClientAuth;
        }

        @Override
        public Boolean getTcpNoDelay()
        {
            return _tcpNoDelay;
        }

        @Override
        public Integer getSendBufferSize()
        {
            return _sendBufferSize;
        }

        @Override
        public Integer getReceiveBufferSize()
        {
            return _receiveBufferSize;
        }

        @Override
        public InetSocketAddress getAddress()
        {
            return _bindingSocketAddress;
        }
    };
}
