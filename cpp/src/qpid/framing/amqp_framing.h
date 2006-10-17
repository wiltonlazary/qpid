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
#include "qpid/framing/amqp_types.h"
#include "qpid/framing/AMQFrame.h"
#include "qpid/framing/AMQBody.h"
#include "qpid/framing/BodyHandler.h"
#include "qpid/framing/AMQMethodBody.h"
#include "qpid/framing/AMQHeaderBody.h"
#include "qpid/framing/AMQContentBody.h"
#include "qpid/framing/AMQHeartbeatBody.h"
#include "qpid/framing/amqp_methods.h"
#include "qpid/framing/InputHandler.h"
#include "qpid/framing/OutputHandler.h"
#include "qpid/framing/InitiationHandler.h"
#include "qpid/framing/ProtocolInitiation.h"
#include "qpid/framing/BasicHeaderProperties.h"
