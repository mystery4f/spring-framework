/*
 * Copyright 2002-2021 the original author or authors.
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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Extension of the standard JavaBeans {@link PropertyDescriptor} class,
 * overriding {@code getPropertyType()} such that a generically declared
 * type variable will be resolved against the containing bean class.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
final class GenericTypeAwarePropertyDescriptor extends PropertyDescriptor {

	private final Class<?> beanClass;

	@Nullable
	private final Method readMethod;

	@Nullable
	private final Method writeMethod;

	@Nullable
	private volatile Set<Method> ambiguousWriteMethods;

	@Nullable
	private MethodParameter writeMethodParameter;

	@Nullable
	private Class<?> propertyType;

	@Nullable
	private final Class<?> propertyEditorClass;


	public GenericTypeAwarePropertyDescriptor(Class<?> beanClass, String propertyName,
			@Nullable Method readMethod, @Nullable Method writeMethod,
			@Nullable Class<?> propertyEditorClass) throws IntrospectionException {

		// 调用父类的构造函数，传入参数propertyName、readMethod、writeMethod
		super(propertyName, null, null);
		// 将beanClass赋值给成员变量
		this.beanClass = beanClass;

		// 如果readMethod不为空，查找bridge方法
		Method readMethodToUse = (readMethod != null ? BridgeMethodResolver.findBridgedMethod(readMethod) : null);
		// 如果writeMethod不为空，查找bridge方法
		Method writeMethodToUse = (writeMethod != null ? BridgeMethodResolver.findBridgedMethod(writeMethod) : null);
		// 如果writeMethod为空，且readMethod不为空，查找set方法
		if (writeMethodToUse == null && readMethodToUse != null) {
			// Fallback: Original JavaBeans introspection might not have found matching setter
			// method due to lack of bridge method resolution, in case of the getter using a
			// covariant return type whereas the setter is defined for the concrete property type.
			Method candidate = ClassUtils.getMethodIfAvailable(
					this.beanClass, "set" + StringUtils.capitalize(getName()), (Class<?>[]) null);
			// 如果找到set方法，且参数个数为1，则赋值给writeMethodToUse
			if (candidate != null && candidate.getParameterCount() == 1) {
				writeMethodToUse = candidate;
			}
		}
		// 将readMethodToUse赋值给成员变量
		this.readMethod = readMethodToUse;
		// 将writeMethodToUse赋值给成员变量
		this.writeMethod = writeMethodToUse;

		// 如果writeMethod不为空，则查找ambiguousWriteMethods
		if (this.writeMethod != null) {
			// 如果readMethod为空，则设置ambiguousWriteMethods
			if (this.readMethod == null) {
				// Write method not matched against read method: potentially ambiguous through
				// several overloaded variants, in which case an arbitrary winner has been chosen
				// by the JDK's JavaBeans Introspector...
				Set<Method> ambiguousCandidates = new HashSet<>();
				for (Method method : beanClass.getMethods()) {
					// 判断method的名称、参数个数是否与writeMethodToUse相同，且不等于writeMethodToUse，不等于bridge方法，不等于抽象方法
					if (method.getName().equals(writeMethodToUse.getName()) &&
							!method.equals(writeMethodToUse) && !method.isBridge() &&
							method.getParameterCount() == writeMethodToUse.getParameterCount()) {
						ambiguousCandidates.add(method);
					}
				}
				// 如果ambiguousCandidates不为空，则设置ambiguousWriteMethods
				if (!ambiguousCandidates.isEmpty()) {
					this.ambiguousWriteMethods = ambiguousCandidates;
				}
			}
			// 查找writeMethodParameter
			this.writeMethodParameter = new MethodParameter(this.writeMethod, 0).withContainingClass(this.beanClass);
		}

		// 如果readMethod不为空，则查找propertyType
		if (this.readMethod != null) {
			// Resolve generic type through return type
			this.propertyType = GenericTypeResolver.resolveReturnType(this.readMethod, this.beanClass);
		}
		// 如果writeMethodParameter不为空，则查找propertyType
		else if (this.writeMethodParameter != null) {
			// Resolve generic type through parameter type
			this.propertyType = this.writeMethodParameter.getParameterType();
		}

		// 将propertyEditorClass赋值给成员变量
		this.propertyEditorClass = propertyEditorClass;
	}


	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	@Override
	@Nullable
	public Method getReadMethod() {
		return this.readMethod;
	}

	@Override
	@Nullable
	public Method getWriteMethod() {
		return this.writeMethod;
	}

	public Method getWriteMethodForActualAccess() {
		Assert.state(this.writeMethod != null, "No write method available");
		Set<Method> ambiguousCandidates = this.ambiguousWriteMethods;
		if (ambiguousCandidates != null) {
			this.ambiguousWriteMethods = null;
			LogFactory.getLog(GenericTypeAwarePropertyDescriptor.class).debug("Non-unique JavaBean property '" +
					getName() + "' being accessed! Ambiguous write methods found next to actually used [" +
					this.writeMethod + "]: " + ambiguousCandidates);
		}
		return this.writeMethod;
	}

	public MethodParameter getWriteMethodParameter() {
		Assert.state(this.writeMethodParameter != null, "No write method available");
		return this.writeMethodParameter;
	}

	@Override
	@Nullable
	public Class<?> getPropertyType() {
		return this.propertyType;
	}

	@Override
	@Nullable
	public Class<?> getPropertyEditorClass() {
		return this.propertyEditorClass;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof GenericTypeAwarePropertyDescriptor)) {
			return false;
		}
		GenericTypeAwarePropertyDescriptor otherPd = (GenericTypeAwarePropertyDescriptor) other;
		return (getBeanClass().equals(otherPd.getBeanClass()) && PropertyDescriptorUtils.equals(this, otherPd));
	}

	@Override
	public int hashCode() {
		int hashCode = getBeanClass().hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getReadMethod());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getWriteMethod());
		return hashCode;
	}

}
