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
 */

/**
 * \file MockPersistableQueue.h
 */

#ifndef tests_storePerftools_asyncPerf_MockPersistableQueue_h_
#define tests_storePerftools_asyncPerf_MockPersistableQueue_h_

#include "AtomicCounter.h" // AsyncOpCounter

#include "qpid/broker/AsyncStore.h" // qpid::broker::DataSource
#include "qpid/broker/PersistableQueue.h"
#include "qpid/broker/QueueHandle.h"
#include "qpid/sys/Monitor.h"

#include <boost/shared_ptr.hpp>
#include <boost/enable_shared_from_this.hpp>

namespace qpid {
namespace asyncStore {
class AsyncStoreImpl;
}
namespace broker {
class AsyncResultQueue;
}
namespace framing {
class FieldTable;
}}

namespace tests {
namespace storePerftools {
namespace asyncPerf {

class Messages;
class MockPersistableMessage;
class MockPersistableQueue;
class MockTransactionContext;
class QueueAsyncContext;
class QueuedMessage;

class MockPersistableQueue : public boost::enable_shared_from_this<MockPersistableQueue>,
                             public qpid::broker::PersistableQueue,
                             public qpid::broker::DataSource
{
public:
    MockPersistableQueue(const std::string& name,
                         const qpid::framing::FieldTable& args,
                         qpid::asyncStore::AsyncStoreImpl* store,
                         qpid::broker::AsyncResultQueue& rq);
    virtual ~MockPersistableQueue();

//    static void handleAsyncResult(const qpid::broker::AsyncResult* res,
//                                  qpid::broker::BrokerAsyncContext* bc);
    const qpid::broker::QueueHandle& getHandle() const;
    qpid::broker::QueueHandle& getHandle();
    qpid::asyncStore::AsyncStoreImpl* getStore();

    void asyncCreate();
    void asyncDestroy(const bool deleteQueue);

    // --- Methods in msg handling path from qpid::Queue ---
    void deliver(boost::shared_ptr<MockPersistableMessage> msg);
    bool dispatch(); // similar to qpid::broker::Queue::distpatch(Consumer&) but without Consumer param
    bool enqueue(MockTransactionContext* ctxt,
                 QueuedMessage& qm);
    bool dequeue(MockTransactionContext* ctxt,
                 QueuedMessage& qm);

    // --- Interface qpid::broker::Persistable ---
    virtual void encode(qpid::framing::Buffer& buffer) const;
    virtual uint32_t encodedSize() const;
    virtual uint64_t getPersistenceId() const;
    virtual void setPersistenceId(uint64_t persistenceId) const;

    // --- Interface qpid::broker::PersistableQueue ---
    virtual void flush();
    virtual const std::string& getName() const;
    virtual void setExternalQueueStore(qpid::broker::ExternalQueueStore* inst);

    // --- Interface DataStore ---
    virtual uint64_t getSize();
    virtual void write(char* target);

protected:
    const std::string m_name;
    qpid::asyncStore::AsyncStoreImpl* m_store;
    qpid::broker::AsyncResultQueue& m_resultQueue;
    AsyncOpCounter m_asyncOpCounter;
    mutable uint64_t m_persistenceId;
    std::string m_persistableData;
    qpid::broker::QueueHandle m_queueHandle;
    bool m_destroyPending;
    bool m_destroyed;

    // --- Members & methods in msg handling path copied from qpid::Queue ---
    struct UsageBarrier
    {
        MockPersistableQueue& m_parent;
        uint32_t m_count;
        qpid::sys::Monitor m_monitor;
        UsageBarrier(MockPersistableQueue& q);
        bool acquire();
        void release();
        void destroy();
    };
    struct ScopedUse
    {
        UsageBarrier& m_barrier;
        const bool m_acquired;
        ScopedUse(UsageBarrier& b);
        ~ScopedUse();
    };
    UsageBarrier m_barrier;
    std::auto_ptr<Messages> m_messages;
    void push(QueuedMessage& qm,
              bool isRecovery = false);

    // -- Async ops ---
    bool asyncEnqueue(MockTransactionContext* txn,
                      QueuedMessage& qm);
    bool asyncDequeue(MockTransactionContext* txn,
                      QueuedMessage& qm);

    // --- Async op counter ---
    void destroyCheck(const std::string& opDescr) const;

    // --- Async op completions (called through handleAsyncResult) ---
    void createComplete(const QueueAsyncContext* qc);
    void flushComplete(const QueueAsyncContext* qc);
    void destroyComplete(const QueueAsyncContext* qc);
    void enqueueComplete(const QueueAsyncContext* qc);
    void dequeueComplete(const QueueAsyncContext* qc);
};

}}} // namespace tests::storePerftools::asyncPerf

#endif // tests_storePerftools_asyncPerf_MockPersistableQueue_h_
