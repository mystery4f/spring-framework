/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * 用于为 {@link BeanDefinition} 生成 Bean 名称的策略接口。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 */
public interface BeanNameGenerator {

	/**
	 * 生成给定Bean定义的Bean名称。
	 *
	 * @param definition 用于生成名称的Bean定义
	 * @param registry   给定定义应该被注册的Bean定义注册表
	 * @return 生成的Bean名称
	 */
	String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry);

}
