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
     * Fully create a new bean instance of the given class.
     * <p>Performs full initialization of the bean, including all applicable
     * {@link BeanPostProcessor BeanPostProcessors}.
     * <p>Note: This is intended for creating a fresh instance, populating annotated
     * fields and methods as well as applying all standard bean initialization callbacks.
     * It does <i>not</i> imply traditional by-name or by-type autowiring of properties;
     * use {@link #createBean(Class, int, boolean)} for those purposes.
     *
     * @param beanClass the class of the bean to create
     * @return the new bean instance
     * @throws BeansException if instantiation or wiring failed
     */
    <T> T createBean(Class<T> beanClass) throws BeansException;

    /**
     * Populate the given bean instance through applying after-instantiation callbacks
     * and bean property post-processing (e.g. for annotation-driven injection).
     * <p>Note: This is essentially intended for (re-)populating annotated fields and
     * methods, either for new instances or for deserialized instances. It does
     * <i>not</i> imply traditional by-name or by-type autowiring of properties;
     * use {@link #autowireBeanProperties} for those purposes.
     *
     * @param existingBean the existing bean instance
     * @throws BeansException if wiring failed
     */
    void autowireBean(Object existingBean) throws BeansException;

    /**
     * Configure the given raw bean: autowiring bean properties, applying
     * bean property values, applying factory callbacks such as {@code setBeanName}
     * and {@code setBeanFactory}, and also applying all bean post processors
     * (including ones which might wrap the given raw bean).
     * <p>This is effectively a superset of what {@link #initializeBean} provides,
     * fully applying the configuration specified by the corresponding bean definition.
     * <b>Note: This method requires a bean definition for the given name!</b>
     *
     * @param existingBean the existing bean instance
     * @param beanName     the name of the bean, to be passed to it if necessary
     *                     (a bean definition of that name has to be available)
     * @return the bean instance to use, either the original or a wrapped one
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if there is no bean definition with the given name
     * @throws BeansException                                                  if the initialization failed
     * @see #initializeBean
     */
    Object configureBean(Object existingBean, String beanName) throws BeansException;


    //-------------------------------------------------------------------------
    // Specialized methods for fine-grained control over the bean lifecycle
    //-------------------------------------------------------------------------

    /**
     * Fully create a new bean instance of the given class with the specified
     * autowire strategy. All constants defined in this interface are supported here.
     * <p>Performs full initialization of the bean, including all applicable
     * {@link BeanPostProcessor BeanPostProcessors}. This is effectively a superset
     * of what {@link #autowire} provides, adding {@link #initializeBean} behavior.
     *
     * @param beanClass       the class of the bean to create
     * @param autowireMode    by name or type, using the constants in this interface
     * @param dependencyCheck whether to perform a dependency check for objects
     *                        (not applicable to autowiring a constructor, thus ignored there)
     * @return the new bean instance
     * @throws BeansException if instantiation or wiring failed
     * @see #AUTOWIRE_NO
     * @see #AUTOWIRE_BY_NAME
     * @see #AUTOWIRE_BY_TYPE
     * @see #AUTOWIRE_CONSTRUCTOR
     */
    Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

    /**
     * Instantiate a new bean instance of the given class with the specified autowire
     * strategy. All constants defined in this interface are supported here.
     * Can also be invoked with {@code AUTOWIRE_NO} in order to just apply
     * before-instantiation callbacks (e.g. for annotation-driven injection).
     * <p>Does <i>not</i> apply standard {@link BeanPostProcessor BeanPostProcessors}
     * callbacks or perform any further initialization of the bean. This interface
     * offers distinct, fine-grained operations for those purposes, for example
     * {@link #initializeBean}. However, {@link InstantiationAwareBeanPostProcessor}
     * callbacks are applied, if applicable to the construction of the instance.
     *
     * @param beanClass       the class of the bean to instantiate
     * @param autowireMode    by name or type, using the constants in this interface
     * @param dependencyCheck whether to perform a dependency check for object
     *                        references in the bean instance (not applicable to autowiring a constructor,
     *                        thus ignored there)
     * @return the new bean instance
     * @throws BeansException if instantiation or wiring failed
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
     * Autowire the bean properties of the given bean instance by name or type.
     * Can also be invoked with {@code AUTOWIRE_NO} in order to just apply
     * after-instantiation callbacks (e.g. for annotation-driven injection).
     * <p>Does <i>not</i> apply standard {@link BeanPostProcessor BeanPostProcessors}
     * callbacks or perform any further initialization of the bean. This interface
     * offers distinct, fine-grained operations for those purposes, for example
     * {@link #initializeBean}. However, {@link InstantiationAwareBeanPostProcessor}
     * callbacks are applied, if applicable to the configuration of the instance.
     *
     * @param existingBean    the existing bean instance
     * @param autowireMode    by name or type, using the constants in this interface
     * @param dependencyCheck whether to perform a dependency check for object
     *                        references in the bean instance
     * @throws BeansException if wiring failed
     * @see #AUTOWIRE_BY_NAME
     * @see #AUTOWIRE_BY_TYPE
     * @see #AUTOWIRE_NO
     */
    void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) throws BeansException;

    /**
     * Apply the property values of the bean definition with the given name to
     * the given bean instance. The bean definition can either define a fully
     * self-contained bean, reusing its property values, or just property values
     * meant to be used for existing bean instances.
     * <p>This method does <i>not</i> autowire bean properties; it just applies
     * explicitly defined property values. Use the {@link #autowireBeanProperties}
     * method to autowire an existing bean instance.
     * <b>Note: This method requires a bean definition for the given name!</b>
     * <p>Does <i>not</i> apply standard {@link BeanPostProcessor BeanPostProcessors}
     * callbacks or perform any further initialization of the bean. This interface
     * offers distinct, fine-grained operations for those purposes, for example
     * {@link #initializeBean}. However, {@link InstantiationAwareBeanPostProcessor}
     * callbacks are applied, if applicable to the configuration of the instance.
     *
     * @param existingBean the existing bean instance
     * @param beanName     the name of the bean definition in the bean factory
     *                     (a bean definition of that name has to be available)
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if there is no bean definition with the given name
     * @throws BeansException                                                  if applying the property values failed
     * @see #autowireBeanProperties
     */
    void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;

    /**
     * Initialize the given raw bean, applying factory callbacks
     * such as {@code setBeanName} and {@code setBeanFactory},
     * also applying all bean post processors (including ones which
     * might wrap the given raw bean).
     * <p>Note that no bean definition of the given name has to exist
     * in the bean factory. The passed-in bean name will simply be used
     * for callbacks but not checked against the registered bean definitions.
     *
     * @param existingBean the existing bean instance
     * @param beanName     the name of the bean, to be passed to it if necessary
     *                     (only passed to {@link BeanPostProcessor BeanPostProcessors};
     *                     can follow the {@link #ORIGINAL_INSTANCE_SUFFIX} convention in order to
     *                     enforce the given instance to be returned, i.e. no proxies etc)
     * @return the bean instance to use, either the original or a wrapped one
     * @throws BeansException if the initialization failed
     * @see #ORIGINAL_INSTANCE_SUFFIX
     */
    Object initializeBean(Object existingBean, String beanName) throws BeansException;

    /**
     * Apply {@link BeanPostProcessor BeanPostProcessors} to the given existing bean
     * instance, invoking their {@code postProcessBeforeInitialization} methods.
     * The returned bean instance may be a wrapper around the original.
     *
     * @param existingBean the existing bean instance
     * @param beanName     the name of the bean, to be passed to it if necessary
     *                     (only passed to {@link BeanPostProcessor BeanPostProcessors};
     *                     can follow the {@link #ORIGINAL_INSTANCE_SUFFIX} convention in order to
     *                     enforce the given instance to be returned, i.e. no proxies etc)
     * @return the bean instance to use, either the original or a wrapped one
     * @throws BeansException if any post-processing failed
     * @see BeanPostProcessor#postProcessBeforeInitialization
     * @see #ORIGINAL_INSTANCE_SUFFIX
     */
    Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException;

    /**
     * Apply {@link BeanPostProcessor BeanPostProcessors} to the given existing bean
     * instance, invoking their {@code postProcessAfterInitialization} methods.
     * The returned bean instance may be a wrapper around the original.
     *
     * @param existingBean the existing bean instance
     * @param beanName     the name of the bean, to be passed to it if necessary
     *                     (only passed to {@link BeanPostProcessor BeanPostProcessors};
     *                     can follow the {@link #ORIGINAL_INSTANCE_SUFFIX} convention in order to
     *                     enforce the given instance to be returned, i.e. no proxies etc)
     * @return the bean instance to use, either the original or a wrapped one
     * @throws BeansException if any post-processing failed
     * @see BeanPostProcessor#postProcessAfterInitialization
     * @see #ORIGINAL_INSTANCE_SUFFIX
     */
    Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException;

    /**
     * Destroy the given bean instance (typically coming from {@link #createBean}),
     * applying the {@link org.springframework.beans.factory.DisposableBean} contract as well as
     * registered {@link DestructionAwareBeanPostProcessor DestructionAwareBeanPostProcessors}.
     * <p>Any exception that arises during destruction should be caught
     * and logged instead of propagated to the caller of this method.
     *
     * @param existingBean the bean instance to destroy
     */
    void destroyBean(Object existingBean);


    //-------------------------------------------------------------------------
    // Delegate methods for resolving injection points
    //-------------------------------------------------------------------------

    /**
     * Resolve the bean instance that uniquely matches the given object type, if any,
     * including its bean name.
     * <p>This is effectively a variant of {@link #getBean(Class)} which preserves the
     * bean name of the matching instance.
     *
     * @param requiredType type the bean must match; can be an interface or superclass
     * @return the bean name plus bean instance
     * @throws NoSuchBeanDefinitionException   if no matching bean was found
     * @throws NoUniqueBeanDefinitionException if more than one matching bean was found
     * @throws BeansException                  if the bean could not be created
     * @see #getBean(Class)
     * @since 4.3.3
     */
    <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

    /**
     * Resolve a bean instance for the given bean name, providing a dependency descriptor
     * for exposure to target factory methods.
     * <p>This is effectively a variant of {@link #getBean(String, Class)} which supports
     * factory methods with an {@link org.springframework.beans.factory.InjectionPoint}
     * argument.
     *
     * @param name       the name of the bean to look up
     * @param descriptor the dependency descriptor for the requesting injection point
     * @return the corresponding bean instance
     * @throws NoSuchBeanDefinitionException if there is no bean with the specified name
     * @throws BeansException                if the bean could not be created
     * @see #getBean(String, Class)
     * @since 5.1.5
     */
    Object resolveBeanByName(String name, DependencyDescriptor descriptor) throws BeansException;

    /**
     * Resolve the specified dependency against the beans defined in this factory.
     *
     * @param descriptor         the descriptor for the dependency (field/method/constructor)
     * @param requestingBeanName the name of the bean which declares the given dependency
     * @return the resolved object, or {@code null} if none found
     * @throws NoSuchBeanDefinitionException   if no matching bean was found
     * @throws NoUniqueBeanDefinitionException if more than one matching bean was found
     * @throws BeansException                  if dependency resolution failed for any other reason
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
