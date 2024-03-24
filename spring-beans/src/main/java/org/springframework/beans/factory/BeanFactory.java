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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * The root interface for accessing a Spring bean container.
 *
 * <p>This is the basic client view of a bean container;
 * further interfaces such as {@link ListableBeanFactory} and
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * are available for specific purposes.
 *
 * <p>This interface is implemented by objects that hold a number of bean definitions,
 * each uniquely identified by a String name. Depending on the bean definition,
 * the factory will return either an independent instance of a contained object
 * (the Prototype design pattern), or a single shared instance (a superior
 * alternative to the Singleton design pattern, in which the instance is a
 * singleton in the scope of the factory). Which type of instance will be returned
 * depends on the bean factory configuration: the API is the same. Since Spring
 * 2.0, further scopes are available depending on the concrete application
 * context (e.g. "request" and "session" scopes in a web environment).
 *
 * <p>The point of this approach is that the BeanFactory is a central registry
 * of application components, and centralizes configuration of application
 * components (no more do individual objects need to read properties files,
 * for example). See chapters 4 and 11 of "Expert One-on-One J2EE Design and
 * Development" for a discussion of the benefits of this approach.
 *
 * <p>Note that it is generally better to rely on Dependency Injection
 * ("push" configuration) to configure application objects through setters
 * or constructors, rather than use any form of "pull" configuration like a
 * BeanFactory lookup. Spring's Dependency Injection functionality is
 * implemented using this BeanFactory interface and its subinterfaces.
 *
 * <p>Normally a BeanFactory will load bean definitions stored in a configuration
 * source (such as an XML document), and use the {@code org.springframework.beans}
 * package to configure the beans. However, an implementation could simply return
 * Java objects it creates as necessary directly in Java code. There are no
 * constraints on how the definitions could be stored: LDAP, RDBMS, XML,
 * properties file, etc. Implementations are encouraged to support references
 * amongst beans (Dependency Injection).
 *
 * <p>In contrast to the methods in {@link ListableBeanFactory}, all of the
 * operations in this interface will also check parent factories if this is a
 * {@link HierarchicalBeanFactory}. If a bean is not found in this factory instance,
 * the immediate parent factory will be asked. Beans in this factory instance
 * are supposed to override beans of the same name in any parent factory.
 *
 * <p>Bean factory implementations should support the standard bean lifecycle interfaces
 * as far as possible. The full set of initialization methods and their standard order is:
 * <ol>
 * <li>BeanNameAware's {@code setBeanName}
 * <li>BeanClassLoaderAware's {@code setBeanClassLoader}
 * <li>BeanFactoryAware's {@code setBeanFactory}
 * <li>EnvironmentAware's {@code setEnvironment}
 * <li>EmbeddedValueResolverAware's {@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware's {@code setResourceLoader}
 * (only applicable when running in an application context)
 * <li>ApplicationEventPublisherAware's {@code setApplicationEventPublisher}
 * (only applicable when running in an application context)
 * <li>MessageSourceAware's {@code setMessageSource}
 * (only applicable when running in an application context)
 * <li>ApplicationContextAware's {@code setApplicationContext}
 * (only applicable when running in an application context)
 * <li>ServletContextAware's {@code setServletContext}
 * (only applicable when running in a web application context)
 * <li>{@code postProcessBeforeInitialization} methods of BeanPostProcessors
 * <li>InitializingBean's {@code afterPropertiesSet}
 * <li>a custom {@code init-method} definition
 * <li>{@code postProcessAfterInitialization} methods of BeanPostProcessors
 * </ol>
 *
 * <p>On shutdown of a bean factory, the following lifecycle methods apply:
 * <ol>
 * <li>{@code postProcessBeforeDestruction} methods of DestructionAwareBeanPostProcessors
 * <li>DisposableBean's {@code destroy}
 * <li>a custom {@code destroy-method} definition
 * </ol>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see BeanNameAware#setBeanName
 * @see BeanClassLoaderAware#setBeanClassLoader
 * @see BeanFactoryAware#setBeanFactory
 * @see org.springframework.context.EnvironmentAware#setEnvironment
 * @see org.springframework.context.EmbeddedValueResolverAware#setEmbeddedValueResolver
 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader
 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher
 * @see org.springframework.context.MessageSourceAware#setMessageSource
 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
 * @see org.springframework.web.context.ServletContextAware#setServletContext
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization
 * @see InitializingBean#afterPropertiesSet
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getInitMethodName
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor#postProcessBeforeDestruction
 * @see DisposableBean#destroy
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName
 * @since 13 April 2001
 */
public interface BeanFactory {

	/**
	 * 用于取消引用{@link FactoryBean}实例并将其与由FactoryBean创建的bean区分开来。
	 * 例如，如果名为{@code myJndiObject}的bean是一个FactoryBean，
	 * 获取{@code &myJndiObject}将返回工厂，而不是工厂创建的实例。
	 */
	String FACTORY_BEAN_PREFIX = "&";


	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p>此方法允许Spring BeanFactory用作Singleton或Prototype设计模式的替代品。
	 * 调用者可以在Singleton bean的情况下保留对返回对象的引用。
	 * <p>将别名翻译回相应的规范bean名称。
	 * <p>如果在本工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要检索的bean的名称
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException 如果没有具有指定名称的bean
	 * @throws BeansException                如果无法获取bean
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * 返回指定bean的一个实例，该实例可能是共享的或独立的。
	 * <p>与{@link #getBean(String)}, 行为相同，但通过抛出BeanNotOfRequiredTypeException来提供一定的类型安全性，
	 * 如果bean不是所需类型，这种情况可能会导致ClassCastException。
	 * 这意味着正确地强制转换结果时不会抛出ClassCastException。
	 * <p>将别名转换回相应的规范bean名称。
	 * <p>如果在本工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name         要检索的bean的名称
	 * @param requiredType 类型必须匹配的bean；可以是接口或父类
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException  如果没有这样的bean定义
	 * @throws BeanNotOfRequiredTypeException 如果bean不是所需类型
	 * @throws BeansException                 如果bean不能被创建
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p>允许指定显式的构造函数参数/工厂方法参数，
	 * 覆盖bean定义中指定的默认参数（如果有的话）。
	 *
	 * @param name 要检索的bean的名称
	 * @param args 使用显式参数创建bean实例时使用的参数
	 *             （仅当创建新实例而不是检索现有实例时应用）
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException 如果找不到这样的bean定义
	 * @throws BeanDefinitionStoreException  如果给出了参数但受影响的bean不是一个原型
	 * @throws BeansException                如果无法创建bean
	 * @since 2.5
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * 返回与给定对象类型匹配的唯一bean实例（如果存在）。
	 * <p>此方法进入{@link ListableBeanFactory}的类型查找领域，
	 * 但也可以根据给定类型的名称翻译成传统的按名称查找。
	 * 对于更多的bean检索操作，请使用{@link ListableBeanFactory}和/或{@link BeanFactoryUtils}。
	 *
	 * @param requiredType 类型，bean必须与之匹配；可以是接口或父类
	 * @return 匹配所需类型的bean实例
	 * @throws NoSuchBeanDefinitionException   如果没有找到给定类型的bean，则抛出此异常
	 * @throws NoUniqueBeanDefinitionException 如果找到了多个给定类型的bean，则抛出此异常
	 * @throws BeansException                  如果bean无法创建，则抛出此异常
	 * @see ListableBeanFactory
	 * @since 3.0
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定bean的一个实例，该实例可能是共享的或独立的。
	 * <p>允许指定显式的构造函数参数/工厂方法参数，
	 * 覆盖bean定义中指定的默认参数（如果有的话）。
	 * <p>此方法进入{@link ListableBeanFactory} by-type查找领域
	 * 但也可以转换为基于给定类型的常规by-name查找。
	 * 对于更广泛的检索操作，请使用{@link ListableBeanFactory}和/或{@link BeanFactoryUtils}。
	 *
	 * @param requiredType 必须匹配的bean类型；可以是接口或超类
	 * @param args         使用显式参数创建bean实例时使用的参数（仅在创建新实例时应用，而不是检索现有实例）
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException 如果找不到这样的bean定义
	 * @throws BeanDefinitionStoreException  如果给出了参数但受影响的bean不是原型
	 * @throws BeansException                如果bean无法创建
	 * @since 4.1
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

	/**
	 * 返回指定bean的提供者，允许在需要时进行实例的延迟获取，包括可用性和唯一性选项。
	 * <p>对于匹配通用类型，请考虑使用{@link #getBeanProvider(ResolvableType)}。
	 *
	 * @param requiredType 类型bean必须匹配；可以是一个接口或超类
	 * @return 相应的提供者句柄
	 * @see #getBeanProvider(ResolvableType)
	 * @since 5.1
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

	/**
	 * 返回指定bean的提供者，允许在需要时进行实例的延迟获取，包括可用性和唯一性选项。
	 * 这个变体允许指定通用类型进行匹配，类似于反射注入点中的泛型类型声明
	 * 在方法/构造函数参数中。
	 * <p>请注意，这里不支持集合bean，与反射注入点不同。对于程序化检索与特定类型匹配的bean列表，
	 * 请在此处指定实际bean类型，随后使用{@link ObjectProvider#orderedStream()}或其
	 * 延迟流/迭代选项。
	 * <p>此外，泛型匹配在这里是严格的，遵循Java赋值规则。
	 * 对于带有未检查语义的宽松匹配（类似于
	 * ´unchecked´Java编译警告），可以考虑在
	 * {@link #getBeanProvider(Class)}中以原始类型调用，作为第二步，
	 * 如果此变体中没有完全的泛型匹配
	 * {@link ObjectProvider#getIfAvailable()}可用。
	 *
	 * @param requiredType 类型bean必须匹配；可以是一个泛型类型声明
	 * @return 相应的提供者句柄
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @since 5.1
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

	/**
	 * 检查此bean工厂是否包含具有给定名称的bean定义或外部注册的单例实例？
	 * <p>如果给定的名称是别名，它将回到相应的规范bean名称。
	 * <p>如果此工厂是层次化的，如果在此工厂实例中找不到bean，它将询问父工厂。
	 * <p>如果找到与给定名称匹配的bean定义或单例实例，此方法将返回{@code true}，
	 * 不论命名bean定义是具体的还是抽象的，懒惰的还是积极的，在范围内还是不在范围内。
	 * 因此，请注意，此方法返回{@code true}并不一定表示{@link #getBean}
	 * 能够为相同的名称获取实例。
	 *
	 * @param name 要查询的bean的名称
	 * @return 是否存在具有给定名称的bean
	 */
	boolean containsBean(String name);


	/**
	 * 此Bean是否为共享单例？也就是说，{@link #getBean} 总是返回相同的实例？
	 * <p>注意：此方法返回 {@code false} 不明确表示独立实例。它表示非单例实例，可能对应于一个作用域Bean。使用 {@link #isPrototype} 操作显式检查独立实例。
	 * <p>将别名转换回相应的规范Bean名称。
	 * <p>如果在此工厂实例中找不到Bean，将询问父工厂。
	 *
	 * @param name 要查询的Bean的名称
	 * @return 此Bean是否对应于单例实例
	 * @throws NoSuchBeanDefinitionException 如果找不到具有给定名称的Bean
	 * @see #getBean
	 * @see #isPrototype
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 此Bean是否为原型？也就是说，{@link #getBean} 总是返回独立的实例？
	 * <p>注意：此方法返回 {@code false} 不明确表示单例对象。它表示非独立实例，可能对应于一个作用域Bean。使用 {@link #isSingleton} 操作显式检查共享单例实例。
	 * <p>将别名转换回相应的规范Bean名称。
	 * <p>如果在此工厂实例中找不到Bean，将询问父工厂。
	 *
	 * @param name 要查询的Bean的名称
	 * @return 此Bean是否总是提供独立实例
	 * @throws NoSuchBeanDefinitionException 如果找不到具有给定名称的Bean
	 * @see #getBean
	 * @see #isSingleton
	 * @since 2.0.3
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 检查给定名称的bean是否与指定的类型匹配。
	 * 更具体地说，检查{@link #getBean}对于给定名称的调用是否会返回一个可以分配给指定目标类型的对象。
	 * <p>将别名转换回相应的规范bean名称。
	 * <p>如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name        要查询的bean的名称
	 * @param typeToMatch 要匹配的目标类型（作为{@code ResolvableType}）
	 * @return 如果bean类型匹配，则为{@code true}，否则为{@code false}或无法确定
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的bean
	 * @see #getBean
	 * @see #getType
	 * @since 4.2
	 */
	boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 检查给定名称的bean是否与指定的类型匹配。
	 * 更具体地说，检查{@link #getBean}对于给定名称的调用是否会返回一个可以分配给指定目标类型的对象。
	 * <p>将别名转换回相应的规范bean名称。
	 * <p>如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name        要查询的bean的名称
	 * @param typeToMatch 要匹配的目标类型（作为{@code Class}）
	 * @return 如果bean类型匹配，则为{@code true}，否则为{@code false}或无法确定
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的bean
	 * @see #getBean
	 * @see #getType
	 * @since 2.0.1
	 */
	boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 确定给定名称的bean的类型。更具体地说，确定{@link #getBean}对于给定名称的调用会返回的对象的类型。
	 * <p>对于{@link FactoryBean}，返回FactoryBean创建的类型，通过{@link FactoryBean#getObjectType()}暴露。
	 * 这可能导致初始化先前未初始化的{@code FactoryBean}(see {@link #getType(String, boolean)}。
	 * <p>将别名转换回相应的规范bean名称。
	 * <p>如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要查询的bean的名称
	 * @return bean的类型，如果无法确定则为{@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的bean
	 * @see #getBean
	 * @see #isTypeMatch
	 * @since 1.1.2
	 */
	@Nullable
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;


	/**
	 * 确定给定名称的bean的类型。更具体地说，确定{@link #getBean}对于给定名称的调用会返回的对象的类型。
	 * <p>对于{@link FactoryBean}，返回FactoryBean创建的类型，通过{@link FactoryBean#getObjectType()}暴露。
	 * 根据{@code allowFactoryBeanInit}标志，这可能导致初始化先前未初始化的{@code FactoryBean}（如果无法提供早期的类型信息）。
	 * <p>将别名转换回相应的规范bean名称。
	 * <p>如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name                 要查询的bean的名称
	 * @param allowFactoryBeanInit 是否允许为了确定对象类型而初始化{@code FactoryBean}
	 * @return bean的类型，如果无法确定则为{@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的bean
	 * @see #getBean
	 * @see #isTypeMatch
	 * @since 5.2
	 */
	@Nullable
	Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定bean名称的别名（如果有）。
	 * <p>所有这些别名在{@link #getBean}调用中指向您相同的bean。
	 * <p>如果给定的名称是别名，则相应的原始bean名称和其他别名（如果有）将返回，其中原始bean名称是数组中的第一个元素。
	 * <p>如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要检查别名的bean名称
	 * @return 别名，如果没有则为空数组
	 * @see #getBean
	 */
	String[] getAliases(String name);


}
