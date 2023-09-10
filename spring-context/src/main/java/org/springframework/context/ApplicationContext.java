/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 *
 * <p>An ApplicationContext provides:
 * <ul>
 * <li>Bean factory methods for accessing application components.
 * Inherited from {@link org.springframework.beans.factory.ListableBeanFactory}.
 * <li>The ability to load file resources in a generic fashion.
 * Inherited from the {@link org.springframework.core.io.ResourceLoader} interface.
 * <li>The ability to publish events to registered listeners.
 * Inherited from the {@link ApplicationEventPublisher} interface.
 * <li>The ability to resolve messages, supporting internationalization.
 * Inherited from the {@link MessageSource} interface.
 * <li>Inheritance from a parent context. Definitions in a descendant context
 * will always take priority. This means, for example, that a single parent
 * context can be used by an entire web application, while each servlet has
 * its own child context that is independent of that of any other servlet.
 * </ul>
 *
 * <p>In addition to standard {@link org.springframework.beans.factory.BeanFactory}
 * lifecycle capabilities, ApplicationContext implementations detect and invoke
 * {@link ApplicationContextAware} beans as well as {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware} and {@link MessageSourceAware} beans.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * 返回此应用程序上下文的唯一标识符。
	 *
	 * @return 上下文的唯一标识符，如果没有则返回{@code null}
	 */
	@Nullable
	String getId();

	/**
	 * 返回此上下文所属的部署应用程序的名称。
	 *
	 * @return 部署应用程序的名称，如果没有则返回空字符串
	 */
	String getApplicationName();

	/**
	 * 返回此上下文的友好名称。
	 *
	 * @return 此上下文的显示名称（永远不为{@code null}）
	 */
	String getDisplayName();

	/**
	 * 返回此上下文首次加载时的时间戳。
	 *
	 * @return 上下文首次加载时的时间戳（毫秒）
	 */
	long getStartupDate();

	/**
	 * 返回父上下文，如果没有父上下文且这是上下文层次结构的根，则返回{@code null}。
	 *
	 * @return 父上下文，如果没有父上下文则返回{@code null}
	 */
	@Nullable
	ApplicationContext getParent();

	/**
	 * 为此上下文公开AutowireCapableBeanFactory功能。
	 * <p>除了初始化应用程序上下文之外，通常不会由应用程序代码使用，还可以初始化生活在应用程序上下文之外的bean实例，
	 * 将Spring bean生命周期（全部或部分）应用于它们。
	 * <p>另外，由{@link ConfigurableApplicationContext}接口公开的内部BeanFactory也提供对{@link AutowireCapableBeanFactory}接口的访问。
	 * 现有的方法主要作为ApplicationContext接口上的便利特定设施。
	 * <p><b>注意：自4.2版本起，该方法在应用程序上下文关闭后将始终抛出IllegalStateException。</b>
	 * 在当前的Spring Framework版本中，只有可刷新的应用程序上下文才会以这种方式行为；
	 * 自4.2版本以来，所有应用程序上下文实现都将需要遵守。
	 *
	 * @return 此上下文的AutowireCapableBeanFactory
	 * @throws IllegalStateException 如果上下文不支持{@link AutowireCapableBeanFactory}接口，
	 *                               或者尚未持有可自动装配的bean工厂（例如，从未调用{@code refresh()}），
	 *                               或者上下文已经关闭
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
