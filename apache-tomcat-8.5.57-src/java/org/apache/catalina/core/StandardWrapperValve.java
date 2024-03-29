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
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.coyote.CloseNowException;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.log.SystemLogHandler;
import org.apache.tomcat.util.res.StringManager;

/**
 * Valve that implements the default basic behavior for the
 * <code>StandardWrapper</code> container implementation.
 *
 * @author Craig R. McClanahan
 */
final class StandardWrapperValve
        extends ValveBase {

    //------------------------------------------------------ Constructor
    public StandardWrapperValve() {
        super(true);
    }

    // ----------------------------------------------------- Instance Variables


    // Some JMX statistics. This valve is associated with a StandardWrapper.
    // We expose the StandardWrapper as JMX ( j2eeType=Servlet ). The fields
    // are here for performance.
    private volatile long processingTime;
    private volatile long maxTime;
    private volatile long minTime = Long.MAX_VALUE;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);


    /**
     * The string manager for this package.
     */
    private static final StringManager sm =
            StringManager.getManager(Constants.Package);


    // --------------------------------------------------------- Public Methods


    /**
     * Invoke the servlet we are managing, respecting the rules regarding
     * servlet lifecycle and SingleThreadModel support.
     *
     * @param request  Request to be processed
     * @param response Response to be produced
     * @throws IOException      if an input/output error occurred
     * @throws ServletException if a servlet error occurred
     */
    @Override
    public final void invoke(Request request, Response response)
            throws IOException, ServletException {

        // Initialize local variables we may need
        // 初始化我们可能需要的局部变量
        boolean unavailable = false;
        Throwable throwable = null;
        // This should be a Request attribute...
        long t1 = System.currentTimeMillis();
        // CAS 原子操作 增加请求次数
        requestCount.incrementAndGet();
        // 每次增加都会有对性的StandardWrapper和Value与之对应
        StandardWrapper wrapper = (StandardWrapper) getContainer();
        Servlet servlet = null;
        // 获取父容器Context
        Context context = (Context) wrapper.getParent();

        // Check for the application being marked unavailable
        if (!context.getState().isAvailable()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    sm.getString("standardContext.isUnavailable"));
            unavailable = true;
        }

        // Check for the servlet being marked unavailable
        // 检查servlet是否存在
        if (!unavailable && wrapper.isUnavailable()) {
            container.getLogger().info(sm.getString("standardWrapper.isUnavailable", wrapper.getName()));
            long available = wrapper.getAvailable();
            // 设置 Response 头信息
            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                response.setDateHeader("Retry-After", available);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, sm.getString("standardWrapper.isUnavailable",
                        wrapper.getName()));
            } else if (available == Long.MAX_VALUE) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, sm.getString("standardWrapper.notFound",
                        wrapper.getName()));
            }
            unavailable = true;
        }

        // 分配一个servlet实例来处理此请求
        // Servlet默认是在第一次请求的时候 实例化 类似于延迟加载
        try {
            if (!unavailable) {
                servlet = wrapper.allocate();
            }
        } catch (UnavailableException e) {
            container.getLogger().error(sm.getString("standardWrapper.allocateException", wrapper.getName()), e);
            long available = wrapper.getAvailable();
            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                response.setDateHeader("Retry-After", available);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, sm.getString("standardWrapper.isUnavailable", wrapper.getName()));
            } else if (available == Long.MAX_VALUE) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, sm.getString("standardWrapper.notFound", wrapper.getName()));
            }
        } catch (ServletException e) {
            container.getLogger().error(sm.getString("standardWrapper.allocateException", wrapper.getName()), StandardWrapper.getRootCause(e));
            throwable = e;
            exception(request, response, e);
        } catch (Throwable e) {
            ExceptionUtils.handleThrowable(e);
            container.getLogger().error(sm.getString("standardWrapper.allocateException", wrapper.getName()), e);
            throwable = e;
            exception(request, response, e);
            servlet = null;
        }
        // Get Path
        MessageBytes requestPathMB = request.getRequestPathMB();
        // 获取类型
        DispatcherType dispatcherType = DispatcherType.REQUEST;
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            dispatcherType = DispatcherType.ASYNC;
        }
        request.setAttribute(Globals.DISPATCHER_TYPE_ATTR, dispatcherType);
        request.setAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR, requestPathMB);
        // Create the filter chain for this request
        // 创建过滤链
        ApplicationFilterChain filterChain = ApplicationFilterFactory.createFilterChain(request, wrapper, servlet);

        // Call the filter chain for this request
        // NOTE: This also calls the servlet's service() method
        // 为此请求调用筛选器链
        // 注意:这也会调用servlet的service()方法
        try {
            if ((servlet != null) && (filterChain != null)) {
                // Swallow output if needed
                if (context.getSwallowOutput()) {
                    try {
                        SystemLogHandler.startCapture();
                        if (request.isAsyncDispatching()) {
                            request.getAsyncContextInternal().doInternalDispatch();
                        } else {
                            // 执行过滤链
                            filterChain.doFilter(request.getRequest(), response.getResponse());
                        }
                    } finally {
                        String log = SystemLogHandler.stopCapture();
                        if (log != null && log.length() > 0) {
                            context.getLogger().info(log);
                        }
                    }
                } else {
                    if (request.isAsyncDispatching()) {
                        request.getAsyncContextInternal().doInternalDispatch();
                    } else {
                        filterChain.doFilter(request.getRequest(), response.getResponse());
                    }
                }

            }
        } catch (ClientAbortException | CloseNowException e) {
            if (container.getLogger().isDebugEnabled()) {
                container.getLogger().debug(sm.getString("standardWrapper.serviceException", wrapper.getName(), context.getName()), e);
            }
            throwable = e;
            exception(request, response, e);
        } catch (IOException e) {
            container.getLogger().error(sm.getString("standardWrapper.serviceException", wrapper.getName(), context.getName()), e);
            throwable = e;
            exception(request, response, e);
        } catch (UnavailableException e) {
            container.getLogger().error(sm.getString("standardWrapper.serviceException", wrapper.getName(), context.getName()), e);
            //            throwable = e;
            //            exception(request, response, e);
            wrapper.unavailable(e);
            long available = wrapper.getAvailable();
            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                response.setDateHeader("Retry-After", available);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, sm.getString("standardWrapper.isUnavailable", wrapper.getName()));
            } else if (available == Long.MAX_VALUE) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, sm.getString("standardWrapper.notFound", wrapper.getName()));
            }
            // Do not save exception in 'throwable', because we
            // do not want to do exception(request, response, e) processing
        } catch (ServletException e) {
            Throwable rootCause = StandardWrapper.getRootCause(e);
            if (!(rootCause instanceof ClientAbortException)) {
                container.getLogger().error(sm.getString("standardWrapper.serviceExceptionRoot", wrapper.getName(), context.getName(), e.getMessage()), rootCause);
            }
            throwable = e;
            exception(request, response, e);
        } catch (Throwable e) {
            ExceptionUtils.handleThrowable(e);
            container.getLogger().error(sm.getString("standardWrapper.serviceException", wrapper.getName(), context.getName()), e);
            throwable = e;
            exception(request, response, e);
        } finally {
            // Release the filter chain (if any) for this request
            // 释放资源
            if (filterChain != null) {
                filterChain.release();
            }

            // Deallocate the allocated servlet instance
            try {
                // 回收到Servlet实例池中
                if (servlet != null) {
                    wrapper.deallocate(servlet);
                }
            } catch (Throwable e) {
                ExceptionUtils.handleThrowable(e);
                container.getLogger().error(sm.getString("standardWrapper.deallocateException", wrapper.getName()), e);
                if (throwable == null) {
                    throwable = e;
                    exception(request, response, e);
                }
            }

            // If this servlet has been marked permanently unavailable,
            // unload it and release this instance
            try {
                // 卸载Wrapper
                if ((servlet != null) && (wrapper.getAvailable() == Long.MAX_VALUE)) {
                    wrapper.unload();
                }
            } catch (Throwable e) {
                ExceptionUtils.handleThrowable(e);
                container.getLogger().error(sm.getString("standardWrapper.unloadException", wrapper.getName()), e);
                if (throwable == null) {
                    exception(request, response, e);
                }
            }
            long t2 = System.currentTimeMillis();

            long time = t2 - t1;
            processingTime += time;
            if (time > maxTime) {
                maxTime = time;
            }
            if (time < minTime) {
                minTime = time;
            }
        }
    }


    // -------------------------------------------------------- Private Methods

    /**
     * Handle the specified ServletException encountered while processing
     * the specified Request to produce the specified Response.  Any
     * exceptions that occur during generation of the exception report are
     * logged and swallowed.
     *
     * @param request   The request being processed
     * @param response  The response being generated
     * @param exception The exception that occurred (which possibly wraps
     *                  a root cause exception
     */
    private void exception(Request request, Response response,
                           Throwable exception) {
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setError();
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public long getMinTime() {
        return minTime;
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public int getErrorCount() {
        return errorCount.get();
    }

    public void incrementErrorCount() {
        errorCount.incrementAndGet();
    }

    @Override
    protected void initInternal() throws LifecycleException {
        // NOOP - Don't register this Valve in JMX
    }
}
