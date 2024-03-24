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

import org.springframework.lang.Nullable;

import java.beans.PropertyEditor;

/**
 * 封装了注册JavaBeans {@link PropertyEditor PropertyEditors} 的方法。
 * 这是一个 {@link PropertyEditorRegistrar} 操作的中心接口。
 *
 * <p>被 {@link BeanWrapper} 扩展；被 {@link BeanWrapperImpl} 和 {@link org.springframework.validation.DataBinder} 实现。
 *
 * @author Juergen Hoeller
 * @see java.beans.PropertyEditor
 * @see PropertyEditorRegistrar
 * @see BeanWrapper
 * @see org.springframework.validation.DataBinder
 * @since 1.2.6
 */
public interface PropertyEditorRegistry {

	/**
	 * 为给定类型的所有属性注册自定义属性编辑器。
	 *
	 * @param requiredType   属性的类型
	 * @param propertyEditor 要注册的编辑器
	 */
	void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

	/**
	 * 为给定类型和属性注册自定义属性编辑器，或者为给定类型的所有属性注册。
	 * <p>如果属性路径表示数组或集合属性，则编辑器将应用于数组/集合本身
	 * （{@link PropertyEditor}必须创建数组或集合值）或每个元
	 * （{@code PropertyEditor}必须创建元素类型），具体取决于指定的requiredType。
	 * <p>注意：每个属性路径只支持注册一个自定义编辑。
	 * 对于集合/数组，不要同时为同一属性的集合/数组和每个元素注册编辑器。
	 * <>例如，如果要为"items[n].quantity"（所有值n）注册编辑器，
	 * 则可以将"items.quantity"作为此方法的'propertyPath'参数的值。
	 *
	 * @param requiredType   属性的类型。如果属性已给出但应在任何情况下指定，
	 *                       特别是在集合的情况下，这可能{@code null} - 明确指定编辑器是应用于整个集合本身还是应用于其每个条目。
	 *                       因此，作为一般规则：<b>在集合/数组的情���下，不要在此处指定{@code null}！</b     * @param propertyPath 属性的路径（名称或嵌套路径），或者如果为给定类型的所有属性注册编辑器，则为{@code null}
	 * @param propertyEditor 要注册的编辑器
	 */
	void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor);

	/**
	 * 查找给定类型和属性的自定义属性编辑器。
	 *
	 * @param requiredType 属性的类型（如果给定属性，则可以是{@code null}，但为了一致性检查应在任何情况下指定）
	 * @param propertyPath 属性的路径（名称或嵌套路径），或者如果查找给定类型的所有属性的编辑器，则为{@code null}
	 * @return 注册的编辑器，如果没有则返回{@code null}
	 */
	@Nullable
	PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath);

}

