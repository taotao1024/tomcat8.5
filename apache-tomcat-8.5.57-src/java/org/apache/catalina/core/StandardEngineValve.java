/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Host;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

/**
 * Valve that implements the default basic behavior for the
 * <code>StandardEngine</code> container implementation.
 * <p>
 * <b>USAGE CONSTRAINT</b>:  This implementation is likely to be useful only
 * when processing HTTP requests.
 *
 * @author Craig R. McClanahan
 */
final class StandardEngineValve extends ValveBase {
    private static final Log log = LogFactory.getLog(StandardEngineValve.class);


    //------------------------------------------------------ Constructor
    public StandardEngineValve() {
        super(true);
    }


    // ----------------------------------------------------- Instance Variables

    /**
     * The string manager for this package.
     */
    private static final StringManager sm =
            StringManager.getManager(Constants.Package);


    // --------------------------------------------------------- Public Methods

    /**
     * Select the appropriate child Host to process this request,
     * based on the requested server name.  If no matching Host can
     * be found, return an appropriate HTTP error.
     *
     * @param request  Request to be processed
     * @param response Response to be produced
     * @throws IOException      if an input/output error occurred
     * @throws ServletException if a servlet error occurred
     */
    @Override
    public final void invoke(Request request, Response response)
            throws IOException, ServletException {

        // 获取 StandardHostValve.invoke()
        Host host = request.getHost();
        log.info("Host info " + host);
        if (host == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, sm.getString("standardEngine.noHost", request.getServerName()));
            return;
        }
        // 异步处理
        if (request.isAsyncSupported()) {
            request.setAsyncSupported(host.getPipeline().isAsyncSupported());
        }

        // 请求此主机处理此请求
        // StandardHost.getPipeline()-->StandardPipeline.getFirst()-->Valve.invoke(request, response);
        // StandardEngineValve-->StandardHostValve-->StandardContextValve-->StandardWrapperValve
        // StandardWrapperValve.invoke() 是最终的处理
        host.getPipeline().getFirst().invoke(request, response);

    }
}
