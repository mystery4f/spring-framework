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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

import java.beans.PropertyDescriptor;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * <p>Typically used to suppress default instantiation for specific target beans,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link BeanPostProcessor} interface as far as possible.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 * @since 1.2
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 在目标bean实例化之前应用此BeanPostProcessor。返回的bean对象可以是代理，用于替代目标bean，从而有效地抑制目标bean的默认实例化。
	 * <p>如果此方法返回非空对象，则bean创建过程将被短路。只会应用来自配置的BeanPostProcessors的postProcessAfterInitialization回调。
	 * <p>此回调将应用于具有其bean类的bean定义，以及工厂方法定义，这种情况下返回的bean类型将在此处传递。
	 * <p>后处理器可以实现扩展的SmartInstantiationAwareBeanPostProcessor接口，以便预测他们将在此处返回的bean对象的类型。
	 * <p>默认实现返回null。
	 *
	 * @param beanClass 要实例化的bean的类
	 * @param beanName  bean的名称
	 * @return 代替目标bean的默认实例的bean对象，或null以继续默认实例化。
	 * @throws org.springframework.beans.BeansException 如果发生错误
	 * @see #postProcessAfterInstantiation
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getBeanClass()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName()
	 */
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 在bean实例被实例化之后（通过构造函数或工厂方法），但在Spring属性注入（显式属性或自动装配）发生之前，执行操作。
	 * 这是在Spring自动装配之前在给定bean实例上执行自定义字段注入的理想回调。
	 * 默认实现返回true。
	 *
	 * @param bean     创建的bean实例，其属性尚未设置
	 * @param beanName bean的名称
	 * @return 如果应在bean上设置属性，则为true；如果应跳过属性填充，则为false。
	 * 正常情况下，实现应返回true。
	 * 返回false还将防止在此bean实例上调用任何后续的InstantiationAwareBeanPostProcessor实例。
	 * @throws org.springframework.beans.BeansException 如果出现错误
	 * @see #postProcessBeforeInstantiation
	 */
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * 在工厂将属性应用于给定bean之前，对给定属性值进行后处理，
	 * 无需使用任何属性描述符。
	 * 如果实现一个自定义的 {@link #postProcessPropertyValues} 方法，则应返回 {@code null}（默认值），
	 * 否则返回 {@code pvs}。
	 * 在此接口的将来版本中（删除 {@link #postProcessPropertyValues}），
	 * 默认实现将直接返回给定的 {@code pvs}。
	 *
	 * @param pvs      工厂即将应用的属性值（永不为 {@code null}）
	 * @param bean     已创建但其属性尚未设置的bean实例
	 * @param beanName bean的名称
	 * @return 要应用于给定bean的实际属性值（可以是传入的 PropertyValues 实例），
	 * 或 {@code null}，该值继续使用现有属性，但专门继续调用 {@link #postProcessPropertyValues}
	 * （需要当前bean类的已初始化 {@code PropertyDescriptor}）。
	 * @throws org.springframework.beans.BeansException 如果出现错误
	 * @see #postProcessPropertyValues
	 * @since 5.1
	 */
	@Nullable
	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * Post-process the given property values before the factory applies them
	 * to the given bean. Allows for checking whether all dependencies have been
	 * satisfied, for example based on a "Required" annotation on bean property setters.
	 * <p>Also allows for replacing the property values to apply, typically through
	 * creating a new MutablePropertyValues instance based on the original PropertyValues,
	 * adding or removing specific values.
	 * <p>The default implementation returns the given {@code pvs} as-is.
	 *
	 * @param pvs      the property values that the factory is about to apply (never {@code null})
	 * @param pds      the relevant property descriptors for the target bean (with ignored
	 *                 dependency types - which the factory handles specifically - already filtered out)
	 * @param bean     the bean instance created, but whose properties have not yet been set
	 * @param beanName the name of the bean
	 * @return the actual property values to apply to the given bean (can be the passed-in
	 * PropertyValues instance), or {@code null} to skip property population
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessProperties
	 * @see org.springframework.beans.MutablePropertyValues
	 * @deprecated as of 5.1, in favor of {@link #postProcessProperties(PropertyValues, Object, String)}
	 */
	@Deprecated
	@Nullable
	default PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

}
