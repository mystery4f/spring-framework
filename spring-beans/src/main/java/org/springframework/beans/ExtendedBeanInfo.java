/*
 * Copyright 2002-2019 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.awt.*;
import java.beans.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.*;

/**
 * Decorator for a standard {@link BeanInfo} object, e.g. as created by
 * {@link Introspector#getBeanInfo(Class)}, designed to discover and register
 * static and/or non-void returning setter methods. For example:
 *
 * <pre class="code">
 * public class Bean {
 *
 *     private Foo foo;
 *
 *     public Foo getFoo() {
 *         return this.foo;
 *     }
 *
 *     public Bean setFoo(Foo foo) {
 *         this.foo = foo;
 *         return this;
 *     }
 * }</pre>
 * <p>
 * The standard JavaBeans {@code Introspector} will discover the {@code getFoo} read
 * method, but will bypass the {@code #setFoo(Foo)} write method, because its non-void
 * returning signature does not comply with the JavaBeans specification.
 * {@code ExtendedBeanInfo}, on the other hand, will recognize and include it. This is
 * designed to allow APIs with "builder" or method-chaining style setter signatures to be
 * used within Spring {@code <beans>} XML. {@link #getPropertyDescriptors()} returns all
 * existing property descriptors from the wrapped {@code BeanInfo} as well any added for
 * non-void returning setters. Both standard ("non-indexed") and
 * <a href="https://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html">
 * indexed properties</a> are fully supported.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see #ExtendedBeanInfo(BeanInfo)
 * @see ExtendedBeanInfoFactory
 * @see CachedIntrospectionResults
 * @since 3.1
 */
class ExtendedBeanInfo implements BeanInfo {

	private static final Log logger = LogFactory.getLog(ExtendedBeanInfo.class);

	private final BeanInfo delegate;

	private final Set<PropertyDescriptor> propertyDescriptors = new TreeSet<>(new PropertyDescriptorComparator());


	/**
	 * Wrap the given {@link BeanInfo} instance; copy all its existing property descriptors
	 * locally, wrapping each in a custom {@link SimpleIndexedPropertyDescriptor indexed}
	 * or {@link SimplePropertyDescriptor non-indexed} {@code PropertyDescriptor}
	 * variant that bypasses default JDK weak/soft reference management; then search
	 * through its method descriptors to find any non-void returning write methods and
	 * update or create the corresponding {@link PropertyDescriptor} for each one found.
	 *
	 * @param delegate the wrapped {@code BeanInfo}, which is never modified
	 * @see #getPropertyDescriptors()
	 */
	public ExtendedBeanInfo(BeanInfo delegate) {
		// 设置代理对象
		this.delegate = delegate;
		// 获取代理对象的属性描述
		for (PropertyDescriptor pd : delegate.getPropertyDescriptors()) {
			try {
				// 判断属性描述是否为IndexedPropertyDescriptor实例
				this.propertyDescriptors.add(pd instanceof IndexedPropertyDescriptor ?
						// 如果是，则创建新的SimpleIndexedPropertyDescriptor对象
						new SimpleIndexedPropertyDescriptor((IndexedPropertyDescriptor) pd) :
						// 否则，创建新的SimplePropertyDescriptor对象
						new SimplePropertyDescriptor(pd));
			} catch (IntrospectionException ex) {
				// 可能是一个不符合JavaBeans模式的方法
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring invalid bean property '" + pd.getName() + "': " + ex.getMessage());
				}
			}
		}
		// 获取代理对象的方法描述
		MethodDescriptor[] methodDescriptors = delegate.getMethodDescriptors();
		if (methodDescriptors != null) {
			for (Method method : findCandidateWriteMethods(methodDescriptors)) {
				try {
					// 处理候选的写方法
					handleCandidateWriteMethod(method);
				} catch (IntrospectionException ex) {
					// 我们只是尝试找到候选方法， easily ignore extra ones here...
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring candidate write method [" + method + "]: " + ex.getMessage());
					}
				}
			}
		}
	}


	private List<Method> findCandidateWriteMethods(MethodDescriptor[] methodDescriptors) {
		List<Method> matches = new ArrayList<>();
		for (MethodDescriptor methodDescriptor : methodDescriptors) {
			Method method = methodDescriptor.getMethod();
			if (isCandidateWriteMethod(method)) {
				matches.add(method);
			}
		}
		// Sort non-void returning write methods to guard against the ill effects of
		// non-deterministic sorting of methods returned from Class#getDeclaredMethods
		// under JDK 7. See https://bugs.java.com/view_bug.do?bug_id=7023180
		matches.sort((m1, m2) -> m2.toString().compareTo(m1.toString()));
		return matches;
	}

	private void handleCandidateWriteMethod(Method method) throws IntrospectionException {
		// 获取方法参数数量
		int nParams = method.getParameterCount();
		// 获取属性名
		String propertyName = propertyNameFor(method);
		// 获取属性类型
		Class<?> propertyType = method.getParameterTypes()[nParams - 1];
		// 查找现有的属性描述器
		PropertyDescriptor existingPd = findExistingPropertyDescriptor(propertyName, propertyType);
		// 参数数量为1
		if (nParams == 1) {
			// 如果没有现有的属性描述器
			if (existingPd == null) {
				// 添加新的属性描述器
				this.propertyDescriptors.add(new SimplePropertyDescriptor(propertyName, null, method));
			} else {
				// 否则，设置写方法
				existingPd.setWriteMethod(method);
			}

		}
		// 参数数量为2
		else if (nParams == 2) {
			// 如果没有现有的属性描述器
			if (existingPd == null) {
				// 添加新的属性描述器
				this.propertyDescriptors.add(
						new SimpleIndexedPropertyDescriptor(propertyName, null, null, null, method));
				// 否则，如果属性描述器为IndexedPropertyDescriptor
			} else if (existingPd instanceof IndexedPropertyDescriptor) {
				// 设置索引写方法
				((IndexedPropertyDescriptor) existingPd).setIndexedWriteMethod(method);
				// 否则
			} else {
				// 移除现有的属性描述器
				this.propertyDescriptors.remove(existingPd);
				// 添加新的属性描述器
				this.propertyDescriptors.add(new SimpleIndexedPropertyDescriptor(
						propertyName, existingPd.getReadMethod(), existingPd.getWriteMethod(), null, method));
			}

		} else {
			// 抛出异常
			throw new IllegalArgumentException("Write method must have exactly 1 or 2 parameters: " + method);
		}
	}

	public static boolean isCandidateWriteMethod(Method method) {
		String methodName = method.getName();
		int nParams = method.getParameterCount();
		return (methodName.length() > 3 && methodName.startsWith("set") && Modifier.isPublic(method.getModifiers()) &&
				(!void.class.isAssignableFrom(method.getReturnType()) || Modifier.isStatic(method.getModifiers())) &&
				(nParams == 1 || (nParams == 2 && int.class == method.getParameterTypes()[0])));
	}

	private String propertyNameFor(Method method) {
		return Introspector.decapitalize(method.getName().substring(3));
	}

	@Nullable
	private PropertyDescriptor findExistingPropertyDescriptor(String propertyName, Class<?> propertyType) {
		for (PropertyDescriptor pd : this.propertyDescriptors) {
			// 获取属性的类型
			final Class<?> candidateType;
			// 获取属性的名称
			final String candidateName = pd.getName();
			// 如果属性描述器是索引映射属性描述器
			if (pd instanceof IndexedPropertyDescriptor) {
				// 将索引映射属性描述器赋值给ipd
				IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
				// 获取索引映射属性的类型
				candidateType = ipd.getIndexedPropertyType();
				// 如果属性的名称等于给定的属性名称，并且属性的类型等于给定的属性类型，或者属性的类型等于给定的属性类型的组件类型
				if (candidateName.equals(propertyName) &&
						(candidateType.equals(propertyType) || candidateType.equals(propertyType.getComponentType()))) {
					// 返回属性描述器
					return pd;
				}
			} else {
				// 获取属性类型
				candidateType = pd.getPropertyType();
				// 如果属性的名称等于给定的属性名称，并且属性的类型等于给定的属性类型，或者属性的类型等于给定的属性类型的组件类型
				if (candidateName.equals(propertyName) &&
						(candidateType.equals(propertyType) || propertyType.equals(candidateType.getComponentType()))) {
					// 返回属性描述器
					return pd;
				}
			}
		}
		// 如果找不到属性描述器，返回null
		return null;
	}

	/**
	 * Return the set of {@link PropertyDescriptor PropertyDescriptors} from the wrapped
	 * {@link BeanInfo} object as well as {@code PropertyDescriptors} for each non-void
	 * returning setter method found during construction.
	 *
	 * @see #ExtendedBeanInfo(BeanInfo)
	 */
	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		return this.propertyDescriptors.toArray(new PropertyDescriptor[0]);
	}

	@Override
	public BeanInfo[] getAdditionalBeanInfo() {
		return this.delegate.getAdditionalBeanInfo();
	}

	@Override
	public BeanDescriptor getBeanDescriptor() {
		return this.delegate.getBeanDescriptor();
	}

	@Override
	public int getDefaultEventIndex() {
		return this.delegate.getDefaultEventIndex();
	}

	@Override
	public int getDefaultPropertyIndex() {
		return this.delegate.getDefaultPropertyIndex();
	}

	@Override
	public EventSetDescriptor[] getEventSetDescriptors() {
		return this.delegate.getEventSetDescriptors();
	}

	@Override
	public Image getIcon(int iconKind) {
		return this.delegate.getIcon(iconKind);
	}

	@Override
	public MethodDescriptor[] getMethodDescriptors() {
		return this.delegate.getMethodDescriptors();
	}


	/**
	 * A simple {@link PropertyDescriptor}.
	 */
	static class SimplePropertyDescriptor extends PropertyDescriptor {

		// 声明一个Method类型的变量，用于存储读取属性的方法
		@Nullable
		private Method readMethod;

		// 声明一个Method类型的变量，用于存储写入属性的方法
		@Nullable
		private Method writeMethod;

		// 声明一个Class<?>类型的变量，用于存储属性类型
		@Nullable
		private Class<?> propertyType;

		// 声明一个Class<?>类型的变量，用于存储属性编辑器类
		@Nullable
		private Class<?> propertyEditorClass;

		public SimplePropertyDescriptor(PropertyDescriptor original) throws IntrospectionException {
			this(original.getName(), original.getReadMethod(), original.getWriteMethod());
			PropertyDescriptorUtils.copyNonMethodProperties(original, this);
		}

		public SimplePropertyDescriptor(String propertyName, @Nullable Method readMethod, Method writeMethod)
				throws IntrospectionException {

			super(propertyName, null, null);
			this.readMethod = readMethod;
			this.writeMethod = writeMethod;
			this.propertyType = PropertyDescriptorUtils.findPropertyType(readMethod, writeMethod);
		}

		@Override
		@Nullable
		public Class<?> getPropertyEditorClass() {
			return this.propertyEditorClass;
		}

		@Override
		public void setPropertyEditorClass(@Nullable Class<?> propertyEditorClass) {
			this.propertyEditorClass = propertyEditorClass;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof PropertyDescriptor &&
					PropertyDescriptorUtils.equals(this, (PropertyDescriptor) other)));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(getReadMethod()) * 29 + ObjectUtils.nullSafeHashCode(getWriteMethod()));
		}

		@Override
		@Nullable
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setReadMethod(@Nullable Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		@Nullable
		public Method getWriteMethod() {
			return this.writeMethod;
		}

		@Override
		public void setWriteMethod(@Nullable Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		@Override
		public String toString() {
			return String.format("%s[name=%s, propertyType=%s, readMethod=%s, writeMethod=%s]",
					getClass().getSimpleName(), getName(), getPropertyType(), this.readMethod, this.writeMethod
			);
		}

		@Override
		@Nullable
		public Class<?> getPropertyType() {
			if (this.propertyType == null) {
				try {
					this.propertyType = PropertyDescriptorUtils.findPropertyType(this.readMethod, this.writeMethod);
				} catch (IntrospectionException ex) {
					// Ignore, as does PropertyDescriptor#getPropertyType
				}
			}
			return this.propertyType;
		}
	}


	/**
	 * A simple {@link IndexedPropertyDescriptor}.
	 */
	static class SimpleIndexedPropertyDescriptor extends IndexedPropertyDescriptor {

		@Nullable
		private Method readMethod;

		@Nullable
		private Method writeMethod;

		@Nullable
		private Class<?> propertyType;

		@Nullable
		private Method indexedReadMethod;

		@Nullable
		private Method indexedWriteMethod;

		@Nullable
		private Class<?> indexedPropertyType;

		@Nullable
		private Class<?> propertyEditorClass;

		public SimpleIndexedPropertyDescriptor(IndexedPropertyDescriptor original) throws IntrospectionException {
			this(original.getName(), original.getReadMethod(), original.getWriteMethod(),
					original.getIndexedReadMethod(), original.getIndexedWriteMethod()
			);
			PropertyDescriptorUtils.copyNonMethodProperties(original, this);
		}

		public SimpleIndexedPropertyDescriptor(String propertyName, @Nullable Method readMethod,
											   @Nullable Method writeMethod, @Nullable Method indexedReadMethod, Method indexedWriteMethod)
				throws IntrospectionException {

			super(propertyName, null, null, null, null);
			this.readMethod = readMethod;
			this.writeMethod = writeMethod;
			this.propertyType = PropertyDescriptorUtils.findPropertyType(readMethod, writeMethod);
			this.indexedReadMethod = indexedReadMethod;
			this.indexedWriteMethod = indexedWriteMethod;
			this.indexedPropertyType = PropertyDescriptorUtils.findIndexedPropertyType(
					propertyName, this.propertyType, indexedReadMethod, indexedWriteMethod);
		}

		@Override
		@Nullable
		public Class<?> getPropertyEditorClass() {
			return this.propertyEditorClass;
		}

		@Override
		public void setPropertyEditorClass(@Nullable Class<?> propertyEditorClass) {
			this.propertyEditorClass = propertyEditorClass;
		}

		/*
		 * See java.beans.IndexedPropertyDescriptor#equals
		 */
		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof IndexedPropertyDescriptor)) {
				return false;
			}
			IndexedPropertyDescriptor otherPd = (IndexedPropertyDescriptor) other;
			return (ObjectUtils.nullSafeEquals(getIndexedReadMethod(), otherPd.getIndexedReadMethod()) &&
					ObjectUtils.nullSafeEquals(getIndexedWriteMethod(), otherPd.getIndexedWriteMethod()) &&
					ObjectUtils.nullSafeEquals(getIndexedPropertyType(), otherPd.getIndexedPropertyType()) &&
					PropertyDescriptorUtils.equals(this, otherPd));
		}

		@Override
		@Nullable
		public Method getIndexedReadMethod() {
			return this.indexedReadMethod;
		}

		@Override
		public void setIndexedReadMethod(@Nullable Method indexedReadMethod) throws IntrospectionException {
			this.indexedReadMethod = indexedReadMethod;
		}

		@Override
		@Nullable
		public Method getIndexedWriteMethod() {
			return this.indexedWriteMethod;
		}

		@Override
		public void setIndexedWriteMethod(@Nullable Method indexedWriteMethod) throws IntrospectionException {
			this.indexedWriteMethod = indexedWriteMethod;
		}

		@Override
		@Nullable
		public Class<?> getIndexedPropertyType() {
			if (this.indexedPropertyType == null) {
				try {
					this.indexedPropertyType = PropertyDescriptorUtils.findIndexedPropertyType(
							getName(), getPropertyType(), this.indexedReadMethod, this.indexedWriteMethod);
				} catch (IntrospectionException ex) {
					// Ignore, as does IndexedPropertyDescriptor#getIndexedPropertyType
				}
			}
			return this.indexedPropertyType;
		}

		@Override
		@Nullable
		public Class<?> getPropertyType() {
			if (this.propertyType == null) {
				try {
					this.propertyType = PropertyDescriptorUtils.findPropertyType(this.readMethod, this.writeMethod);
				} catch (IntrospectionException ex) {
					// Ignore, as does IndexedPropertyDescriptor#getPropertyType
				}
			}
			return this.propertyType;
		}

		@Override
		public int hashCode() {
			int hashCode = ObjectUtils.nullSafeHashCode(getReadMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getWriteMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getIndexedReadMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getIndexedWriteMethod());
			return hashCode;
		}

		@Override
		@Nullable
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setReadMethod(@Nullable Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		@Nullable
		public Method getWriteMethod() {
			return this.writeMethod;
		}

		@Override
		public void setWriteMethod(@Nullable Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		@Override
		public String toString() {
			return String.format("%s[name=%s, propertyType=%s, indexedPropertyType=%s, " +
							"readMethod=%s, writeMethod=%s, indexedReadMethod=%s, indexedWriteMethod=%s]",
					getClass().getSimpleName(), getName(), getPropertyType(), getIndexedPropertyType(),
					this.readMethod, this.writeMethod, this.indexedReadMethod, this.indexedWriteMethod
			);
		}
	}


	/**
	 * Sorts PropertyDescriptor instances alpha-numerically to emulate the behavior of
	 * {@link java.beans.BeanInfo#getPropertyDescriptors()}.
	 *
	 * @see ExtendedBeanInfo#propertyDescriptors
	 */
	static class PropertyDescriptorComparator implements Comparator<PropertyDescriptor> {

		@Override
		public int compare(PropertyDescriptor desc1, PropertyDescriptor desc2) {
			String left = desc1.getName();
			String right = desc2.getName();
			byte[] leftBytes = left.getBytes();
			byte[] rightBytes = right.getBytes();
			for (int i = 0; i < left.length(); i++) {
				if (right.length() == i) {
					return 1;
				}
				int result = leftBytes[i] - rightBytes[i];
				if (result != 0) {
					return result;
				}
			}
			return left.length() - right.length();
		}
	}

}
