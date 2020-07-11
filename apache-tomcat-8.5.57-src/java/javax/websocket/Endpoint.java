/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.websocket;

/**
 * Endpoint节点监听器
 */
public abstract class Endpoint {

    /**
     * Event that is triggered when a new session starts.
     * 当新会话启动时触发的事件。
     *
     * @param session 新的会话
     * @param config  The configuration with which the Endpoint was
     *                configured.
     */
    public abstract void onOpen(Session session, EndpointConfig config);

    /**
     * Event that is triggered when a session has closed.
     * 当会话关闭时触发的事件。
     *
     * @param session     当前会话
     * @param closeReason 会议为什么关闭
     */
    public void onClose(Session session, CloseReason closeReason) {
        // NO-OP by default
    }

    /**
     * Event that is triggered when a protocol error occurs.
     * 当发生协议错误时触发的事件。
     *
     * @param session   当前会话
     * @param throwable 异常
     */
    public void onError(Session session, Throwable throwable) {
        // NO-OP by default
    }
}
