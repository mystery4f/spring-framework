/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Constructor;

/**
 * 该接口是 {@link InstantiationAwareBeanPostProcessor} 接口的扩展，添加了一个用于预测已处理 `Bean` 对象最终类型的回调函数。
 * <p>
 * 需要注意的是，该接口是一个专用的接口，主要供框架内部使用。通常情况下，应用程序提供的后置处理器应该只实现普通的 {@link BeanPostProcessor} 接口或继承 {@link InstantiationAwareBeanPostProcessorAdapter} 类。在点版本中甚至可能会添加新的方法到该接口中。
 * <p>
 * 该接口是 Spring IoC 容器在创建 `Bean` 对象过程中的一个扩展点，能够让开发人员拓展 `Bean` 对象实例化和初始化的过程，并添加自定义的操作以适应各种需求。
 *
 * @author Juergen Hoeller
 * @see InstantiationAwareBeanPostProcessorAdapter
 * @since 2.0.3
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * 预测在此处理器的{@link #postProcessBeforeInstantiation}回调中最终返回的bean的类型。
	 * <p>默认实现返回{@code null}。
	 *
	 * @param beanClass bean的原始类
	 * @param beanName  bean的名称
	 * @return bean的类型，如果无法预测则返回{@code null}
	 * @throws org.springframework.beans.BeansException 如果发生错误
	 */
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}


	/**
	 * 确定用于给定bean的候选构造函数。
	 * <p>默认实现返回{@code null}。
	 *
	 * @param beanClass bean的原始类（永远不会为{@code null}）
	 * @param beanName  bean的名称
	 * @return 候选构造函数，如果没有指定则返回{@code null}
	 * @throws org.springframework.beans.BeansException 如果发生错误
	 */
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * 获取对指定bean的早期访问引用，通常用于解析循环引用。
	 * <p>此回调为后处理器提供了在目标bean实例完全初始化之前提前公开包装器的机会。
	 * 公开的对象应该等同于{@link #postProcessBeforeInitialization} / {@link #postProcessAfterInitialization}
	 * 否则会公开的内容。请注意，此方法返回的对象将用作bean引用，除非后处理器从上述后处理回调中返回不同的包装器。
	 * 换句话说：这些后处理回调可能最终公开相同的引用，或者从这些后续回调中返回原始bean实例（如果已为调用此方法构建了受影响bean的包装器，
	 * 则默认情况下将公开为最终bean引用）。
	 * <p>默认实现将给定的{@code bean}原样返回。
	 *
	 * @param bean     原始bean实例
	 * @param beanName bean的名称
	 * @return 要公开为bean引用的对象（通常使用传入的bean实例作为默认值）
	 * @throws org.springframework.beans.BeansException 如果发生错误
	 */
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
