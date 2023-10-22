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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于保存管理的List元素的标签集合类，可以包含运行时的bean引用（将被解析为bean对象）。
 *
 * @param <E> 元素类型
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 27.05.2003
 */
@SuppressWarnings("serial")
public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement {

	@Nullable
	private Object source;

	@Nullable
	private String elementTypeName;

	private boolean mergeEnabled;


	public ManagedList() {
	}

	public ManagedList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置此元数据元素的配置源对象。
	 * <p>对象的确切类型将取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	/**
	 * 返回用于此列表的默认元素类型名称（类名）。
	 */
	@Nullable
	public String getElementTypeName() {
		return this.elementTypeName;
	}

	/**
	 * 设置用于此列表的默认元素类型名称（类名）。
	 */
	public void setElementTypeName(String elementTypeName) {
		this.elementTypeName = elementTypeName;
	}

	@Override
	public boolean isMergeEnabled() {
		return this.mergeEnabled;
	}

	/**
	 * 设置是否启用合并功能，以便在存在“父”集合值的情况下进行合并。
	 */
	public void setMergeEnabled(boolean mergeEnabled) {
		this.mergeEnabled = mergeEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	/**
	 * 合并列表
	 * @param parent 父列表
	 * @return 合并后的列表
	 */
	public List<E> merge(@Nullable Object parent) {
		// 检查是否允许合并
		if (!this.mergeEnabled) {
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		// 如果父列表为空，则直接返回当前列表
		if (parent == null) {
			return this;
		}
		// 如果父列表不是List类型，则抛出异常
		if (!(parent instanceof List)) {
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		// 创建一个新的列表用于存储合并后的元素
		List<E> merged = new ManagedList<>();
		// 将父列表的元素添加到合并列表中
		merged.addAll((List<E>) parent);
		// 将当前列表的元素添加到合并列表中
		merged.addAll(this);
		// 返回合并后的列表
		return merged;
	}

}
