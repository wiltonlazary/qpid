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
package org.apache.qpid.client;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.apache.qpid.AMQException;
import org.apache.qpid.client.message.AbstractJMSMessage;
import org.apache.qpid.client.message.MessageConverter;
import org.apache.qpid.client.protocol.AMQProtocolHandler;
import org.apache.qpid.framing.ContentBody;
import org.apache.qpid.util.UUIDGen;
import org.apache.qpid.util.UUIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicMessageProducer extends Closeable implements org.apache.qpid.jms.MessageProducer
{
    enum PublishMode { ASYNC_PUBLISH_ALL, SYNC_PUBLISH_PERSISTENT, SYNC_PUBLISH_ALL };

    protected final Logger _logger = LoggerFactory.getLogger(getClass());

    private AMQConnection _connection;

    /**
     * If true, messages will not get a timestamp.
     */
    protected boolean _disableTimestamps;

    /**
     * Priority of messages created by this producer.
     */
    private int _messagePriority = Message.DEFAULT_PRIORITY;

    /**
     * Time to live of messages. Specified in milliseconds but AMQ has 1 second resolution.
     */
    private long _timeToLive;

    /**
     * Delivery mode used for this producer.
     */
    private int _deliveryMode = DeliveryMode.PERSISTENT;

    /**
     * The Destination used for this consumer, if specified upon creation.
     */
    protected AMQDestination _destination;

    /**
     * Default encoding used for messages produced by this producer.
     */
    private String _encoding;

    /**
     * Default encoding used for message produced by this producer.
     */
    private String _mimeType;

    protected AMQProtocolHandler _protocolHandler;

    /**
     * True if this producer was created from a transacted session
     */
    private boolean _transacted;

    protected int _channelId;

    /**
     * This is an id generated by the session and is used to tie individual producers to the session. This means we
     * can deregister a producer with the session when the producer is clsoed. We need to be able to tie producers
     * to the session so that when an error is propagated to the session it can close the producer (meaning that
     * a client that happens to hold onto a producer reference will get an error if he tries to use it subsequently).
     */
    private long _producerId;

    /**
     * The session used to create this producer
     */
    protected AMQSession _session;

    private final boolean _immediate;

    private final boolean _mandatory;

    private final boolean _waitUntilSent;

    private boolean _disableMessageId;

    private UUIDGen _messageIdGenerator = UUIDs.newGenerator();

    protected String _userID;  // ref user id used in the connection.

    private static final ContentBody[] NO_CONTENT_BODIES = new ContentBody[0];

    protected PublishMode publishMode = PublishMode.ASYNC_PUBLISH_ALL;

    protected BasicMessageProducer(AMQConnection connection, AMQDestination destination, boolean transacted, int channelId,
                                   AMQSession session, AMQProtocolHandler protocolHandler, long producerId, boolean immediate, boolean mandatory,
                                   boolean waitUntilSent)
    {
        _connection = connection;
        _destination = destination;
        _transacted = transacted;
        _protocolHandler = protocolHandler;
        _channelId = channelId;
        _session = session;
        _producerId = producerId;
        if (destination != null  && !(destination instanceof AMQUndefinedDestination))
        {
            declareDestination(destination);
        }

        _immediate = immediate;
        _mandatory = mandatory;
        _waitUntilSent = waitUntilSent;
        _userID = connection.getUsername();
        setPublishMode();
    }

    void setPublishMode()
    {
        // Publish mode could be configured at destination level as well.
        // Will add support for this when we provide a more robust binding URL

        String syncPub = _connection.getSyncPublish();
        // Support for deprecated option sync_persistence
        if (syncPub.equals("persistent") || _connection.getSyncPersistence())
        {
            publishMode = PublishMode.SYNC_PUBLISH_PERSISTENT;
        }
        else if (syncPub.equals("all"))
        {
            publishMode = PublishMode.SYNC_PUBLISH_ALL;
        }

        _logger.info("MessageProducer " + toString() + " using publish mode : " + publishMode);
    }

    void resubscribe() throws AMQException
    {
        if (_destination != null && !(_destination instanceof AMQUndefinedDestination))
        {
            declareDestination(_destination);
        }
    }

    abstract void declareDestination(AMQDestination destination);

    public void setDisableMessageID(boolean b) throws JMSException
    {
        checkPreConditions();
        checkNotClosed();
        _disableMessageId = b;
    }

    public boolean getDisableMessageID() throws JMSException
    {
        checkNotClosed();

        return _disableMessageId;
    }

    public void setDisableMessageTimestamp(boolean b) throws JMSException
    {
        checkPreConditions();
        _disableTimestamps = b;
    }

    public boolean getDisableMessageTimestamp() throws JMSException
    {
        checkNotClosed();

        return _disableTimestamps;
    }

    public void setDeliveryMode(int i) throws JMSException
    {
        checkPreConditions();
        if ((i != DeliveryMode.NON_PERSISTENT) && (i != DeliveryMode.PERSISTENT))
        {
            throw new JMSException("DeliveryMode must be either NON_PERSISTENT or PERSISTENT. Value of " + i
                                   + " is illegal");
        }

        _deliveryMode = i;
    }

    public int getDeliveryMode() throws JMSException
    {
        checkNotClosed();

        return _deliveryMode;
    }

    public void setPriority(int i) throws JMSException
    {
        checkPreConditions();
        if ((i < 0) || (i > 9))
        {
            throw new IllegalArgumentException("Priority of " + i + " is illegal. Value must be in range 0 to 9");
        }

        _messagePriority = i;
    }

    public int getPriority() throws JMSException
    {
        checkNotClosed();

        return _messagePriority;
    }

    public void setTimeToLive(long l) throws JMSException
    {
        checkPreConditions();
        if (l < 0)
        {
            throw new IllegalArgumentException("Time to live must be non-negative - supplied value was " + l);
        }

        _timeToLive = l;
    }

    public long getTimeToLive() throws JMSException
    {
        checkNotClosed();

        return _timeToLive;
    }

    public Destination getDestination() throws JMSException
    {
        checkNotClosed();

        return _destination;
    }

    public void close()
    {
        _closed.set(true);
        _session.deregisterProducer(_producerId);
    }

    public void send(Message message) throws JMSException
    {
        send(message, _deliveryMode);
    }

    public void send(Message message, int deliveryMode) throws JMSException
    {
        send(message, deliveryMode, _immediate);
    }

    public void send(Message message, int deliveryMode, boolean immediate) throws JMSException
    {
        send(message, deliveryMode, _messagePriority, _timeToLive, _mandatory, immediate);
    }

    public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException
    {
        send(message, deliveryMode, priority, timeToLive, _mandatory, _immediate);
    }

    public void send(Message message, int deliveryMode, int priority, long timeToLive, boolean mandatory, boolean immediate) throws JMSException
    {
        checkPreConditions();
        checkInitialDestination();
        synchronized (_connection.getFailoverMutex())
        {
            sendImpl(_destination, message, deliveryMode, priority, timeToLive, mandatory, immediate, _waitUntilSent);
        }
    }

    public void send(Destination destination, Message message) throws JMSException
    {
        send(destination, message, _deliveryMode, _messagePriority, _timeToLive);
    }

    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive)
        throws JMSException
    {
        send((AMQDestination) destination, message, deliveryMode, priority, timeToLive, _mandatory);
    }

    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive,
                     boolean mandatory) throws JMSException
    {
        send((AMQDestination) destination, message, deliveryMode, priority, timeToLive, mandatory, _immediate);
    }

    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive,
                     boolean mandatory, boolean immediate) throws JMSException
    {
        send((AMQDestination) destination, message, deliveryMode, priority, timeToLive, mandatory, immediate, _waitUntilSent);
    }

    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive,
                     boolean mandatory, boolean immediate, boolean waitUntilSent) throws JMSException
    {
        checkPreConditions();
        checkDestination(destination);
        synchronized (_connection.getFailoverMutex())
        {
            validateDestination(destination);
            sendImpl((AMQDestination) destination, message, deliveryMode, priority, timeToLive, mandatory, immediate,
                     waitUntilSent);
        }
    }

    private AbstractJMSMessage convertToNativeMessage(Message message) throws JMSException
    {
        if (message instanceof AbstractJMSMessage)
        {
            return (AbstractJMSMessage) message;
        }
        else
        {
            AbstractJMSMessage newMessage;

            if (message instanceof BytesMessage)
            {
                newMessage = new MessageConverter(_session, (BytesMessage) message).getConvertedMessage();
            }
            else if (message instanceof MapMessage)
            {
                newMessage = new MessageConverter(_session, (MapMessage) message).getConvertedMessage();
            }
            else if (message instanceof ObjectMessage)
            {
                newMessage = new MessageConverter(_session, (ObjectMessage) message).getConvertedMessage();
            }
            else if (message instanceof TextMessage)
            {
                newMessage = new MessageConverter(_session, (TextMessage) message).getConvertedMessage();
            }
            else if (message instanceof StreamMessage)
            {
                newMessage = new MessageConverter(_session, (StreamMessage) message).getConvertedMessage();
            }
            else
            {
                newMessage = new MessageConverter(_session, message).getConvertedMessage();
            }

            if (newMessage != null)
            {
                return newMessage;
            }
            else
            {
                throw new JMSException("Unable to send message, due to class conversion error: "
                                       + message.getClass().getName());
            }
        }
    }

    private void validateDestination(Destination destination) throws JMSException
    {
        if (!(destination instanceof AMQDestination))
        {
            throw new JMSException("Unsupported destination class: "
                                   + ((destination != null) ? destination.getClass() : null));
        }

        AMQDestination amqDestination = (AMQDestination) destination;
        if(!amqDestination.isExchangeExistsChecked())
        {
            declareDestination(amqDestination);
            amqDestination.setExchangeExistsChecked(true);
        }
    }

    /**
     * The caller of this method must hold the failover mutex.
     *
     * @param destination
     * @param origMessage
     * @param deliveryMode
     * @param priority
     * @param timeToLive
     * @param mandatory
     * @param immediate
     *
     * @throws JMSException
     */
    protected void sendImpl(AMQDestination destination, Message origMessage, int deliveryMode, int priority, long timeToLive,
                            boolean mandatory, boolean immediate, boolean wait) throws JMSException
    {
        checkTemporaryDestination(destination);
        origMessage.setJMSDestination(destination);

        AbstractJMSMessage message = convertToNativeMessage(origMessage);

        if (_transacted)
        {
            if (_session.hasFailedOver() && _session.isDirty())
            {
                throw new JMSAMQException("Failover has occurred and session is dirty so unable to send.",
                                          new AMQSessionDirtyException("Failover has occurred and session is dirty " +
                                                                       "so unable to send."));
            }
        }

        UUID messageId = null;
        if (_disableMessageId)
        {
            message.setJMSMessageID((UUID)null);
        }
        else
        {
            messageId = _messageIdGenerator.generate();
            message.setJMSMessageID(messageId);
        }

        sendMessage(destination, origMessage, message, messageId, deliveryMode, priority, timeToLive, mandatory, immediate, wait);

        if (message != origMessage)
        {
            _logger.debug("Updating original message");
            origMessage.setJMSPriority(message.getJMSPriority());
            origMessage.setJMSTimestamp(message.getJMSTimestamp());
            _logger.debug("Setting JMSExpiration:" + message.getJMSExpiration());
            origMessage.setJMSExpiration(message.getJMSExpiration());
            origMessage.setJMSMessageID(message.getJMSMessageID());
        }

        if (_transacted)
        {
            _session.markDirty();
        }
    }

    abstract void sendMessage(AMQDestination destination, Message origMessage, AbstractJMSMessage message,
                              UUID messageId, int deliveryMode, int priority, long timeToLive, boolean mandatory,
                              boolean immediate, boolean wait) throws JMSException;

    private void checkTemporaryDestination(AMQDestination destination) throws JMSException
    {
        if (destination instanceof TemporaryDestination)
        {
            _logger.debug("destination is temporary destination");
            TemporaryDestination tempDest = (TemporaryDestination) destination;
            if (tempDest.getSession().isClosed())
            {
                _logger.debug("session is closed");
                throw new JMSException("Session for temporary destination has been closed");
            }

            if (tempDest.isDeleted())
            {
                _logger.debug("destination is deleted");
                throw new JMSException("Cannot send to a deleted temporary destination");
            }
        }
    }

    public void setMimeType(String mimeType) throws JMSException
    {
        checkNotClosed();
        _mimeType = mimeType;
    }

    public void setEncoding(String encoding) throws JMSException, UnsupportedEncodingException
    {
        checkNotClosed();
        _encoding = encoding;
    }

    private void checkPreConditions() throws javax.jms.IllegalStateException, JMSException
    {
        checkNotClosed();

        if ((_session == null) || _session.isClosed())
        {
            throw new javax.jms.IllegalStateException("Invalid Session");
        }
        if(_session.getAMQConnection().isClosed())
        {
            throw new javax.jms.IllegalStateException("Connection closed");
        }
    }

    private void checkInitialDestination()
    {
        if (_destination == null)
        {
            throw new UnsupportedOperationException("Destination is null");
        }
    }

    private void checkDestination(Destination suppliedDestination) throws InvalidDestinationException
    {
        if ((_destination != null) && (suppliedDestination != null))
        {
            throw new UnsupportedOperationException(
                    "This message producer was created with a Destination, therefore you cannot use an unidentified Destination");
        }

        if (suppliedDestination == null)
        {
            throw new InvalidDestinationException("Supplied Destination was invalid");
        }

    }

    public AMQSession getSession()
    {
        return _session;
    }

    public boolean isBound(AMQDestination destination) throws JMSException
    {
        return _session.isQueueBound(destination.getExchangeName(), null, destination.getRoutingKey());
    }
}
