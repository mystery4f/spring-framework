/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context;

/**
 * A common interface defining methods for start/stop lifecycle control.
 * The typical use case for this is to control asynchronous processing.
 * <b>NOTE: This interface does not imply specific auto-startup semantics.
 * Consider implementing {@link SmartLifecycle} for that purpose.</b>
 *
 * <p>Can be implemented by both components (typically a Spring bean defined in a
 * Spring context) and containers  (typically a Spring {@link ApplicationContext}
 * itself). Containers will propagate start/stop signals to all components that
 * apply within each container, e.g. for a stop/restart scenario at runtime.
 *
 * <p>Can be used for direct invocations or for management operations via JMX.
 * In the latter case, the {@link org.springframework.jmx.export.MBeanExporter}
 * will typically be defined with an
 * {@link org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler},
 * restricting the visibility of activity-controlled components to the Lifecycle
 * interface.
 *
 * <p>Note that the present {@code Lifecycle} interface is only supported on
 * <b>top-level singleton beans</b>. On any other component, the {@code Lifecycle}
 * interface will remain undetected and hence ignored. Also, note that the extended
 * {@link SmartLifecycle} interface provides sophisticated integration with the
 * application context's startup and shutdown phases.
 *
 * @author Juergen Hoeller
 * @see SmartLifecycle
 * @see ConfigurableApplicationContext
 * @see org.springframework.jms.listener.AbstractMessageListenerContainer
 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean
 * @since 2.0
 */
public interface Lifecycle {
	/**
	 * 启动此组件。
	 * <p>如果组件已经在运行，则不应抛出异常。
	 * <p>对于容器而言，这将向所有适用的组件传播启动信号。
	 *
	 * @see SmartLifecycle#isAutoStartup()
	 */
	void start();

	/**
	 * 停止此组件，通常以同步方式进行，以便在此方法返回时组件完全停止。当需要异步停止行为时，考虑实现{@link SmartLifecycle}及其{@code stop(Runnable)}变体。
	 * <p>请注意，此停止通知不能保证在销毁之前到来：
	 * 在常规关闭期间，{@code Lifecycle} bean将在一般销毁回调被传播之前首先接收到停止通知；
	 * 但是，在上下文生命周期内的热刷新或刷新尝试中止时，将调用给定bean的销毁方法，而不考虑提前停止信号。
	 * <p>如果组件未运行（尚未启动），则不应抛出异常。
	 * <p>对于容器而言，这将向所有适用的组件传播停止信号。
	 *
	 * @see SmartLifecycle#stop(Runnable)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	void stop();

	/**
	 * 检查此组件当前是否正在运行。
	 * <p>对于容器而言，只有当所有适用的组件当前正在运行时，此方法才会返回{@code true}。
	 *
	 * @return 组件当前是否正在运行
	 */
	boolean isRunning();

}
