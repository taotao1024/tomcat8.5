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
package org.apache.catalina;


/**
 * Common interface for component life cycle methods. <P>
 * 组件生命周期方法的公共接口。<P>
 * Catalina components may implement this interface (as well
 * as the appropriate interface(s) forthe functionality they
 * support) in order to provide a consistent mechanism
 * to start and stop the component.<P>
 * Catalina组件可以实现这个接口(以及相应的接口) 它们支持的功能)，
 * 以提供启动和停止组件的一致机制。<P>
 * <br>
 * The valid state transitions for components that support {@link Lifecycle}<P>
 * 支持{@link生命周期}的组件的有效状态转换<P>
 * are:
 * <pre>
 *            start()
 *  -----------------------------
 *  |                           |
 *  | init()                    |
 * NEW -»-- INITIALIZING        |
 * | |           |              |     ------------------«-----------------------
 * | |           |auto          |     |                                        |
 * | |          \|/    start() \|/   \|/     auto          auto         stop() |
 * | |      INITIALIZED --»-- STARTING_PREP --»- STARTING --»- STARTED --»---  |
 * | |         |                                                            |  |
 * | |destroy()|                                                            |  |
 * | --»-----«--    ------------------------«--------------------------------  ^
 * |     |          |                                                          |
 * |     |         \|/          auto                 auto              start() |
 * |     |     STOPPING_PREP ----»---- STOPPING ------»----- STOPPED -----»-----
 * |    \|/                               ^                     |  ^
 * |     |               stop()           |                     |  |
 * |     |       --------------------------                     |  |
 * |     |       |                                              |  |
 * |     |       |    destroy()                       destroy() |  |
 * |     |    FAILED ----»------ DESTROYING ---«-----------------  |
 * |     |                        ^     |                          |
 * |     |     destroy()          |     |auto                      |
 * |     --------»-----------------    \|/                         |
 * |                                 DESTROYED                     |
 * |                                                               |
 * |                            stop()                             |
 * ----»-----------------------------»------------------------------
 *
 * Any state can transition to FAILED.<P>
 * 任何状态都可以转换为失败。<P>
 *
 * Calling start() while a component is in states STARTING_PREP, STARTING or
 * STARTED has no effect.<P>
 * 当组件处于STARTING_PREP、STARTING、STARTED状态时调用start()启动没有效果。<P>
 *
 * Calling start() while a component is in state NEW will cause init() to be
 * called immediately after the start() method is entered.<P>
 * 当组件处于NEW状态时调用start()将导致init()为 在进入start()方法后立即调用。<P>
 *
 * Calling stop() while a component is in states STOPPING_PREP, STOPPING or
 * STOPPED has no effect.<P>
 * 当组件处于STOPPING_PREP、STOPPING、STOPPED时调用stop()停止没有效果。<P>
 *
 * Calling stop() while a component is in state NEW transitions the component
 * to STOPPED. This is typically encountered when a component fails to start and
 * does not start all its sub-components. When the component is stopped, it will
 * try to stop all sub-components - even those it didn't start.<P>
 * 在组件处于NEW状态时调用stop()停了下来。这通常在组件启动失败时遇到不会启动所有子组件。
 * 当组件停止时，它会停止尝试停止所有子组件-即使那些没有启动。<P>
 *
 * Attempting any other transition will throw {@link LifecycleException}.
 * 尝试任何其他转换将抛出{@link LifecycleException}。
 *
 * </pre>
 * The {@link LifecycleEvent}s fired during state changes are defined in the
 * methods that trigger the changed. No {@link LifecycleEvent}s are fired if the
 * attempted transition is not valid.
 *
 * @author Craig R. McClanahan
 */
public interface Lifecycle {


    // ----------------------------------------------------- Manifest Constants


    /**
     * The LifecycleEvent type for the "component before init" event(事件).
     */
    public static final String BEFORE_INIT_EVENT = "before_init";


    /**
     * The LifecycleEvent type for the "component after init" event.
     */
    public static final String AFTER_INIT_EVENT = "after_init";


    /**
     * The LifecycleEvent type for the "component start" event.
     */
    public static final String START_EVENT = "start";


    /**
     * The LifecycleEvent type for the "component before start" event.
     */
    public static final String BEFORE_START_EVENT = "before_start";


    /**
     * The LifecycleEvent type for the "component after start" event.
     */
    public static final String AFTER_START_EVENT = "after_start";


    /**
     * The LifecycleEvent type for the "component stop" event.
     */
    public static final String STOP_EVENT = "stop";


    /**
     * The LifecycleEvent type for the "component before stop" event.
     */
    public static final String BEFORE_STOP_EVENT = "before_stop";


    /**
     * The LifecycleEvent type for the "component after stop" event.
     */
    public static final String AFTER_STOP_EVENT = "after_stop";


    /**
     * The LifecycleEvent type for the "component after destroy" event.
     */
    public static final String AFTER_DESTROY_EVENT = "after_destroy";


    /**
     * The LifecycleEvent type for the "component before destroy" event.
     */
    public static final String BEFORE_DESTROY_EVENT = "before_destroy";


    /**
     * The LifecycleEvent type for the "periodic" event.
     */
    public static final String PERIODIC_EVENT = "periodic";


    /**
     * The LifecycleEvent type for the "configure_start" event. Used by those
     * components that use a separate component to perform configuration and
     * need to signal when configuration should be performed - usually after
     * {@link #BEFORE_START_EVENT} and before {@link #START_EVENT}.
     */
    public static final String CONFIGURE_START_EVENT = "configure_start";


    /**
     * The LifecycleEvent type for the "configure_stop" event. Used by those
     * components that use a separate component to perform configuration and
     * need to signal when de-configuration should be performed - usually after
     * {@link #STOP_EVENT} and before {@link #AFTER_STOP_EVENT}.
     */
    public static final String CONFIGURE_STOP_EVENT = "configure_stop";


    // --------------------------------------------------------- Public Methods


    /**
     * Add a LifecycleEvent listener to this component.
     * <p>
     * 向该组件添加生命周期事件侦听器
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener);


    /**
     * Get the life cycle listeners associated with this life cycle.
     *
     * @return An array containing the life cycle listeners associated with this
     * life cycle. If this component has no listeners registered, a
     * zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners();


    /**
     * Remove a LifecycleEvent listener from this component.
     * <p>
     * 移除该组件的一个生命周期事件监听器
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener);


    /**
     * Prepare the component for starting. This method should perform any
     * initialization required post object creation.
     * <p>
     * 准备启动组件，此方法应该执行任何操作，初始化需要post对象创建。<p>
     * The following
     * {@link LifecycleEvent}s will be fired in the following order:
     * <ol>
     * <li>INIT_EVENT: On the successful completion of component
     * initialization.</li>
     * </ol>
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that prevents this component from being used
     */
    public void init() throws LifecycleException;

    /**
     * Prepare for the beginning of active use of the public methods other than
     * property getters/setters and life cycle methods of this component. This
     * method should be called before any of the public methods other than
     * property getters/setters and life cycle methods of this component are
     * utilized. The following {@link LifecycleEvent}s will be fired in the
     * following order:
     * <ol>
     * <li>BEFORE_START_EVENT: At the beginning of the method. It is as this
     * point the state transitions to
     * {@link LifecycleState#STARTING_PREP}.</li>
     * <li>START_EVENT: During the method once it is safe to call start() for
     * any child components. It is at this point that the
     * state transitions to {@link LifecycleState#STARTING}
     * and that the public methods other than property
     * getters/setters and life cycle methods may be
     * used.</li>
     * <li>AFTER_START_EVENT: At the end of the method, immediately before it
     * returns. It is at this point that the state
     * transitions to {@link LifecycleState#STARTED}.
     * </li>
     * </ol>
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that prevents this component from being used
     */
    public void start() throws LifecycleException;


    /**
     * Gracefully terminate the active use of the public methods other than
     * property getters/setters and life cycle methods of this component. Once
     * the STOP_EVENT is fired, the public methods other than property
     * getters/setters and life cycle methods should not be used. The following
     * {@link LifecycleEvent}s will be fired in the following order:
     * <ol>
     * <li>BEFORE_STOP_EVENT: At the beginning of the method. It is at this
     * point that the state transitions to
     * {@link LifecycleState#STOPPING_PREP}.</li>
     * <li>STOP_EVENT: During the method once it is safe to call stop() for
     * any child components. It is at this point that the
     * state transitions to {@link LifecycleState#STOPPING}
     * and that the public methods other than property
     * getters/setters and life cycle methods may no longer be
     * used.</li>
     * <li>AFTER_STOP_EVENT: At the end of the method, immediately before it
     * returns. It is at this point that the state
     * transitions to {@link LifecycleState#STOPPED}.
     * </li>
     * </ol>
     * <p>
     * Note that if transitioning from {@link LifecycleState#FAILED} then the
     * three events above will be fired but the component will transition
     * directly from {@link LifecycleState#FAILED} to
     * {@link LifecycleState#STOPPING}, bypassing
     * {@link LifecycleState#STOPPING_PREP}
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that needs to be reported
     */
    public void stop() throws LifecycleException;

    /**
     * Prepare to discard the object. The following {@link LifecycleEvent}s will
     * be fired in the following order:
     * <ol>
     * <li>DESTROY_EVENT: On the successful completion of component
     * destruction.</li>
     * </ol>
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that prevents this component from being used
     */
    public void destroy() throws LifecycleException;


    /**
     * Obtain the current state of the source component.
     *
     * @return The current state of the source component.
     */
    public LifecycleState getState();


    /**
     * Obtain a textual representation of the current component state. Useful
     * for JMX. The format of this string may vary between point releases and
     * should not be relied upon to determine component state. To determine
     * component state, use {@link #getState()}.
     *
     * @return The name of the current component state.
     */
    public String getStateName();


    /**
     * Marker interface used to indicate that the instance should only be used
     * once. Calling {@link #stop()} on an instance that supports this interface
     * will automatically call {@link #destroy()} after {@link #stop()}
     * completes.
     */
    public interface SingleUse {
    }
}