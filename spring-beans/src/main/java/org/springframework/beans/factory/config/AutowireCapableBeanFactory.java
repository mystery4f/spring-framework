/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.lang.Nullable;

import java.util.Set;

/**
 * {@link org.springframework.beans.factory.BeanFactory} 接口的扩展，由能够自动装配的 Bean 工厂实现，
 * 前提是他们希望为现有的 Bean 实例公开此功能。
 *
 * <p>这个 BeanFactory 的子接口不应该在正常应用程序代码中使用：对于典型用例，请使用
 * {@link org.springframework.beans.factory.BeanFactory} 或 {@link org.springframework.beans.factory.ListableBeanFactory}。
 *
 * <p>其他框架的集成代码可以利用此接口来进行连线并填充 Spring 无法控制生命周期的现有 Bean 实例。
 * 例如，这对于 WebWork 操作和 Tapestry 页面对象非常有用。
 *
 * <p>请注意，{@link org.springframework.context.ApplicationContext} 门面不实现此接口，
 * 因为它几乎从不被应用程序代码使用。也就是说，它也可以从应用程序上下文中获取，
 * 可通过 ApplicationContext 的 {@link org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()} 方法访问。
 *
 * <p>您还可以实现 {@link org.springframework.beans.factory.BeanFactoryAware} 接口，即使在 ApplicationContext 中运行时，
 * 也会公开内部 BeanFactory，以访问 AutowireCapableBeanFactory：
 * 只需将传入的 BeanFactory 强制转换为 AutowireCapableBeanFactory 即可。
 *
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
 * @since 04.12.2003
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

	/**
	 * 用于指示无外部定义自动装配。请注意，仍将应用BeanFactoryAware等和注释驱动的注入。
	 *
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_NO = 0;

	/**
	 * 用于指示按名称自动装配bean属性（适用于所有bean属性setter）。
	 *
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_NAME = 1;

	/**
	 * 用于指示按类型自动装配bean属性（适用于所有bean属性setter）。
	 *
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_TYPE = 2;

	/**
	 * 用于指示自动装配可以满足的最贪婪的构造函数（涉及解析适当的构造函数）。
	 *
	 * @see #createBean
	 * @see #autowire
	 */
	int AUTOWIRE_CONSTRUCTOR = 3;

	/**
	 * 用于通过对bean类进行内省来确定适当的自动装配策略的常量
	 *
	 * @see #createBean
	 * @see #autowire
	 * @deprecated 自Spring 3.0以来：如果您正在使用混合自动装配策略，请使用基于注释的自动装配，以便更清楚地划分自动装配需求。
	 */
	@Deprecated
	int AUTOWIRE_AUTODETECT = 4;

	/**
	 * 用于在初始化现有bean实例时执行“原始实例”约定的后缀：要附加到完全限定的bean类名称中，
	 * 例如“com.mypackage.MyClass.ORIGINAL”，以强制返回给定的实例，即无代理等。
	 *
	 * @see #initializeBean(Object, String)
	 * @see #applyBeanPostProcessorsBeforeInitialization(Object, String)
	 * @see #applyBeanPostProcessorsAfterInitialization(Object, String)
	 * @since 5.1
	 */
	String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";


	//-------------------------------------------------------------------------
	// Typical methods for creating and populating external bean instances
	//-------------------------------------------------------------------------

	/**
	 * 完全创建给定类的新的bean实例。
	 * <p>执行bean的完全初始化，包括所有适用的
	 * {@link BeanPostProcessor BeanPostProcessors}。
	 * <p>注意：这旨在创建一个新的实例，填充注释的字段和方法以及
	 * 应用所有标准的bean初始化回调。
	 * <p>这并不implies传统的按名称或按类型自动装配属性；
	 * 使用{@link #createBean(Class, int, boolean)}对于这些目的。
	 *
	 * @param beanClass 要创建的bean的类
	 * @return 新的bean实例
	 * @throws BeansException 如果实例化或装配失败
	 */
	<T> T createBean(Class<T> beanClass) throws BeansException;

	/**
	 * 通过应用实例化后的回调方法和bean属性后处理（例如注解驱动的注入）来填充给定的bean实例。
	 * <p>注意：这主要是为了（重新）填充带有注解的字段和方法，无论是新实例还是反序列化实例。它不意味着传统的按名称或按类型自动装配属性；
	 * 对于这些目的，请使用{@link #autowireBeanProperties}。
	 *
	 * @param existingBean 现有的bean实例
	 * @throws BeansException 如果装配失败
	 */
	void autowireBean(Object existingBean) throws BeansException;

	/**
	 * 配置给定的原始bean：自动装配bean属性，应用bean属性值，应用工厂回调（例如{@code setBeanName}和{@code setBeanFactory}），
	 * 并应用所有bean后处理器（包括可能包装给定原始bean的那些）。
	 * <p>这实际上是{@link #initializeBean}的 superset，完全应用相应bean定义指定的配置。
	 * <b>注意：此方法需要为给定名称的bean定义！</b>
	 *
	 * @param existingBean 现有的bean实例
	 * @param beanName     bean的名称，如果需要，将传递给它（必须有该名称的bean定义）
	 * @return 要使用的bean实例，原始实例或包装后的实例
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果没有具有给定名称的bean定义
	 * @throws BeansException                                                                   如果初始化失败
	 * @see #initializeBean
	 */
	Object configureBean(Object existingBean, String beanName) throws BeansException;


	//-------------------------------------------------------------------------
	// Specialized methods for fine-grained control over the bean lifecycle
	//-------------------------------------------------------------------------

	/**
	 * 完全创建给定类的新的bean实例，并使用指定的自动装配策略。
	 * 本接口中定义的所有常量在这里都受到支持。
	 * <p>执行bean的完全初始化，包括所有适用的{@link BeanPostProcessor BeanPostProcessors}。
	 * 这实际上是{@link #autowire}提供功能的超集，增加了{@link #initializeBean}行为。
	 *
	 * @param beanClass       bean要创建的类
	 * @param autowireMode    按名称或类型自动装配，使用此接口定义的常量
	 * @param dependencyCheck 是否对bean实例中的对象执行依赖检查
	 *                        （不适用于自动装配构造函数，因此在那里被忽略）
	 * @return 新的bean实例
	 * @throws BeansException 如果实例化或装配失败
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 */
	Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 使用指定的自动装配策略实例化给定类的新的bean实例。
	 * 本接口中定义的所有常量在这里都受到支持。
	 * 也可以使用{@code AUTOWIRE_NO}仅应用实例化前的回调（例如注解驱动的注入）。
	 * <p>不应用标准的{@link BeanPostProcessor BeanPostProcessors}回调，也不执行bean的任何进一步初始化。
	 * 本接口为这些目的提供了区分且细粒度的操作，例如{@link #initializeBean}。
	 * 然而，如果适用于实例的创建，将应用{@link InstantiationAwareBeanPostProcessor}回调。
	 *
	 * @param beanClass       bean要实例化的类
	 * @param autowireMode    按名称或类型自动装配，使用此接口定义的常量
	 * @param dependencyCheck 是否对bean实例中的对象执行依赖检查
	 *                        （不适用于自动装配构造函数，因此在那里被忽略）
	 * @return 新的bean实例
	 * @throws BeansException 如果实例化或装配失败
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #initializeBean
	 * @see #applyBeanPostProcessorsBeforeInitialization
	 * @see #applyBeanPostProcessorsAfterInitialization
	 */
	Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;


	/**
	 * 自动装配给定bean实例的bean属性，按名称或类型。
	 * 也可以使用{@code AUTOWIRE_NO}来仅应用后 instantiation 回调（例如，用于注解驱动的注入）。
	 * <p>不应用标准的{@link BeanPostProcessor BeanPostProcessors}回调或执行任何进一步的初始化。
	 * 此接口提供细粒度的操作，例如{@link #initializeBean}，以实现这些目的。
	 * 然而，{@link InstantiationAwareBeanPostProcessor}回调（如果适用于实例配置）将被应用。
	 *
	 * @param existingBean    现有的bean实例
	 * @param autowireMode    按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否执行对象引用依赖检查
	 * @throws BeansException 如果装配失败
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_NO
	 */
	void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 应用给定名称的bean定义的属性值到给定的bean实例。这个bean定义可以定义一个完全独立的bean，重用它的属性值，也可以只定义用于现有bean实例的属性值。
	 * <p>此方法不自动装配bean属性；它只是应用显式定义的属性值。要自动装配现有的bean实例，请使用{@link #autowireBeanProperties}方法。
	 * <b>注意：这个方法需要为给定名称的bean定义！</b>
	 * <p>不应用标准的{@link BeanPostProcessor BeanPostProcessors}回调或执行任何进一步的初始化。这个接口提供细粒度的操作，例如{@link #initializeBean}，以实现这些目的。
	 * 然而，{@link InstantiationAwareBeanPostProcessor}回调（如果适用于实例配置）将被应用。
	 *
	 * @param existingBean 现有的bean实例
	 * @param beanName     bean工厂中bean定义的名称（必须有该名称的bean定义）
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果找不到具有给定名称的bean定义
	 * @throws BeansException                                                  如果应用属性值失败
	 * @see #autowireBeanProperties
	 */
	void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;


	/**
	 * 初始化给定的原始bean，应用工厂回调（如setBeanName和setBeanFactory），
	 * 同时应用所有的bean后处理器（包括可能包装给定原始bean的处理器）。
	 * <p>注意，不需要存在给定名称的bean定义。传入的bean名称仅仅用于回调，
	 * 而不是与注册的bean定义进行匹配。
	 *
	 * @param existingBean 现有的bean实例
	 * @param beanName     bean的名称，如果需要，将传递给它（仅传递给BeanPostProcessor；
	 *                     可以遵循{@link #ORIGINAL_INSTANCE_SUFFIX}约定，以强制返回原始实例，即没有代理等）
	 * @return 要使用的bean实例，可能是原始实例或包装后的实例
	 * @throws BeansException 如果初始化失败
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object initializeBean(Object existingBean, String beanName) throws BeansException;

	/**
	 * 将给定的现有bean实例应用{@link BeanPostProcessor BeanPostProcessors}，
	 * 调用它们的{@code postProcessBeforeInitialization}方法。
	 * 返回的bean实例可能是原始实例的包装器。
	 *
	 * @param existingBean 现有的bean实例
	 * @param beanName     bean的名称，如果需要，将传递给它（仅传递给BeanPostProcessor；
	 *                     可以遵循{@link #ORIGINAL_INSTANCE_SUFFIX}约定，以强制返回原始实例，即没有代理等）
	 * @return 要使用的bean实例，可能是原始实例或包装后的实例
	 * @throws BeansException 如果任何后处理失败
	 * @see BeanPostProcessor#postProcessBeforeInitialization
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException;

	/**
	 * 将给定的现有bean实例应用{@link BeanPostProcessor BeanPostProcessors}，
	 * 调用它们的{@code postProcessAfterInitialization}方法。返回的bean实例可能是原始实例的包装器。
	 *
	 * @param existingBean 现有的bean实例
	 * @param beanName     bean的名称，如果需要，将传递给它（仅传递给BeanPostProcessor；
	 *                     可以遵循{@link #ORIGINAL_INSTANCE_SUFFIX}约定，以强制返回原始实例，即没有代理等）
	 * @return 要使用的bean实例，可能是原始实例或包装后的实例
	 * @throws BeansException 如果任何后处理失败
	 * @see BeanPostProcessor#postProcessAfterInitialization
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException;

	/**
	 * 销毁给定的bean实例（通常来自{@link #createBean}），
	 * 应用{@link org.springframework.beans.factory.DisposableBean}契约以及注册的{@link DestructionAwareBeanPostProcessor DestructionAwareBeanPostProcessors}。
	 * <p>在销毁过程中出现的任何异常都应该被捕捉并记录，而不是传播给此方法的调用者。
	 *
	 * @param existingBean 要销毁的bean实例
	 */
	void destroyBean(Object existingBean);

	//-------------------------------------------------------------------------
	// Delegate methods for resolving injection points
	//-------------------------------------------------------------------------

	/**
	 * 解析给定对象类型的唯一匹配bean实例（如果有的话），包括其bean名称。
	 * 这实际上是{@link #getBean(Class)}的一个变体，它保留了匹配实例的bean名称。
	 *
	 * @param requiredType 类型，该bean必须匹配；可以是接口或超类
	 * @return bean名称和bean实例
	 * @throws NoSuchBeanDefinitionException   如果没有找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到了多个匹配的bean
	 * @throws BeansException                  如果bean无法创建
	 * @see #getBean(Class)
	 * @since 4.3.3
	 */
	<T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

	/**
	 * 为给定的bean名称解析bean实例，为依赖项描述符提供暴露给目标工厂方法的目标。
	 * 这实际上是{@link #getBean(String, Class)}的一个变体，它支持带有{@link org.springframework.beans.factory.InjectionPoint}参数的工厂方法。
	 *
	 * @param name       要查找的bean的名称
	 * @param descriptor 依赖项描述符，用于请求注入点
	 * @return 相应的bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到具有指定名称的bean
	 * @throws BeansException                如果bean无法创建
	 * @see #getBean(String, Class)
	 * @since 5.1.5
	 */
	Object resolveBeanByName(String name, DependencyDescriptor descriptor) throws BeansException;

	/**
	 * 将指定的依赖项解析为在这个工厂中定义的bean。
	 *
	 * @param descriptor         依赖项描述符（字段/方法/构造函数）
	 * @param requestingBeanName 请求依赖项的bean的名称
	 * @return 解析的对象，如果没有找到则返回{@code null}
	 * @throws NoSuchBeanDefinitionException   如果没有找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到了多个匹配的bean
	 * @throws BeansException                  如果依赖项解析失败
	 * @see #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
	 * @since 2.5
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException;


	/**
	 * 根据此工厂中定义的bean解析指定的依赖项。
	 *
	 * @param descriptor         依赖项的描述符（字段/方法/构造函数）
	 * @param requestingBeanName 声明给定依赖项的bean的名称
	 * @param autowiredBeanNames 需要添加所有自动装配bean名称的集合（用于解决给定依赖项）
	 * @param typeConverter      用于填充数组和集合的TypeConverter
	 * @return 已解析的对象；如果没有找到则为{@code null}
	 * @throws NoSuchBeanDefinitionException   如果没有找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的bean
	 * @throws BeansException                  如果由于任何其他原因导致依赖项解析失败
	 * @see DependencyDescriptor
	 * @since 2.5
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName, @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException;

}
