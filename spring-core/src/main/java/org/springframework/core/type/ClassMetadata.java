/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.type;

import org.springframework.lang.Nullable;

/**
 * 定义抽象访问特定类元数据的接口，在不加载该类的情况下即可访问。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @see StandardClassMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
 * @see AnnotationMetadata
 * @since 2.5
 */
public interface ClassMetadata {

	/**
	 * 返回底层类的名称。
	 */
	String getClassName();

	/**
	 * 返回底层类是否表示注释。
	 *
	 * @since 4.1
	 */
	boolean isAnnotation();

	/**
	 * 返回底层类是否为具体类，即不是接口也不是抽象类。
	 */
	default boolean isConcrete() {
		return !(isInterface() || isAbstract());
	}

	/**
	 * 返回底层类是否表示接口。
	 */
	boolean isInterface();

	/**
	 * 返回底层类是否为抽象类。
	 */
	boolean isAbstract();

	/**
	 * 返回底层类是否为最终类。
	 */
	boolean isFinal();

	/**
	 * 确定底层类是否独立，即 whether it is a top-level class or a nested class (static inner class) that
	 * can be constructed independently from an enclosing class.
	 */
	boolean isIndependent();

	/**
	 * 返回底层类是否在闭包类中（即 underlying class 是内部/嵌套类或方法中的局部类）。
	 * <p>如果此方法返回 {@code false}，则底层类是顶级类。
	 */
	default boolean hasEnclosingClass() {
		return (getEnclosingClassName() != null);
	}

	/**
	 * 返回闭包类的名称，或 {@code null} 如果底层类是顶级类。
	 */
	@Nullable
	String getEnclosingClassName();

	/**
	 * 返回底层类是否有父类。
	 */
	default boolean hasSuperClass() {
		return (getSuperClassName() != null);
	}

	/**
	 * 返回父类的名称，或 {@code null} 如果没有定义父类。
	 */
	@Nullable
	String getSuperClassName();

	/**
	 * 返回底层类实现的接口名称数组，或空数组如果没有接口。
	 */
	String[] getInterfaceNames();

	/**
	 * 返回底层类声明的成员类名数组，空数组如果没有成员类或接口。
	 *
	 * @since 3.1
	 */
	String[] getMemberClassNames();

}
