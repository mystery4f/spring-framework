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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 一个条件接口，组件只有在该条件被{@linkplain #matches 匹配}的情况下才能被注册。
 *
 * <p>条件在 bean 定义即将注册的立即时刻进行检查，并且可以根据当时任何可确定的条件自由决定是否阻止注册。
 *
 * <p>条件必须遵守与{@link BeanFactoryPostProcessor}相同的限制，并且要避免与 bean 实例交互。如果需要更细粒度地控制与{@code @Configuration} beans 交互的条件，可以考虑实现 {@link ConfigurationCondition} 接口。
 *
 * @author Phillip Webb
 * @see ConfigurationCondition 用于更细粒度控制的配置条件接口。
 * @see Conditional 基于条件的注解接口，用于标记类或方法上。
 * @see ConditionContext 条件上下文，提供环境信息和元数据访问。
 * @since 4.0
 */
@FunctionalInterface
public interface Condition {

	/**
	 * 判断条件是否匹配。
	 *
	 * @param context  条件上下文，包含环境信息和元数据。
	 * @param metadata 当前正在检查的类的{@link org.springframework.core.type.AnnotationMetadata 注解元数据}或{@link org.springframework.core.type.MethodMetadata 方法元数据}。
	 * @return 如果条件匹配且组件可以注册 ，则返回{@code true}；如果条件不匹配，要阻止注解组件的注册，则返回{@code false}。
	 */
	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);


}
