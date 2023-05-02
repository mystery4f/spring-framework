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
	 * 检查此 bean 工厂是否包含给定名称的 bean 定义。不考虑此工厂可能参与的任何层次结构，并忽略通过其他方式注册的任何单例 bean。
	 *
	 * @param beanName 要查找的 bean 的名称
	 * @return 如果此 bean 工厂包含给定名称的 bean 定义，则为 true
	 * @see #containsBean
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回工厂中定义的 bean 数量。不考虑此工厂可能参与的任何层次结构，并忽略通过其他方式注册的任何单例 bean。
	 *
	 * @return 工厂中定义的 bean 数量
	 */
	int getBeanDefinitionCount();

	/**
	 * 返回此工厂中定义的所有 bean 的名称。不考虑此工厂可能参与的任何层次结构，并忽略通过其他方式注册的任何单例 bean。
	 *
	 * @return 此工厂中定义的所有 bean 的名称，如果没有定义，则返回空数组
	 */
	String[] getBeanDefinitionNames();


	/**
	 * 返回指定bean的提供者，允许在需要时进行惰性按需检索实例，
	 * 包括可用性和唯一性选项。
	 *
	 * @param requiredType   bean必须匹配的类型；可以是接口或超类
	 * @param allowEagerInit 是否允许流式访问初始化“惰性初始化的单例”和“由FactoryBeans创建的对象”
	 *                       （或由具有“factory-bean”引用的工厂方法）以进行类型检查
	 * @return 对应的提供者句柄
	 * @see #getBeanProvider(ResolvableType, boolean)
	 * @see #getBeanProvider(Class)
	 * @see #getBeansOfType(Class, boolean, boolean)
	 * @see #getBeanNamesForType(Class, boolean, boolean)
	 * @since 5.3
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);


	/**
	 * 返回指定bean的提供者，允许延迟按需检索实例，包括可用性和唯一性选项。
	 *
	 * @param requiredType   必须匹配的bean的类型；可以是通用类型声明。
	 *                       请注意，与反射注入点不同，此处不支持集合类型。要通过编程方式检索与特定类型匹配的bean列表，请在此处指定
	 *                       实际的bean类型，然后随后使用{@link ObjectProvider#orderedStream（）}或其惰性流/迭代选项。
	 * @param allowEagerInit 是否可以初始化<i>懒惰初始化单例</i>和<i>由FactoryBeans创建</i>的对象
	 *                       （或通过具有“factory-bean”引用的工厂方法）。用于类型检查
	 *                       流式访问
	 * @return 相应的提供者句柄
	 * @see #getBeanProvider(ResolvableType)
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @see #getBeanNamesForType(ResolvableType, boolean, boolean)
	 * @since 5.3
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit);


	/**
	 * 返回与给定类型（包括子类）匹配的bean的名称，根据bean定义或FactoryBeans的{@code getObjectType}的值判断。
	 * <p><b>注意：此方法仅内省顶级bean。</b>它不会检查可能匹配指定类型的嵌套bean。
	 * <p>考虑由FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。如果FactoryBean创建的对象不匹配，
	 * 则原始FactoryBean本身将与类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 请使用BeanFactoryUtils的{@code beanNamesForTypeIncludingAncestors}将祖先工厂中的bean也包括在内。
	 * <p>注意：不会忽略已通过其他方式注册的单例bean。
	 * <p>此版本的{@code getBeanNamesForType}匹配所有类型的bean，无论是单例，原型还是FactoryBeans。
	 * 在大多数实现中，结果与{@code getBeanNamesForType（type，true，true）}相同。
	 * <p>此方法返回的bean名称应始终返回bean名称<i>按定义的顺序<i/>在后端配置中，尽可能的。
	 *
	 * @param type 要匹配的通用类型的类或接口
	 * @return 匹配给定对象类型（包括子类）的bean（或由FactoryBeans创建的对象）的名称，如果没有，则为空数组
	 * @see #isTypeMatch(String, ResolvableType)
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType)
	 * @since 4.2
	 */
	String[] getBeanNamesForType(ResolvableType type);


	/**
	 * 返回与给定类型（包括子类）匹配的bean的名称，根据bean定义或FactoryBeans的getObjectType的值来判断。
	 * <p><b>注意：此方法仅内省顶级bean。</b>它不会检查可能与指定类型匹配的嵌套bean。
	 * <p>如果设置了“allowEagerInit”标志，则将考虑FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。
	 * 如果由FactoryBean创建的对象不匹配，则原始FactoryBean本身将与类型匹配。
	 * 如果未设置“allowEagerInit”，则仅检查原始FactoryBeans（不需要初始化每个FactoryBean）。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用BeanFactoryUtils的beanNamesForTypeIncludingAncestors将祖先工厂中的bean也包括在内。
	 * <p>注意：不会忽略以其他方式注册的单例bean。
	 * <p>此方法返回的bean名称应始终返回bean名称<i>按照后端配置的定义顺序</i>，尽可能。
	 *
	 * @param type                 要匹配的泛型类型类或接口
	 * @param includeNonSingletons 是否包括原型或作用域bean，或仅包括单例（也适用于FactoryBeans）
	 * @param allowEagerInit       是否初始化<i>lazy-init单例</i>和<i>由FactoryBeans创建的对象</i>（或由带有“factory-bean”引用的工厂方法创建）进行类型检查。
	 *                             请注意，FactoryBeans需要被急切地初始化以确定其类型：因此，请注意为此标志传递“true”将初始化FactoryBeans和“factory-bean”引用。
	 * @return 匹配给定对象类型（包括子类）的bean的名称（或由FactoryBeans创建的对象），如果没有，则返回空数组
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
