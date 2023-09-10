/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;

import java.io.Closeable;

/**
 * SPI interface to be implemented by most if not all application contexts.
 * Provides facilities to configure an application context in addition
 * to the application context client methods in the
 * {@link org.springframework.context.ApplicationContext} interface.
 *
 * <p>Configuration and lifecycle methods are encapsulated here to avoid
 * making them obvious to ApplicationContext client code. The present
 * methods should only be used by startup and shutdown code.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 03.11.2003
 */
public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {

	/**
	 * CONFIG_LOCATION_DELIMITERS常量定义了多个上下文配置路径之间的分隔符。
	 *
	 * @see org.springframework.context.support.AbstractXmlApplicationContext#setConfigLocation
	 * @see org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
	 * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
	 */
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

	/**
	 * CONVERSION_SERVICE_BEAN_NAME常量定义了工厂中的ConversionService bean的名称。
	 * 如果没有提供，则应用默认的转换规则。
	 *
	 * @see org.springframework.core.convert.ConversionService
	 * @since 3.0
	 */
	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	/**
	 * LOAD_TIME_WEAVER_BEAN_NAME常量定义了工厂中的LoadTimeWeaver bean的名称。
	 * 如果提供了这样一个bean，上下文将使用临时的类加载器进行类型匹配，以便让LoadTimeWeaver处理所有实际的bean类。
	 *
	 * @see org.springframework.instrument.classloading.LoadTimeWeaver
	 * @since 2.5
	 */
	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

	/**
	 * ENVIRONMENT_BEAN_NAME常量定义了工厂中的Environment bean的名称。
	 *
	 * @since 3.1
	 */
	String ENVIRONMENT_BEAN_NAME = "environment";

	/**
	 * 系统属性bean在工厂中的名称。
	 *
	 * @see java.lang.System#getProperties()
	 */
	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

	/**
	 * 系统环境bean在工厂中的名称。
	 *
	 * @see java.lang.System#getenv()
	 */
	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";

	/**
	 * 工厂中应用程序启动的bean的名称。
	 *
	 * @since 5.3
	 */
	String APPLICATION_STARTUP_BEAN_NAME = "applicationStartup";

	/**
	 * {@linkplain #registerShutdownHook() 关闭钩子}线程的{@link Thread#getName() 名称}：{@value}。
	 *
	 * @see #registerShutdownHook()
	 * @since 5.2
	 */
	String SHUTDOWN_HOOK_THREAD_NAME = "SpringContextShutdownHook";


	/**
	 * 设置此应用程序上下文的唯一id。
	 *
	 * @since 3.0
	 */
	void setId(String id);

	/**
	 * 设置此应用程序上下文的父上下文。
	 * <p>注意，父上下文不应该被更改：它只应在构造函数之外设置，
	 * 如果在创建此类的对象时不可用，例如在设置WebApplicationContext时。
	 *
	 * @param parent 父上下文
	 * @see org.springframework.web.context.ConfigurableWebApplicationContext
	 */
	void setParent(@Nullable ApplicationContext parent);

	/**
	 * 返回此应用程序上下文的可配置形式的{@code Environment}，以进行进一步的自定义。
	 *
	 * @since 3.1
	 */
	@Override
	ConfigurableEnvironment getEnvironment();

	/**
	 * 设置此应用程序上下文的{@code Environment}。
	 *
	 * @param environment 新的环境
	 * @since 3.1
	 */
	void setEnvironment(ConfigurableEnvironment environment);

	/**
	 * 返回此应用程序上下文的{@link ApplicationStartup}。
	 *
	 * @since 5.3
	 */
	ApplicationStartup getApplicationStartup();

	/**
	 * 设置此应用程序上下文的{@link ApplicationStartup}。
	 * <p>这允许应用程序上下文在启动期间记录指标。
	 *
	 * @param applicationStartup 新的上下文事件工厂
	 * @since 5.3
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

	/**
	 * 添加一个新的BeanFactoryPostProcessor，将在刷新时应用于此应用程序上下文的内部bean工厂，
	 * 在任何bean定义被评估之前。在上下文配置期间调用。
	 *
	 * @param postProcessor 要注册的工厂处理器
	 */
	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

	/**
	 * 添加一个新的ApplicationListener，将在上下文事件（如上下文刷新和上下文关闭）发生时通知。
	 * <p>请注意，任何在此注册的ApplicationListener将在刷新时应用，
	 * 如果上下文尚未处于活动状态，或者在上下文已经处于活动状态的情况下使用当前事件多路广播器。
	 *
	 * @param listener 要注册的ApplicationListener
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 指定用于加载类路径资源和bean类的ClassLoader。
	 * <p>此上下文类加载器将传递给内部bean工厂。
	 *
	 * @see org.springframework.core.io.DefaultResourceLoader#DefaultResourceLoader(ClassLoader)
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setBeanClassLoader
	 * @since 5.2.7
	 */
	void setClassLoader(ClassLoader classLoader);

	/**
	 * 使用给定的协议解析器在此应用程序上下文中注册，以处理其他资源协议。
	 * <p>任何此类解析器将在此上下文的标准解析规则之前调用。因此，它也可以覆盖任何默认规则。
	 *
	 * @since 4.3
	 */
	void addProtocolResolver(ProtocolResolver resolver);

	/**
	 * 加载或刷新配置的持久表示，可以是基于 Java 的配置、XML 文件、属性文件、关系数据库模式或其他格式。
	 * <p>由于这是一个启动方法，如果失败，它应该销毁已经创建的单例，以避免悬空资源。换句话说，在调用此方法之后，要么全部实例化单例，要么不实例化任何单例。
	 *
	 * @throws BeansException        如果无法初始化 bean 工厂
	 * @throws IllegalStateException 如果已经初始化并且不支持多次刷新尝试
	 */
	void refresh() throws BeansException, IllegalStateException;

	/**
	 * 使用 JVM 运行时注册关闭挂钩，在 JVM 关闭时关闭此上下文，除非此时已经关闭。
	 * <p>此方法可以多次调用。每个上下文实例只会注册一个关闭挂钩（最多）。
	 * <p>从 Spring Framework 5.2 开始，关闭挂钩线程的{@linkplain Thread#getName() 名称}应为 {@link #SHUTDOWN_HOOK_THREAD_NAME}。
	 *
	 * @see java.lang.Runtime#addShutdownHook
	 * @see #close()
	 */
	void registerShutdownHook();

	/**
	 * 关闭此应用程序上下文，释放实现可能持有的所有资源和锁。这包括销毁所有缓存的单例 bean。
	 * <p>注意：不会在父上下文上调用 {@code close}；父上下文有自己独立的生命周期。
	 * <p>此方法可以多次调用而不产生副作用：对已关闭的上下文进行的后续 {@code close} 调用将被忽略。
	 */
	@Override
	void close();

	/**
	 * 确定此应用程序上下文是否处于活动状态，即是否已经至少刷新一次且尚未关闭。
	 *
	 * @return 上下文是否仍处于活动状态
	 * @see #refresh()
	 * @see #close()
	 * @see #getBeanFactory()
	 */
	boolean isActive();

	/**
	 * 返回此应用程序上下文的内部 bean 工厂。
	 * 可以用于访问底层工厂的特定功能。
	 * <p>注意：不要使用此方法对 bean 工厂进行后处理；单例在实例化之前已经被实例化。使用 BeanFactoryPostProcessor 在触及 bean 之前拦截 BeanFactory 设置过程。
	 * <p>通常，在上下文处于活动状态时，此内部工厂只能访问一次，即在 {@link #refresh()} 和 {@link #close()} 之间。可以使用 {@link #isActive()} 标志来检查上下文是否处于适当的状态。
	 *
	 * @return 底层的 bean 工厂
	 * @throws IllegalStateException 如果上下文不持有内部 bean 工厂（通常是如果尚未调用 {@link #refresh()} 或已经调用了 {@link #close()}）
	 * @see #isActive()
	 * @see #refresh()
	 * @see #close()
	 * @see #addBeanFactoryPostProcessor
	 */
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
