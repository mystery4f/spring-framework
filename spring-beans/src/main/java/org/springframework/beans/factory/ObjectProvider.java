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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A variant of {@link ObjectFactory} designed specifically for injection points,
 * allowing for programmatic optionality and lenient not-unique handling.
 *
 * <p>As of 5.1, this interface extends {@link Iterable} and provides {@link Stream}
 * support. It can be therefore be used in {@code for} loops, provides {@link #forEach}
 * iteration and allows for collection-style {@link #stream} access.
 *
 * @param <T> the object type
 * @author Juergen Hoeller
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 * @since 4.3
 */
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * 返回由该工厂管理的对象的实例（可能是共享的或独立的）。
	 * <p>允许指定显式的构造参数，类似于{@link BeanFactory#getBean(String, Object...)}。
	 *
	 * @param args 创建相应实例时要使用的参数
	 * @return bean的实例
	 * @throws BeansException 如果创建出错
	 * @see #getObject()
	 */
	T getObject(Object... args) throws BeansException;

	/**
	 * 返回由该工厂管理的对象的实例（可能是共享的或独立的）。
	 *
	 * @param defaultSupplier 如果工厂中没有该对象，则提供默认对象的回调函数
	 * @return bean的实例，如果没有这样的bean可用，则返回提供的默认对象
	 * @throws BeansException 如果创建出错
	 * @see #getIfAvailable()
	 * @since 5.0
	 */
	default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 返回由该工厂管理的对象的实例（可能是共享的或独立的）。
	 *
	 * @return bean的实例，如果不可用则返回{@code null}
	 * @throws BeansException 如果创建出错
	 * @see #getObject()
	 */
	@Nullable
	T getIfAvailable() throws BeansException;

	/**
	 * 如果可用，则消费由该工厂管理的对象的实例（可能是共享的或独立的）。
	 *
	 * @param dependencyConsumer 处理目标对象的回调函数（如果可用）（否则不调用）
	 * @throws BeansException 如果创建出错
	 * @see #getIfAvailable()
	 * @since 5.0
	 */
	default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfAvailable();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回由该工厂管理的对象的实例（可能是共享的或独立的）。
	 *
	 * @param defaultSupplier 如果工厂中没有唯一的候选项，则提供默认对象的回调函数
	 * @return bean的实例，如果没有这样的bean可用或者在工厂中不唯一（即找到多个候选项，没有一个标记为主要）则返回提供的默认对象
	 * @throws BeansException 如果创建出错
	 * @see #getIfUnique()
	 * @since 5.0
	 */
	default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfUnique();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 返回由该工厂管理的对象的实例（可能是共享的或独立的）。
	 *
	 * @return bean的实例，如果不可用或不唯一（即找到多个候选项，没有一个标记为主要）则返回{@code null}
	 * @throws BeansException 如果创建出错
	 * @see #getObject()
	 */
	@Nullable
	T getIfUnique() throws BeansException;

	/**
	 * 如果唯一，则消费由该工厂管理的对象的实例（可能是共享的或独立的）。
	 *
	 * @param dependencyConsumer 处理目标对象的回调函数（如果唯一）（否则不调用）
	 * @throws BeansException 如果创建出错
	 * @see #getIfAvailable()
	 * @since 5.0
	 */
	default void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfUnique();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回所有匹配的对象实例的{@link Iterator}，没有特定的排序保证（但通常是按注册顺序）。
	 *
	 * @see #stream()
	 * @since 5.1
	 */
	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	/**
	 * 返回所有匹配的对象实例的顺序{@link Stream}，没有特定的排序保证（但通常是按注册顺序）。
	 *
	 * @see #iterator()
	 * @see #orderedStream()
	 * @since 5.1
	 */
	default Stream<T> stream() {
		throw new UnsupportedOperationException("不支持多个元素访问");
	}

	/**
	 * 返回所有匹配的对象实例的顺序{@link Stream}，根据工厂的常规排序比较器进行预排序。
	 * <p>在标准的Spring应用程序上下文中，这将按照{@link org.springframework.core.Ordered}约定进行排序，
	 * 并且在基于注解的配置的情况下，还考虑{@link org.springframework.core.annotation.Order}注解，
	 * 类似于列表/数组类型的多元素注入点。
	 *
	 * @see #stream()
	 * @see org.springframework.core.OrderComparator
	 * @since 5.1
	 */
	default Stream<T> orderedStream() {
		throw new UnsupportedOperationException("不支持有序元素访问");
	}

}
