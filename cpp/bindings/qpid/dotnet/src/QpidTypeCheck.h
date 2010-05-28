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
#pragma once

#include <windows.h>
#include <msclr\lock.h>
#include <oletx2xa.h>
#include <string>
#include <limits>

namespace org {
namespace apache {
namespace qpid {
namespace messaging {

    /// <summary>
    /// QpidTypeCheck determines if a given managed object represents
    /// a qpid type per the scheme presented by the messaging DLL.
    ///
    // The supported mapping is:
    /// * a managed Dictionary and a Qpid Messaging Map
    /// * a managed List       and a Qpid Messaging List
    /// </summary>

    typedef System::Collections::Generic::Dictionary<
                System::String^, 
                System::Object^> 
                    QpidMap;

    typedef System::Collections::Generic::List<
                System::Object^> 
                    QpidList;

    private ref class QpidTypeCheckConstants
    {
    public:
        static System::Type const ^ const mapTypeP = System::Type::GetType(
            "System.Collections.Generic.Dictionary`2[System.String,System.Object]");
        static System::Type const ^ const listTypeP = System::Type::GetType(
            "System.Collections.Generic.List`1[System.Object]");
    };


    public ref class QpidTypeCheck
    {

    public:

        static bool ObjectIsMap (System::Object ^ object)
        { 
            return (*object).GetType() == QpidTypeCheckConstants::mapTypeP;
        }

        static bool ObjectIsList(System::Object ^ object)
        { 
            return (*object).GetType() == QpidTypeCheckConstants::listTypeP;
        }
    };
}}}}
