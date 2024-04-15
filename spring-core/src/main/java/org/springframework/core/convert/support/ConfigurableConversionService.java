/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * 一个配置接口，大多数（如果不是全部）{@link ConversionService}类型都需要实现它。该接口合并了
 * {@link ConversionService} 所暴露的只读操作和 {@link ConverterRegistry} 的可变操作，以便通过此接口方便地
 * 随机添加和移除 {@link org.springframework.core.convert.converter.Converter 转换器}。特别是在应用上下文启动代码中操作
 * {@link org.springframework.core.env.ConfigurableEnvironment 可配置环境} 实例时，后者特别有用。
 *
 * @author Chris Beams
 * @see org.springframework.core.env.ConfigurablePropertyResolver#getConversionService()
 * @see org.springframework.core.env.ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 * @since 3.1
 */
public interface ConfigurableConversionService extends ConversionService, ConverterRegistry {

}
