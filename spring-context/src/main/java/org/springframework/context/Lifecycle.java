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
 * 一个通用的接口，定义了用于控制启动/停止生命周期的方法。
 * 典型的用例是控制异步处理。
 * <b>注意：此接口不暗示特定的自动启动语义。考虑实现{@link SmartLifecycle}来实现该目的。</b>
 *
 * <p>可以由组件（通常是在Spring上下文中定义的Spring bean）和容器（通常是Spring的{@link ApplicationContext}）来实现。
 * 容器会向每个容器内的所有适用组件传播启动/停止信号，例如在运行时的停止/重启场景。
 *
 * <p>可以用于直接调用或通过JMX进行管理操作。
 * 在后一种情况下，{@link org.springframework.jmx.export.MBeanExporter}通常会被定义为一个
 * {@link org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler}，将活动控制组件的可见性限制在Lifecycle接口上。
 *
 * <p>请注意，当前的{@code Lifecycle}接口仅支持在<b>顶级单例bean</b>上。对于任何其他组件，{@code Lifecycle}接口将保持未检测状态，因此会被忽略。此外，注意扩展的{@link SmartLifecycle}接口提供了与应用程序上下文的启动和关闭阶段的复杂集成。
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
