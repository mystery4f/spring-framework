/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 一个基于{@link TypeDescriptor}的{@code source}和{@code target}属性条件执行的{@link GenericConverter}。
 *
 * <p>详细信息见{@link ConditionalConverter}。
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @see GenericConverter
 * @see ConditionalConverter
 * @since 3.0
 */
public interface ConditionalGenericConverter extends GenericConverter, ConditionalConverter {
}

