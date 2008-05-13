#ifndef _broker_LinkRegistry_h
#define _broker_LinkRegistry_h

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

#include <map>
#include "Link.h"
#include "Bridge.h"
#include "MessageStore.h"
#include "Timer.h"
#include "qpid/sys/Mutex.h"
#include "qpid/management/Manageable.h"

namespace qpid {
namespace broker {

    class Broker;
    class LinkRegistry {

        // Declare a timer task to manage the establishment of link connections and the
        // re-establishment of lost link connections.
        struct Periodic : public TimerTask
        {
            LinkRegistry& links;

            Periodic(LinkRegistry& links);
            virtual ~Periodic() {};
            void fire();
        };

        typedef std::map<std::string, Link::shared_ptr> LinkMap;
        typedef std::map<std::string, Bridge::shared_ptr> BridgeMap;

        LinkMap   links;
        LinkMap   linksToDestroy;
        BridgeMap bridges;
        BridgeMap bridgesToDestroy;

        qpid::sys::Mutex lock;
        Broker* broker;
        Timer   timer;
        management::Manageable* parent;
        MessageStore* store;

        void periodicMaintenance ();

    public:
        LinkRegistry (Broker* _broker);
        std::pair<Link::shared_ptr, bool>
            declare(std::string& host,
                    uint16_t     port,
                    bool         useSsl,
                    bool         durable,
                    std::string& authMechanism,
                    std::string& username,
                    std::string& password);
        std::pair<Bridge::shared_ptr, bool>
            declare(std::string& host,
                    uint16_t     port,
                    bool         durable,
                    std::string& src,
                    std::string& dest,
                    std::string& key,
                    bool         is_queue,
                    bool         is_local,
                    std::string& id,
                    std::string& excludes);

        void destroy(const std::string& host, const uint16_t port);
        void destroy(const std::string& host,
                     const uint16_t     port,
                     const std::string& src,
                     const std::string& dest,
                     const std::string& key);

        /**
         * Register the manageable parent for declared queues
         */
        void setParent (management::Manageable* _parent) { parent = _parent; }

        /**
         * Set the store to use.  May only be called once.
         */
        void setStore (MessageStore*);

        /**
         * Return the message store used.
         */
        MessageStore* getStore() const;
    };
}
}


#endif  /*!_broker_LinkRegistry_h*/
