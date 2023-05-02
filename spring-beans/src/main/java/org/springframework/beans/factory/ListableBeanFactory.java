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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Extension of the {@link BeanFactory} interface to be implemented by bean factories
 * that can enumerate all their bean instances, rather than attempting bean lookup
 * by name one by one as requested by clients. BeanFactory implementations that
 * preload all their bean definitions (such as XML-based factories) may implement
 * this interface.
 *
 * <p>If this is a {@link HierarchicalBeanFactory}, the return values will <i>not</i>
 * take any BeanFactory hierarchy into account, but will relate only to the beans
 * defined in the current factory. Use the {@link BeanFactoryUtils} helper class
 * to consider beans in ancestor factories too.
 *
 * <p>The methods in this interface will just respect bean definitions of this factory.
 * They will ignore any singleton beans that have been registered by other means like
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}'s
 * {@code registerSingleton} method, with the exception of
 * {@code getBeanNamesForType} and {@code getBeansOfType} which will check
 * such manually registered singletons too. Of course, BeanFactory's {@code getBean}
 * does allow transparent access to such special beans as well. However, in typical
 * scenarios, all beans will be defined by external bean definitions anyway, so most
 * applications don't need to worry about this differentiation.
 *
 * <p><b>NOTE:</b> With the exception of {@code getBeanDefinitionCount}
 * and {@code containsBeanDefinition}, the methods in this interface
 * are not designed for frequent invocation. Implementations may be slow.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see HierarchicalBeanFactory
 * @see BeanFactoryUtils
 * @since 16 April 2001
 */
public interface ListableBeanFactory extends BeanFactory {

	/**
	 * Check if this bean factory contains a bean definition with the given name.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 *
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a bean definition with the given name
	 * @see #containsBean
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * Return the number of beans defined in the factory.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 *
	 * @return the number of beans defined in the factory
	 */
	int getBeanDefinitionCount();

	/**
	 * Return the names of all beans defined in this factory.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 *
	 * @return the names of all beans defined in this factory,
	 * or an empty array if none defined
	 */
	String[] getBeanDefinitionNames();

	/**
	 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
	 * of instances, including availability and uniqueness options.
	 *
	 * @param requiredType   type the bean must match; can be an interface or superclass
	 * @param allowEagerInit whether stream-based access may initialize <i>lazy-init
	 *                       singletons</i> and <i>objects created by FactoryBeans</i> (or by factory methods
	 *                       with a "factory-bean" reference) for the type check
	 * @return a corresponding provider handle
	 * @see #getBeanProvider(ResolvableType, boolean)
	 * @see #getBeanProvider(Class)
	 * @see #getBeansOfType(Class, boolean, boolean)
	 * @see #getBeanNamesForType(Class, boolean, boolean)
	 * @since 5.3
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);

	/**
	 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
	 * of instances, including availability and uniqueness options.
	 *
	 * @param requiredType   type the bean must match; can be a generic type declaration.
	 *                       Note that collection types are not supported here, in contrast to reflective
	 *                       injection points. For programmatically retrieving a list of beans matching a
	 *                       specific type, specify the actual bean type as an argument here and subsequently
	 *                       use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
	 * @param allowEagerInit whether stream-based access may initialize <i>lazy-init
	 *                       singletons</i> and <i>objects created by FactoryBeans</i> (or by factory methods
	 *                       with a "factory-bean" reference) for the type check
	 * @return a corresponding provider handle
	 * @see #getBeanProvider(ResolvableType)
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @see #getBeanNamesForType(ResolvableType, boolean, boolean)
	 * @since 5.3
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit);

	/**
	 * Return the names of beans matching the given type (including subclasses),
	 * judging from either bean definitions or the value of {@code getObjectType}
	 * in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
	 * will get initialized. If the object created by the FactoryBean doesn't match,
	 * the raw FactoryBean itself will be matched against the type.
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>This version of {@code getBeanNamesForType} matches all kinds of beans,
	 * be it singletons, prototypes, or FactoryBeans. In most implementations, the
	 * result will be the same as for {@code getBeanNamesForType(type, true, true)}.
	 * <p>Bean names returned by this method should always return bean names <i>in the
	 * order of definition</i> in the backend configuration, as far as possible.
	 *
	 * @param type the generically typed class or interface to match
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @see #isTypeMatch(String, ResolvableType)
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType)
	 * @since 4.2
	 */
	String[] getBeanNamesForType(ResolvableType type);

	/**
	 * Return the names of beans matching the given type (including subclasses),
	 * judging from either bean definitions or the value of {@code getObjectType}
	 * in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set,
	 * which means that FactoryBeans will get initialized. If the object created by the
	 * FactoryBean doesn't match, the raw FactoryBean itself will be matched against the
	 * type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked
	 * (which doesn't require initialization of each FactoryBean).
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>Bean names returned by this method should always return bean names <i>in the
	 * order of definition</i> in the backend configuration, as far as possible.
	 *
	 * @param type                 the generically typed class or interface to match
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 *                             or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit       whether to initialize <i>lazy-init singletons</i> and
	 *                             <i>objects created by FactoryBeans</i> (or by factory methods with a
	 *                             "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 *                             eagerly initialized to determine their type: So be aware that passing in "true"
	 *                             for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType, boolean, boolean)
	 * @since 5.2
	 */
	String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与给定类型（包括子类）匹配的bean的名称，判断是从bean定义还是从FactoryBean的{@code getObjectType}的值来的。
	 * <p><b>注意：此方法仅内省顶级bean。</b> 它不检查可能与指定类型匹配的嵌套bean。
	 * <p>考虑由FactoryBean创建的对象，这意味着FactoryBean将被初始化。如果FactoryBean创建的对象不匹配，则将原始FactoryBean本身与类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。使用BeanFactoryUtils的{@code beanNamesForTypeIncludingAncestors}来包含祖先工厂中的bean。
	 * <p>注意：不会忽略通过其他方式注册的单例bean。
	 * <p>此版本的{@code getBeanNamesForType}匹配所有类型的bean，无论是单例、原型还是FactoryBeans。在大多数实现中，结果将与{@code getBeanNamesForType(type, true, true)}的结果相同。
	 * <p>此方法返回的bean名称应始终以后端配置中定义的顺序返回bean名称。
	 *
	 * @param type 要匹配的类或接口，或{@code null}以获取所有bean名称
	 * @return 与给定对象类型（包括子类）匹配的bean（或FactoryBeans创建的对象）的名称，如果没有，则为空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type);


	/**
	 * 返回与给定类型（包括子类）匹配的bean的名称，判断是从bean定义还是从
	 * FactoryBeans的{@code getObjectType}的值得出的。
	 * <p><b>注意：此方法仅introspects顶级bean。</b>它不会检查可能匹配指定类型的嵌套bean。
	 * <p>如果设置了"allowEagerInit"标志，则会考虑由FactoryBeans创建的对象，这意味着FactoryBeans将得到初始化。
	 * 如果由FactoryBean创建的对象不匹配，则原始FactoryBean本身将与类型匹配。
	 * 如果未设置"allowEagerInit"，则只会检查原始的FactoryBeans（不需要初始化每个FactoryBean）。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用BeanFactoryUtils的{@code beanNamesForTypeIncludingAncestors}也包括祖先工厂中的bean。
	 * <p>注意：不会忽略通过除bean定义之外的其他方式注册的单例bean。
	 * <p>此方法返回的bean名称应始终以后端配置中的定义顺序返回bean名称。
	 *
	 * @param type                 要匹配的类或接口，或{@code null}表示所有bean名称
	 * @param includeNonSingletons 是否还包括原型或作用域bean，或仅包括单例（也适用于FactoryBeans）
	 * @param allowEagerInit       是否初始化<i>lazy-init singletons</i>和<i>objects created by FactoryBeans</i>
	 *                             （或通过具有“factory-bean”引用的工厂方法创建的对象）进行类型检查。
	 *                             请注意，FactoryBeans需要被急切初始化才能确定其类型：因此，请注意将此标志传递为"true"将初始化FactoryBeans和"factory-bean"引用。
	 * @return 匹配给定对象类型（包括子类）的bean的名称（或由FactoryBeans创建的对象），如果没有则返回空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与给定对象类型（包括子类）匹配的bean实例，判断是从bean定义还是从{@code getObjectType}的值
	 * 在FactoryBeans的情况下。
	 * <p><b>注意：此方法仅introspects顶级bean。</b>它不会检查可能匹配指定类型的嵌套bean。
	 * <p>考虑由FactoryBeans创建的对象，这意味着FactoryBeans将得到初始化。如果由FactoryBean创建的对象不匹配，
	 * 则原始FactoryBean本身将与类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用BeanFactoryUtils的{@code beansOfTypeIncludingAncestors}也可以在祖先工厂中包括bean。
	 * <p>注意：不会忽略通过除bean定义之外的其他方式注册的单例bean。
	 * <p>此版本的getBeansOfType匹配所有种类的bean，无论是单例、原型还是FactoryBeans。
	 * 在大多数实现中，结果将与{@code getBeansOfType(type, true, true)}相同。
	 * <p>此方法返回的Map应始终以后端配置中的定义顺序返回bean名称和相应的bean实例。
	 *
	 * @param type 要匹配的类或接口，或{@code null}表示所有具体bean
	 * @return 包含匹配bean的Map，其中包含bean名称作为键，相应的bean实例作为值
	 * @throws BeansException 如果无法创建bean
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 * @since 1.1.2
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException;


	/**
	 * 返回与给定对象类型（包括子类）匹配的bean实例，基于bean定义或FactoryBean情况下的getObjectType值。
	 * <p> <b>注意：此方法仅内省顶级bean。</b> 它不会检查可能匹配指定类型的嵌套bean。
	 * <p>如果设置了“allowEagerInit”标志，则考虑由FactoryBeans创建的对象，这意味着FactoryBeans将得到初始化。
	 * 如果由FactoryBean创建的对象不匹配，则原始FactoryBean本身将与类型匹配。
	 * 如果未设置“allowEagerInit”，则仅检查原始FactoryBeans（这不需要初始化每个FactoryBean）。
	 * <p>不考虑此工厂可能参与的任何层次结构。 使用BeanFactoryUtils的{@code beansOfTypeIncludingAncestors}也包括祖先工厂中的bean。
	 * <p>注意：不会忽略通过其他方式注册的单例bean。
	 * <p>此方法返回的Map应始终尽可能以后端配置中定义的顺序<i>返回bean名称和相应的bean实例</i>。
	 *
	 * @param type                 要匹配的类或接口，或为{@code null}以获取所有具体bean
	 * @param includeNonSingletons 是否包括原型或作用域的bean，或仅包括单例（也适用于FactoryBeans）
	 * @param allowEagerInit       是否初始化<i> lazy-init singletons </ i>和
	 *                             <i>由FactoryBeans创建的对象</ i>（或由具有“factory-bean”引用的工厂方法）进行类型检查。 请注意，FactoryBeans需要急切地初始化才能确定其类型：
	 *                             因此请注意，传递此标志的“true”将初始化FactoryBeans和“factory-bean”引用。
	 * @return 包含匹配的bean的Map，其中包含bean名称作为键，相应的bean实例作为值
	 * @throws BeansException 如果无法创建bean
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;


	/**
	 * 查找所有使用指定{@link Annotation}类型注释的bean的名称，但尚未创建相应的bean实例。
	 * <p>请注意，此方法考虑由FactoryBean创建的对象，这意味着将初始化FactoryBean以确定其对象类型。
	 *
	 * @param annotationType 要查找的注释类型（在指定bean的类、接口或工厂方法级别）
	 * @return 所有匹配的bean名称
	 * @see #findAnnotationOnBean
	 * @since 4.0
	 */
	String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);


	/**
	 * 查找所有被标注为指定{@link Annotation}类型的bean，返回一个包含bean名称和对应bean实例的Map。
	 * <p>请注意，此方法考虑由FactoryBeans创建的对象，这意味着需要初始化FactoryBeans以确定它们的对象类型。
	 *
	 * @param annotationType 要查找的注解类型（在指定bean的类、接口或工厂方法级别）
	 * @return 包含匹配的bean的Map，其中包含bean名称作为键，对应的bean实例作为值
	 * @throws BeansException 如果无法创建bean
	 * @see #findAnnotationOnBean
	 * @since 3.0
	 */
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;


	/**
	 * 查找指定 bean 上的 {@link Annotation}，如果在给定类本身上找不到注释，则遍历其接口和超类，
	 * 并检查 bean 的工厂方法（如果有）。
	 *
	 * @param beanName       要查找注释的 bean 的名称
	 * @param annotationType 要查找的注释类型（在指定 bean 的类、接口或工厂方法级别上）
	 * @return 如果找到给定类型的注释，则返回该注释，否则返回 null
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的 bean
	 * @see #getBeanNamesForAnnotation
	 * @see #getBeansWithAnnotation
	 * @since 3.0
	 */
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException;


}
