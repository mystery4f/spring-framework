/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.lang.Nullable;

import java.util.Iterator;

/**
 * Configuration interface to be implemented by most listable bean factories.
 * In addition to {@link ConfigurableBeanFactory}, it provides facilities to
 * analyze and modify bean definitions, and to pre-instantiate singletons.
 *
 * <p>This subinterface of {@link org.springframework.beans.factory.BeanFactory}
 * is not meant to be used in normal application code: Stick to
 * {@link org.springframework.beans.factory.BeanFactory} or
 * {@link org.springframework.beans.factory.ListableBeanFactory} for typical
 * use cases. This interface is just meant to allow for framework-internal
 * plug'n'play even when needing access to bean factory configuration methods.
 *
 * @author Juergen Hoeller
 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactory()
 * @since 03.11.2003
 */
public interface ConfigurableListableBeanFactory
		extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

	/**
	 * 忽略自动装配中的给定依赖类型。
	 * 例如，String。默认情况下没有忽略任何类型。
	 *
	 * @param type 要忽略的依赖类型
	 */
	void ignoreDependencyType(Class<?> type);

	/**
	 * 忽略自动装配中的给定依赖接口。
	 * <p>这通常由应用程序上下文用于注册以其他方式解析的依赖项，
	 * 如通过BeanFactoryAware或ApplicationContextAware解析的BeanFactory或ApplicationContext。
	 * <p>默认情况下，只有BeanFactoryAware接口被忽略。
	 * 若要忽略更多类型，请为每个类型调用此方法。
	 *
	 * @param ifc 要忽略的依赖接口
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	void ignoreDependencyInterface(Class<?> ifc);

	/**
	 * 使用相应的自动装配值注册特殊的依赖类型。
	 * <p>这是用于工厂/上下文引用的自动装配的类型，
	 * 这些引用本身不是工厂中定义的bean：
	 * 例如，一个类型为ApplicationContext的依赖会解析为
	 * 所属bean所在的ApplicationContext实例。
	 * <p>注意：在普通的BeanFactory中没有这样的默认类型注册，
	 * 甚至没有BeanFactory接口本身。
	 *
	 * @param dependencyType 要注册的依赖类型。通常这将是一个基本接口，例如BeanFactory，
	 *                       如果声明为自动装配依赖项的扩展（例如ListableBeanFactory），则其扩展也将被解析，
	 *                       只要给定的值实际上实现了扩展接口。
	 * @param autowiredValue 相应的自动装配值。这也可能是org.springframework.beans.factory.ObjectFactory
	 *                       接口的实现，允许延迟解析实际目标值。
	 */
	void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue);

	/**
	 * 确定指定的bean是否符合条件作为自动装配候选对象，
	 * 以被其他声明了匹配依赖类型的bean注入。
	 * <p>此方法也会检查父工厂。
	 *
	 * @param beanName   要检查的bean的名称
	 * @param descriptor 依赖的描述符
	 * @return 该bean是否应被视为自动装配候选对象
	 * @throws NoSuchBeanDefinitionException 如果没有找到给定名称的bean的定义
	 */
	boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException;

	/**
	 * 返回注册的BeanDefinition，允许访问其属性值和构造函数参数值
	 * （在bean工厂后处理期间可以修改）。
	 * <p>返回的BeanDefinition对象不应该是复制后的对象，而应该是工厂中注册的原始定义对象。
	 * 这意味着应该可以将其强制转换为更特定的实现类型，如果有必要的话。
	 * <p><b>注意：</b>此方法 <i>不</i> 考虑父工厂。
	 * 它仅用于访问此工厂的本地bean定义。
	 *
	 * @param beanName the name of the bean
	 * @return the registered BeanDefinition
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 *                                       defined in this factory
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回一个统一视图，包含此工厂管理的所有bean名称。
	 * <p>包括bean定义名称以及手动注册的单例实例名称，其中bean定义名称始终首先出现，
	 * 类似于按类型/注解特定检索bean名称的工作方式。
	 *
	 * @return the composite iterator for the bean names view
	 * @see #containsBeanDefinition
	 * @see #registerSingleton
	 * @see #getBeanNamesForType
	 * @see #getBeanNamesForAnnotation
	 * @since 4.1.2
	 */
	Iterator<String> getBeanNamesIterator();

	/**
	 * 清除合并的bean定义缓存，移除尚未被认为适合完整元数据缓存的bean的条目。
	 * <p>通常在应用{@link BeanFactoryPostProcessor}后触发。请注意，此时已经创建的bean的元数据将被保留。
	 *
	 * @see #getBeanDefinition
	 * @see #getMergedBeanDefinition
	 * @since 4.2
	 */
	void clearMetadataCache();

	/**
	 * 冻结所有bean定义，表示注册的bean定义不会被进一步修改或后处理。
	 * <p>这允许工厂积极缓存bean定义元数据。
	 */
	void freezeConfiguration();

	/**
	 * 返回此工厂的bean定义是否被冻结，
	 * 即是否不应该再修改或后处理bean定义。
	 *
	 * @return 如果工厂的配置被认为是冻结的，则为{@code true}
	 */
	boolean isConfigurationFrozen();

	/**
	 * 确保所有非延迟初始化的单例都被实例化，同时考虑{@link org.springframework.beans.factory.FactoryBean FactoryBeans}。
	 * 通常在工厂设置的最后调用，如果需要的话。
	 *
	 * @throws BeansException 如果无法创建其中一个单例bean。
	 *                        注意：这可能导致工厂已经初始化了一些bean！
	 *                        在这种情况下，调用{@link #destroySingletons()}进行完全清理。
	 * @see #destroySingletons()
	 */
	void preInstantiateSingletons() throws BeansException;


}
