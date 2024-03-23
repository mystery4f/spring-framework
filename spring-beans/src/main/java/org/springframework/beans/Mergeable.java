/*
 * Copyright 2002-2018 the original author or authors.
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

/**
 * Interface representing an object whose value set can be merged with
 * that of a parent object.
 *
 * @author Rob Harrop
 * @see org.springframework.beans.factory.support.ManagedSet
 * @see org.springframework.beans.factory.support.ManagedList
 * @see org.springframework.beans.factory.support.ManagedMap
 * @see org.springframework.beans.factory.support.ManagedProperties
 * @since 2.0
 */
public interface Mergeable {

	/**
	 * 判断当前实例是否启用了合并功能。
	 *
	 * @return 如果启用了合并功能返回true，否则返回false。
	 */
	boolean isMergeEnabled();

	/**
	 * 将当前值集与提供的对象的值集合并。
	 * <p>提供的对象被视为父对象，调用者的值集中的值应该覆盖提供对象的值集中的值。
	 *
	 * @param parent 要与之合并的父对象
	 * @return 合并操作的结果
	 * @throws IllegalArgumentException 如果提供的父对象为{@code null}
	 * @throws IllegalStateException    如果当前实例没有启用合并功能（即{@code mergeEnabled}等于{@code false}）
	 */
	Object merge(@Nullable Object parent);


}
