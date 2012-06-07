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
 * \file OperationQueue.h
 */

#ifndef qpid_asyncStore_OperationQueue_h_
#define qpid_asyncStore_OperationQueue_h_

#include "AsyncOperation.h"

#include "qpid/broker/AsyncStore.h"
#include "qpid/sys/PollableQueue.h"

namespace qpid {
namespace asyncStore {

class OperationQueue
{
public:
    OperationQueue(const boost::shared_ptr<qpid::sys::Poller>& poller,
                   qpid::broker::AsyncResultQueue* resultQueue = 0);
    virtual ~OperationQueue();
    void submit(const AsyncOperation* op);

protected:
    typedef qpid::sys::PollableQueue<const AsyncOperation*> OpQueue;
    OpQueue m_opQueue;
    qpid::broker::AsyncResultQueue* m_resultQueue;

    OpQueue::Batch::const_iterator handle(const OpQueue::Batch& e);
};

}} // namespace qpid::asyncStore

#endif // qpid_asyncStore_OperationQueue_h_
