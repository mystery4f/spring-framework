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
import org.springframework.lang.Nullable;

/**
 * Factory hook that allows for custom modification of new bean instances &mdash;
 * for example, checking for marker interfaces or wrapping beans with proxies.
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} can autodetect {@code BeanPostProcessor} beans
 * in its bean definitions and apply those post-processors to any beans subsequently
 * created. A plain {@code BeanFactory} allows for programmatic registration of
 * post-processors, applying them to all beans created through the bean factory.
 *
 * <h3>Ordering</h3>
 * <p>{@code BeanPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link org.springframework.core.PriorityOrdered} and
 * {@link org.springframework.core.Ordered} semantics. In contrast,
 * {@code BeanPostProcessor} beans that are registered programmatically with a
 * {@code BeanFactory} will be applied in the order of registration; any ordering
 * semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link org.springframework.core.annotation.Order @Order} annotation is not
 * taken into account for {@code BeanPostProcessor} beans.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 * @since 10.10.2003
 */
public interface BeanPostProcessor {

	/**
	 * 在任何Bean初始化回调（如InitializingBean的afterPropertiesSet或自定义init-method）之前，
	 * 将此BeanPostProcessor应用于给定的新Bean实例。Bean将已经填充了属性值。
	 * 返回的Bean实例可能是原始Bean的包装器。
	 * <p>默认实现将给定的bean原样返回。
	 *
	 * @param bean     新的Bean实例
	 * @param beanName Bean的名称
	 * @return 要使用的Bean实例，原始实例或包装实例；如果为null，则不会调用后续的BeanPostProcessors
	 * @throws org.springframework.beans.BeansException 如果发生错误
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * 在bean初始化回调之后应用此BeanPostProcessor到给定的新bean实例。
	 * bean将已经填充了属性值。
	 * 返回的bean实例可能是原始bean的包装器。
	 * <p>
	 * 对于FactoryBean，此回调将为FactoryBean实例和FactoryBean创建的对象（从Spring 2.0开始）调用。
	 * 后处理器可以通过相应的bean instanceof FactoryBean检查来决定是否应用于FactoryBean或创建的对象或两者。
	 * <p>
	 * 与所有其他BeanPostProcessor回调相比，此回调也将在由InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation方法触发的短路之后调用。
	 * <p>
	 * 默认实现将返回给定的bean。
	 *
	 * @param bean     新的bean实例
	 * @param beanName bean的名称
	 * @return 使用的bean实例，原始的或包装的；如果为null，则不会调用后续的BeanPostProcessors
	 * @throws org.springframework.beans.BeansException 如果发生错误
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String bean3Name) throws BeansException {
		return bean;
	}


}
