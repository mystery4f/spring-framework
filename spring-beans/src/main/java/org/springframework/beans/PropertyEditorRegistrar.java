/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans;

/**
 * Interface for strategies that register custom
 * {@link java.beans.PropertyEditor property editors} with a
 * {@link org.springframework.beans.PropertyEditorRegistry property editor registry}.
 *
 * <p>This is particularly useful when you need to use the same set of
 * property editors in several different situations: write a corresponding
 * registrar and reuse that in each case.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see PropertyEditorRegistry
 * @see java.beans.PropertyEditor
 */
public interface PropertyEditorRegistrar {

	/**
	 * 使用给定的{@code PropertyEditorRegistry}注册自定义{@link java.beans.PropertyEditor PropertyEditors}。
	 * <p>传入的注册表通常将是一个{@link BeanWrapper}或一个{@link org.springframework.validation.DataBinder DataBinder}。
	 * <p>预期实现将为每次调用此方法创建全新的{@code PropertyEditors}实例（因为{@code PropertyEditors}不是线程安全的）。
	 * @param registry 要注册自定义{@code PropertyEditors}的{@code PropertyEditorRegistry}
	 */
	void registerCustomEditors(PropertyEditorRegistry registry);


}
