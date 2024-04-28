/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.*;
import org.springframework.context.event.*;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as an "applicationEventMulticaster" bean
 * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading by extending
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overridden in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 * @since January 21, 2001
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 *
	 * @see MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * Name of the LifecycleProcessor bean in the factory.
	 * If none is supplied, a DefaultLifecycleProcessor is used.
	 *
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * Name of the ApplicationEventMulticaster bean in the factory.
	 * If none is supplied, a default SimpleApplicationEventMulticaster is used.
	 *
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	/**
	 * Boolean flag controlled by a {@code spring.spel.ignore} system property that instructs Spring to
	 * ignore SpEL, i.e. to not initialize the SpEL infrastructure.
	 * <p>The default is "false".
	 */
	private static final boolean shouldIgnoreSpel = SpringProperties.getFlag("spring.spel.ignore");


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}


	/**
	 * Logger used by this class. Available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());
	/**
	 * BeanFactoryPostProcessors to apply on refresh.
	 */
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
	/**
	 * Flag that indicates whether this context is currently active.
	 */
	private final AtomicBoolean active = new AtomicBoolean();
	/**
	 * Flag that indicates whether this context has been closed already.
	 */
	private final AtomicBoolean closed = new AtomicBoolean();
	/**
	 * Synchronization monitor for the "refresh" and "destroy".
	 */
	private final Object startupShutdownMonitor = new Object();
	/**
	 * Statically specified listeners.
	 */
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();
	/**
	 * ResourcePatternResolver used by this context.
	 */
	private final ResourcePatternResolver resourcePatternResolver;
	/**
	 * Unique id for this context, if any.
	 */
	private String id = ObjectUtils.identityToString(this);
	/**
	 * Display name.
	 */
	private String displayName = ObjectUtils.identityToString(this);
	/**
	 * Parent context.
	 */
	@Nullable
	private ApplicationContext parent;
	/**
	 * Environment used by this context.
	 */
	@Nullable
	private ConfigurableEnvironment environment;
	/**
	 * System time in milliseconds when this context started.
	 */
	private long startupDate;
	/**
	 * Reference to the JVM shutdown hook, if registered.
	 */
	@Nullable
	private Thread shutdownHook;
	/**
	 * LifecycleProcessor for managing the lifecycle of beans within this context.
	 */
	@Nullable
	private LifecycleProcessor lifecycleProcessor;
	/**
	 * MessageSource we delegate our implementation of this interface to.
	 */
	@Nullable
	private MessageSource messageSource;
	/**
	 * Helper class used in event publishing.
	 */
	@Nullable
	private ApplicationEventMulticaster applicationEventMulticaster;
	/**
	 * Application startup metrics.
	 **/
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;
	/**
	 * Local listeners registered before refresh.
	 */
	@Nullable
	private Set<ApplicationListener<?>> earlyApplicationListeners;

	/**
	 * ApplicationEvents published before the multicaster setup.
	 */
	@Nullable
	private Set<ApplicationEvent> earlyApplicationEvents;


	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 *
	 * @param parent the parent context
	 */
	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}

	/**
	 * Create a new AbstractApplicationContext with no parent.
	 */
	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}


	// ---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	// ---------------------------------------------------------------------

	/**
	 * Return the ResourcePatternResolver to use for resolving location patterns
	 * into Resource instances. Default is a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
	 * supporting Ant-style location patterns.
	 * <p>Can be overridden in subclasses, for extended resolution strategies,
	 * for example in a web environment.
	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
	 * Call the context's {@code getResources} method instead, which
	 * will delegate to the ResourcePatternResolver.
	 *
	 * @return the ResourcePatternResolver for this context
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}

	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * Set the unique id of this application context.
	 * <p>Default is the object id of the context instance, or the name
	 * of the context bean if the context is itself defined as a bean.
	 *
	 * @param id the unique id of the context
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
	 * if already available.
	 *
	 * @see #getBeanFactory()
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 *
	 * @return this application context's internal bean factory (never {@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 *                               (usually if {@link #refresh()} has never been called) or if the context has been
	 *                               closed already
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 */
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 *
	 * @param event the event to publish (may be application-specific or a
	 *              standard framework event)
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 *
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 *              or a payload object to be turned into a {@link PayloadApplicationEvent})
	 */
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * 发布给定的事件到所有监听器。
	 *
	 * @param event     要发布的时间，可以是{@link ApplicationEvent}的一个实例，或者一个负载对象，
	 *                  该对象将被转换为{@link PayloadApplicationEvent}
	 * @param eventType 已解析的事件类型，如果已知
	 * @since 4.2
	 */
	protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		// 确保事件不为null
		Assert.notNull(event, "Event must not be null");

		// 如果需要，将事件装饰为ApplicationEvent
		ApplicationEvent applicationEvent;
		if (event instanceof ApplicationEvent) {
			applicationEvent = (ApplicationEvent) event;
		} else {
			applicationEvent = new PayloadApplicationEvent<>(this, event);
			// 如果eventType为null，则从PayloadApplicationEvent获取
			if (eventType == null) {
				eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
			}
		}

		// 如果可能，立即进行事件多播 - 或者在多播器初始化后延迟进行
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		} else {
			// 使用事件多播器广播事件
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}
		// 通过父上下文也发布事件...
		if (this.parent != null) {
			// 如果父上下文是AbstractApplicationContext的实例，则使用给定的eventType发布事件
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			} else {
				// 否则，只发布事件
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * Return the internal ApplicationEventMulticaster used by the context.
	 *
	 * @return the internal ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " + "call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	/**
	 * Return the internal LifecycleProcessor used by the context.
	 *
	 * @return the internal LifecycleProcessor (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " + "call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}

	/**
	 * Return the list of BeanFactoryPostProcessors that will get applied
	 * to the internal BeanFactory.
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		this.applicationListeners.add(listener);
	}

	/**
	 * Return the list of statically specified ApplicationListeners.
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// 使用 Spring Boot 的模块，该模块维护 Spring Boot 应用程序的启动性能数据，可以调用 start() 方法开始记录性能数据。
			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");

			// * 1. 启动准备阶段：在刷新应用程序上下文之前，该方法准备该上下文的刷新。
			prepareRefresh();

			// * 2. BeanFactory 创建阶段：用于获取新鲜的 Bean 工厂。该工厂负责应用程序上下文内所有的 Bean 对象的创建、配置和管理。
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// * 3. BeanFactory 准备阶段：为应用程序上下文的使用准备 Bean 工厂。
			prepareBeanFactory(beanFactory);

			try {
				// region * 4.BeanFactory  后置处理阶段
				// 由子类实现
				// 允许应用程序上下文的子类对 Bean 工厂进行后处理。
				postProcessBeanFactory(beanFactory);

				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");
				// 调用在应用程序上下文中注册为 Bean 的工厂处理器。
				invokeBeanFactoryPostProcessors(beanFactory);
				//endregion

				// * 5. BeanFactory 注册 BeanPostProcessor 阶段
				// 注册 registerBeanPostProcessors
				registerBeanPostProcessors(beanFactory);
				beanPostProcess.end();
				// * 6. 初始化内建 Bean：MessageSource
				// 初始化该上下文的消息源，用于国际化。
				initMessageSource();

				// * 7. 初始化内建 Bean：Spring 事件广播器：用于该上下文的应用程序事件的多路广播器初始化。
				initApplicationEventMulticaster();

				// * 8. Spring 应用上下文刷新阶段
				// 由子类实现
				// 在特定的应用程序上下文子类中初始化其他特殊的 Bean 对象。
				onRefresh();

				// * 9. Spring 事件监听器注册阶段
				// 检查是否存在监听器 Bean 对象并注册它们。
				registerListeners();

				// * 10. BeanFactory 初始化完成阶段
				// 实例化所有剩余的（非懒加载的）单例 Bean 对象。
				// SmartInitializingSingleton
				finishBeanFactoryInitialization(beanFactory);

				// * 11. Spring 应用上下刷新完成阶段
				// 最后一步是发布相应的事件。
				finishRefresh();
			}
			// 如果在刷新应用程序上下文时出现 BeansException 异常，则抛出异常，并重置 'active' 标志。
			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " + "cancelling refresh attempt: " + ex);
				}

				// 销毁已创建的单例 Bean 对象，以避免留下未释放的资源。
				destroyBeans();

				// 重置 'active' 标志。
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			} finally {
				// 重置 Spring 核心中的公共虚拟机缓存，以避免在将来的单例 Bean 对象中出现元数据错误。
				resetCommonCaches();
				contextRefresh.end();
			}
		}
	}

	/**
	 * 准备刷新当前上下文，包括设置启动日期、激活标志以及初始化属性源。
	 * 该操作将上下文状态设置为活动状态，并执行必要的初始化工作。
	 */
	protected void prepareRefresh() {
		// 设置上下文启动时间并激活上下文
		this.startupDate = System.currentTimeMillis();
		this.closed.set(false);
		this.active.set(true);

		// 根据日志级别输出不同级别的日志信息
		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			} else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// 初始化属性源，例如解析外部配置文件中的占位符
		initPropertySources();

		// 验证所有必需的属性是否可以被解析，不满足则抛出异常
		getEnvironment().validateRequiredProperties();

		// 备份或重置 ApplicationListeners 以发布早期的 ApplicationEvents
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		} else {
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// 创建并初始化一个集合来存储早期的 ApplicationEvents，便于后续发布
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}


	// ---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	// ---------------------------------------------------------------------

	/**
	 * <p>Replace any stub property sources with actual instances.
	 *
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
	}

	/**
	 * 告诉子类刷新内部 BeanFactory。
	 *
	 * @return the fresh BeanFactory instance
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		refreshBeanFactory();
		return getBeanFactory();
	}

	/**
	 * 配置工厂的标准上下文特性，例如上下文的类装载器和后处理器。
	 * 该方法对 BeanFactory 进行一系列的配置，包括设置类加载器、表达式解析器、注册资源编辑器、
	 * 添加 ApplicationContextAware 处理器、忽略特定接口、注册可解析的依赖项、
	 * 检测和配置 LoadTimeWeaver、注册默认环境 Bean 等。
	 *
	 * @param beanFactory 要配置的 BeanFactory, 用于 Spring 容器中管理 Bean 的实例。
	 *                    通过配置该 BeanFactory, 可以定制 Spring 容器的行为和特性。
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 设置 Bean 工厂的类装载器，用于加载 Bean 的类和其他资源
		beanFactory.setBeanClassLoader(getClassLoader());

		// 根据是否忽略 SpEL 来决定是否设置 Bean 表达式解析器
		if (!shouldIgnoreSpel) {
			beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		}

		// 注册资源编辑器的注册器，以支持资源路径的解析
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// 添加 ApplicationContextAwareProcessor，使得 ApplicationContext 可以被注入到 Bean 实例中
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

		// 忽略一系列接口，这些接口在依赖注入时不需要考虑
		// 这样做可以优化依赖注入的过程，避免不必要的检查
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);

		// 注册 BeanFactory 和其他依赖项，以供依赖注入使用
		// 这些注册操作使得 Spring 容器在需要时可以自动注入相应的实例
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// 为内部 Bean 注册 ApplicationListenerDetector，以便在后处理时检测 ApplicationListener 实例
		// 这有助于管理应用事件的监听器
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// 检测 LoadTimeWeaver 并准备编织，如果运行在原生图像中则不进行操作
		// LoadTimeWeaver 用于在类加载时进行代码修改，以实现某些动态功能
		if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			// 添加 LoadTimeWeaverAwareProcessor 并设置临时 ClassLoader 以进行类型匹配
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// 注册默认的环境 Bean，如果它们还没有被注册的话
		// 这些 Bean 提供了与环境相关的信息和功能，如环境变量、系统属性等
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
		if (!beanFactory.containsLocalBean(APPLICATION_STARTUP_BEAN_NAME)) {
			beanFactory.registerSingleton(APPLICATION_STARTUP_BEAN_NAME, getApplicationStartup());
		}
	}


	/**
	 * 在应用程序上下文的标准初始化之后修改其内部的bean工厂。所有bean定义都将被加载，但还没有任何bean被实例化。这允许在某些应用程序上下文实现中注册特殊的BeanPostProcessors等。
	 *
	 * @param beanFactory 应用程序上下文使用的bean工厂。
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 此方法为框架预留的扩展点，用于在bean实例化之前对bean工厂进行后处理。
		// 子类可以通过重写此方法来实现特定的逻辑，例如注册自定义的BeanPostProcessor。
	}

	/**
	 * 实例化并调用所有已注册的BeanFactoryPostProcessor Bean,
	 * 尊重给定的明确顺序。
	 * <p>必须在单例实例化之前调用。
	 *
	 * @param beanFactory 可配置的列表 BeanFactory，提供 Bean 实例化和管理功能。
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// 调用所有注册的 BeanFactoryPostProcessor
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// 检测 LoadTimeWeaver 并准备进行编织，如果在此期间找到（例如，通过 ConfigurationClassPostProcessor 注册的 @Bean 方法）
		if (!NativeDetector.inNativeImage() && beanFactory.getTempClassLoader() == null && beanFactory.containsBean(
				LOAD_TIME_WEAVER_BEAN_NAME)) {
			// 为 BeanFactory 添加 BeanPostProcessor 以支持 LoadTimeWeaver
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// 设置临时类加载器以匹配上下文类型
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * Instantiate and register all BeanPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before any instantiation of application beans.
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * 初始化消息源。
	 * 如果在此上下文中未定义，则使用父级的消息源。
	 */
	protected void initMessageSource() {
		// 获取Bean工厂
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 如果Bean工厂包含名为MESSAGE_SOURCE_BEAN_NAME的本地Bean
		// 在 spring boot 中，如果没有配置 messageSource Bean，则会使用默认的 ResourceBundleMessageSource 的 BeanDefinition，会走到这里
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			// 从Bean工厂获取消息源
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// 将消息源设置为了父消息源的知晓者
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// 只有在父消息源未注册时，将父上下文设置为父消息源
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			// 如果日志级别为跟踪模式，则记录使用的消息源
			if (logger.isTraceEnabled()) {
				logger.trace("Using MessageSource [" + this.messageSource + "]");
			}
		} else {
			// 使用空的消息源以接受getMessage调用
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			// 将消息源注册为单例Bean
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isTraceEnabled()) {
				// 如果未找到名为MESSAGE_SOURCE_BEAN_NAME的Bean，则记录使用的消息源
				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * 初始化 ApplicationEventMulticaster，负责管理应用程序事件的广播。
	 * 如果当前上下文中已经定义了 ApplicationEventMulticaster，则使用已有的定义；
	 * 如果未定义，则默认使用 SimpleApplicationEventMulticaster。
	 *
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	protected void initApplicationEventMulticaster() {
		// 获取当前环境的 BeanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();

		// 检查 BeanFactory 是否包含已定义的 ApplicationEventMulticaster
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			// 从 BeanFactory 中获取 ApplicationEventMulticaster 实例
			this.applicationEventMulticaster = beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
					ApplicationEventMulticaster.class
			);
			// 日志记录：使用的 ApplicationEventMulticaster
			if (logger.isTraceEnabled()) {
				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		} else {
			// 创建并使用默认的 SimpleApplicationEventMulticaster
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			// 将新创建的 SimpleApplicationEventMulticaster 注册到 BeanFactory 中
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			// 日志记录：未找到定义，使用默认的 SimpleApplicationEventMulticaster
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " + "[" + this.applicationEventMulticaster.getClass()
						.getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化 LifecycleProcessor。
	 * 如果上下文中未定义 LifecycleProcessor，则使用 DefaultLifecycleProcessor。
	 *
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	protected void initLifecycleProcessor() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 检查是否在上下文中定义了 LifecycleProcessor
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			// 从上下文中获取 LifecycleProcessor 实例
			this.lifecycleProcessor = beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isTraceEnabled()) {
				// 记录使用的 LifecycleProcessor 实例
				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		} else {
			// 创建 DefaultLifecycleProcessor 实例
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);
			this.lifecycleProcessor = defaultProcessor;
			// 将 DefaultLifecycleProcessor 注册为单例 bean
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isTraceEnabled()) {
				// 记录使用了默认的 LifecycleProcessor 实例
				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " + "[" + this.lifecycleProcessor.getClass()
						.getSimpleName() + "]");
			}
		}
	}

	/**
	 * 此方法为模板方法，子类可以重写以添加特定上下文的刷新逻辑。
	 * 在特定bean的初始化过程中调用，即在单例实例化之前。
	 * <p>此实现为空。
	 *
	 * @throws BeansException 如果发生错误
	 * @see #refresh() 可以参考refresh()方法
	 */
	protected void onRefresh() throws BeansException {
		// 默认情况下子类无需执行任何操作。
	}

	/**
	 * 注册实现ApplicationListener接口的bean作为监听器。
	 * 此操作不影响通过其他方式添加的监听器。
	 */
	protected void registerListeners() {
		// 首先注册静态指定的监听器。
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// 此处不初始化 FactoryBeans：需要让所有常规 bean 保持未初始化状态，
		// 以便后处理器能够应用到它们！
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// 现在终于有了一个多播器，可以发布早期应用事件了...
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;
		if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * 完成此上下文的bean工厂的初始化，初始化所有剩余的单例bean。
	 * 这个方法执行一系列步骤来完成bean工厂的初始化过程，包括：
	 * 1. 初始化转换服务，如果已定义。
	 * 2. 注册默认的嵌入值解析器，如果之前没有注册BeanFactoryPostProcessor。
	 * 3. 提前初始化LoadTimeWeaverAware类型的bean，以注册它们的转换器。
	 * 4. 停止使用临时ClassLoader进行类型匹配。
	 * 5. 冻结bean定义的配置，防止进一步更改。
	 * 6. 实例化所有剩余的非延迟初始化单例bean。
	 *
	 * @param beanFactory 要初始化的bean工厂，不允许为null。
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// 初始化转换服务，如果已定义并匹配ConversionService类型。
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) && beanFactory.isTypeMatch(
				CONVERSION_SERVICE_BEAN_NAME,
				ConversionService.class
		)) {
			beanFactory.setConversionService(beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME,
					ConversionService.class
			));
		}

		// 注册默认的嵌入值解析器，用于解析注解属性值，仅当之前没有注册BeanFactoryPostProcessor时。
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// 提前初始化LoadTimeWeaverAware类型的bean，以注册它们可能提供的转换器。
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// 清理临时ClassLoader，准备进行bean实例化。
		beanFactory.setTempClassLoader(null);

		// 冻结bean定义的配置，防止后续修改。
		beanFactory.freezeConfiguration();

		// 实例化所有剩余的非延迟初始化单例bean。
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * 完成上下文的刷新，调用LifecycleProcessor的onRefresh()方法，
	 * 并发布{@link org.springframework.context.event.ContextRefreshedEvent}事件。
	 * 此方法不接受参数且无返回值。
	 */
	@SuppressWarnings("deprecation")
	protected void finishRefresh() {
		// 清除上下文级别的资源缓存（如扫描时的ASM元数据）。
		clearResourceCaches();

		// 初始化此上下文的生命周期处理器。
		initLifecycleProcessor();

		// 首先向生命周期处理器传播刷新操作。
		getLifecycleProcessor().onRefresh();

		// 发布最终事件。
		publishEvent(new ContextRefreshedEvent(this));

		// 如果NativeBeansView MBean处于活动状态，则参与其中。
		if (!NativeDetector.inNativeImage()) {
			LiveBeansView.registerApplicationContext(this);
		}
	}

	/**
	 * Cancel this context's refresh attempt, resetting the {@code active} flag
	 * after an exception got thrown.
	 *
	 * @param ex the exception that led to the cancellation
	 */
	protected void cancelRefresh(BeansException ex) {
		this.active.set(false);
	}

	/**
	 * Reset Spring's common reflection metadata caches, in particular the
	 * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
	 * and {@link CachedIntrospectionResults} caches.
	 *
	 * @see ReflectionUtils#clearCache()
	 * @see AnnotationUtils#clearCache()
	 * @see ResolvableType#clearCache()
	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
	 * @since 4.2
	 */
	protected void resetCommonCaches() {
		ReflectionUtils.clearCache();
		AnnotationUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}

	/**
	 * 注册一个名为{@code SpringContextShutdownHook}的JVM关闭钩子线程，以便在JVM关闭时关闭此上下文，
	 * 除非在那时它已经被关闭。 <p>将关闭操作委托给{@code doClose()}方法执行实际的关闭过程。
	 *
	 * @see Runtime#addShutdownHook          提供添加关闭钩子线程的能力
	 * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME  定义关闭钩子线程的名称
	 * @see #close()                         关闭上下文的方法，此方法在关闭钩子线程中被调用
	 * @see #doClose()                        执行关闭上下文的实际逻辑的方法
	 */
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// 尚未注册关闭钩子线程。
			this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						doClose();  // 在关闭钩子线程中同步调用doClose方法，确保上下文安全关闭
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);  // 向JVM注册关闭钩子线程
		}
	}

	/**
	 * Callback for destruction of this instance, originally attached
	 * to a {@code DisposableBean} implementation (not anymore in 5.0).
	 * <p>The {@link #close()} method is the native way to shut down
	 * an ApplicationContext, which this method simply delegates to.
	 *
	 * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
	 */
	@Deprecated
	public void destroy() {
		close();
	}

	// * 14. Spring 应用上下文关闭阶段
	/**
	 * 关闭此应用程序上下文，销毁其 bean 工厂中的所有 bean。
	 * <p> 委托给 {@code doClose()} 进行实际的关闭过程。
	 * 还会删除注册的 JVM 关闭挂钩，因为它不再需要。
	 *
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// 如果注册了 JVM 关闭挂钩，现在我们不再需要它：
			// 我们已经显式关闭了上下文。
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				} catch (IllegalStateException ex) {
					// 忽略 - VM 已经在关闭中
				}
			}
		}
	}

	/**
	 * 实际执行上下文关闭的操作：发布一个 ContextClosedEvent 事件并销毁该应用上下文的 bean 工厂中的单例对象。
	 *
	 * <p> 被 close() 方法和 JVM 关闭钩子调用。
	 *
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	@SuppressWarnings("deprecation")
	protected void doClose() {
		// 检查是否需要实际执行关闭操作...
		if (this.active.get() && this.closed.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this);
			}

			if (!NativeDetector.inNativeImage()) {
				LiveBeansView.unregisterApplicationContext(this);
			}

			try {
				// 发布关闭事件。
				publishEvent(new ContextClosedEvent(this));
			} catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// 停止所有 Lifecycle beans，以避免在单个销毁过程中出现延迟。
			if (this.lifecycleProcessor != null) {
				try {
					this.lifecycleProcessor.onClose();
				} catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// 销毁上下文的 BeanFactory 中的所有缓存的单例对象。
			destroyBeans();

			// 关闭上下文本身的状态。
			closeBeanFactory();

			// 如果需要，让子类进行一些最终的清理操作...
			onClose();

			// 将本地应用程序侦听器重置为预刷新状态。
			if (this.earlyApplicationListeners != null) {
				this.applicationListeners.clear();
				this.applicationListeners.addAll(this.earlyApplicationListeners);
			}

			// 切换为非活动状态。
			this.active.set(false);
		}
	}

	/**
	 * 此方法为销毁上下文管理的所有bean的模板方法。
	 * 默认实现销毁上下文中缓存的所有单例，调用{@code DisposableBean.destroy()}和/或指定的"destroy-method"。
	 * <p>可以重写此方法，在标准单例销毁之前或之后添加特定于上下文的bean销毁步骤，
	 * 而此时上下文的BeanFactory仍处于活动状态。
	 *
	 * @see #getBeanFactory() 用于获取BeanFactory的方法。
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons() 用于销毁所有单例的方法。
	 */
	protected void destroyBeans() {
	    // 销毁所有单例
	    getBeanFactory().destroySingletons();
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * The default implementation is empty.
	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
	 * this context's BeanFactory has been closed. If custom shutdown logic
	 * needs to execute while the BeanFactory is still active, override
	 * the {@link #destroyBeans()} method instead.
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}

	/**
	 * Assert that this context's BeanFactory is currently active,
	 * throwing an {@link IllegalStateException} if it isn't.
	 * <p>Invoked by all {@link BeanFactory} delegation methods that depend
	 * on an active context, i.e. in particular all bean accessor methods.
	 * <p>The default implementation checks the {@link #isActive() 'active'} status
	 * of this context overall. May be overridden for more specific checks, or for a
	 * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
	 */
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			} else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}

	/**
	 * Return a friendly name for this context.
	 *
	 * @return a display name for this context (never {@code null})
	 */
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 * <p>Default is the object id of the context instance.
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}


	// ---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	// ---------------------------------------------------------------------

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name, allowFactoryBeanInit);
	}

	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}


	// ---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	// ---------------------------------------------------------------------

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	@Override
	@Nullable
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * Set the parent of this application context.
	 * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
	 * this (child) application context environment if the parent is non-{@code null} and
	 * its environment is an instance of {@link ConfigurableEnvironment}.
	 *
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	/**
	 * Return the {@code Environment} for this application context in configurable
	 * form, allowing for further customization.
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Set the {@code Environment} for this application context.
	 * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
	 * default with this method is one option but configuration through {@link
	 * #getEnvironment()} should also be considered. In either case, such modifications
	 * should be performed <em>before</em> {@link #refresh()}.
	 *
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}


	// ---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	// ---------------------------------------------------------------------

	/**
	 * Create and return a new {@link StandardEnvironment}.
	 * <p>Subclasses may override this method in order to supply
	 * a custom {@link ConfigurableEnvironment} implementation.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 *
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	@Nullable
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext ? ((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
	}


	// ---------------------------------------------------------------------
	// Implementation of MessageSource interface
	// ---------------------------------------------------------------------

	@Override
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 *
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " + "call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	@Nullable
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext ? ((AbstractApplicationContext) getParent()).messageSource : getParent());
	}


	// ---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	// ---------------------------------------------------------------------

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	// ---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	// ---------------------------------------------------------------------

	// * 12. Spring 应用上下文启动阶段
	@Override
	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}
	// * 13. Spring 应用上下文停止阶段
	@Override
	public void stop() {
		// 获取生命周期处理器并停止
		getLifecycleProcessor().stop();
		// 发布一个上下文停止事件
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	// ---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	// ---------------------------------------------------------------------

	/**
	 * 子类必须实现此方法来执行实际的配置加载。
	 * 在任何其他初始化工作之前，{@link #refresh()} 会调用此方法。
	 * <p> 子类要么创建一个新的 bean 工厂并持有对它的引用，要么返回它持有的单个 BeanFactory 实例。在后一种情况下，如果刷新上下文超过一次，它通常会抛出 IllegalStateException。
	 *
	 * @throws BeansException        如果初始化 bean 工厂失败
	 * @throws IllegalStateException 如果已经初始化并且不支持多次刷新尝试
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * This method gets invoked by {@link #close()} after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Return information about this context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(", started on ").append(new Date(getStartupDate()));
		ApplicationContext parent = getParent();
		if (parent != null) {
			sb.append(", parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 */
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

}
