/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.*;

/**
 * Default {@link BeanWrapper} implementation that should be sufficient
 * for all typical use cases. Caches introspection results for efficiency.
 *
 * <p>Note: Auto-registers default property editors from the
 * {@code org.springframework.beans.propertyeditors} package, which apply
 * in addition to the JDK's standard PropertyEditors. Applications can call
 * the {@link #registerCustomEditor(Class, java.beans.PropertyEditor)} method
 * to register an editor for a particular instance (i.e. they are not shared
 * across the application). See the base class
 * {@link PropertyEditorRegistrySupport} for details.
 *
 * <p><b>NOTE: As of Spring 2.5, this is - for almost all purposes - an
 * internal class.</b> It is just public in order to allow for access from
 * other framework packages. For standard application access purposes, use the
 * {@link PropertyAccessorFactory#forBeanPropertyAccess} factory method instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 * @since 15 April 2001
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

	/**
	 * 缓存的 JavaBeans 反射结果，以避免每次都 encountered the cost of JavaBeans introspection.
	 */
	@Nullable
	private CachedIntrospectionResults cachedIntrospectionResults;

	/**
	 * 用于调用属性方法的安全上下文。
	 */
	@Nullable
	private AccessControlContext acc;


	/**
	 * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
	 * Registers default editors.
	 *
	 * @see #setWrappedInstance
	 */
	public BeanWrapperImpl() {
		this(true);
	}

	/**
	 * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
	 *
	 * @param registerDefaultEditors whether to register default editors
	 *                               (can be suppressed if the BeanWrapper won't need any type conversion)
	 * @see #setWrappedInstance
	 */
	public BeanWrapperImpl(boolean registerDefaultEditors) {
		super(registerDefaultEditors);
	}

	/**
	 * Create a new BeanWrapperImpl for the given object.
	 *
	 * @param object the object wrapped by this BeanWrapper
	 */
	public BeanWrapperImpl(Object object) {
		super(object);
	}

	/**
	 * Create a new BeanWrapperImpl, wrapping a new instance of the specified class.
	 *
	 * @param clazz class to instantiate and wrap
	 */
	public BeanWrapperImpl(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * Create a new BeanWrapperImpl for the given object,
	 * registering a nested path that the object is in.
	 *
	 * @param object     the object wrapped by this BeanWrapper
	 * @param nestedPath the nested path of the object
	 * @param rootObject the root object at the top of the path
	 */
	public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
		super(object, nestedPath, rootObject);
	}

	/**
	 * Create a new BeanWrapperImpl for the given object,
	 * registering a nested path that the object is in.
	 *
	 * @param object     the object wrapped by this BeanWrapper
	 * @param nestedPath the nested path of the object
	 * @param parent     the containing BeanWrapper (must not be {@code null})
	 */
	private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
		super(object, nestedPath, parent);
		setSecurityContext(parent.acc);
	}


	/**
	 * Set a bean instance to hold, without any unwrapping of {@link java.util.Optional}.
	 *
	 * @param object the actual target object
	 * @see #setWrappedInstance(Object)
	 * @since 4.3
	 */
	public void setBeanInstance(Object object) {
		this.wrappedObject = object;
		this.rootObject = object;
		this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
		setIntrospectionClass(object.getClass());
	}

	/**
	 * Set the class to introspect.
	 * Needs to be called when the target object changes.
	 *
	 * @param clazz the class to introspect
	 */
	protected void setIntrospectionClass(Class<?> clazz) {
		if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
			this.cachedIntrospectionResults = null;
		}
	}

	@Override
	public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
		super.setWrappedInstance(object, nestedPath, rootObject);
		setIntrospectionClass(getWrappedClass());
	}

	/**
	 * Return the security context used during the invocation of the wrapped instance methods.
	 * Can be null.
	 */
	@Nullable
	public AccessControlContext getSecurityContext() {
		return this.acc;
	}

	/**
	 * Set the security context used during the invocation of the wrapped instance methods.
	 * Can be null.
	 */
	public void setSecurityContext(@Nullable AccessControlContext acc) {
		this.acc = acc;
	}

	/**
	 * Convert the given value for the specified property to the latter's type.
	 * <p>This method is only intended for optimizations in a BeanFactory.
	 * Use the {@code convertIfNecessary} methods for programmatic conversion.
	 *
	 * @param value        the value to convert
	 * @param propertyName the target property
	 *                     (note that nested or indexed properties are not supported here)
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 */
	@Nullable
	public Object convertForProperty(@Nullable Object value, String propertyName) throws TypeMismatchException {
		CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
		PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
		if (pd == null) {
			throw new InvalidPropertyException(getRootClass(),
					getNestedPath() + propertyName,
					"No property '" + propertyName + "' found"
			);
		}
		TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
		if (td == null) {
			td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
		}
		return convertForProperty(propertyName, null, value, td);
	}

	/**
	 * Obtain a lazily initialized CachedIntrospectionResults instance
	 * for the wrapped object.
	 */
	private CachedIntrospectionResults getCachedIntrospectionResults() {
		// 如果缓存的 IntrospectionResults 为空
		if (this.cachedIntrospectionResults == null) {
			// 缓存当前类的 IntrospectionResults
			this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
		}
		// 返回缓存的 IntrospectionResults
		return this.cachedIntrospectionResults;
	}

	// 创建一个Property对象，用于存放属性的信息
	private Property property(PropertyDescriptor pd) {
		// 将 PropertyDescriptor 转换为 GenericTypeAwarePropertyDescriptor
		GenericTypeAwarePropertyDescriptor gpd = (GenericTypeAwarePropertyDescriptor) pd;
		// 创建一个新的 Property 对象，用于存放属性的信息
		return new Property(gpd.getBeanClass(), gpd.getReadMethod(), gpd.getWriteMethod(), gpd.getName());
	}

	@Override
	@Nullable
	// 根据属性名获取本地属性处理器
	protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
		// 获取属性描述符
		PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
		// 如果属性描述符不为空，则创建一个新的 Bean 属性处理器，否则返回 null
		return (pd != null ? new BeanPropertyHandler(pd) : null);
	}

	@Override
	protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
		return new BeanWrapperImpl(object, nestedPath, this);
	}

	@Override
	protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
		// 创建一个 PropertyMatches 对象，用于比较指定属性名下的 Property
		PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
		// 抛出一个 NotWritablePropertyException，表示属性不可写
		throw new NotWritablePropertyException(getRootClass(),
				getNestedPath() + propertyName,
				matches.buildErrorMessage(),
				matches.getPossibleMatches()
		);
	}

	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		return getCachedIntrospectionResults().getPropertyDescriptors();
	}

	@Override
	public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
		BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
		String finalPath = getFinalPath(nestedBw, propertyName);
		PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
		if (pd == null) {
			throw new InvalidPropertyException(getRootClass(),
					getNestedPath() + propertyName,
					"No property '" + propertyName + "' found"
			);
		}
		return pd;
	}


	private class BeanPropertyHandler extends PropertyHandler {

		private final PropertyDescriptor pd;

		public BeanPropertyHandler(PropertyDescriptor pd) {
			super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
			this.pd = pd;
		}

		@Override
		public ResolvableType getResolvableType() {
			return ResolvableType.forMethodReturnType(this.pd.getReadMethod());
		}

		@Override
		public TypeDescriptor toTypeDescriptor() {
			return new TypeDescriptor(property(this.pd));
		}

		@Override
		@Nullable
		public TypeDescriptor nested(int level) {
			return TypeDescriptor.nested(property(this.pd), level);
		}

		@Override
		@Nullable
		public Object getValue() throws Exception {
			Method readMethod = this.pd.getReadMethod();
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(readMethod);
					return null;
				});
				try {
					return AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> readMethod.invoke(getWrappedInstance(),
							(Object[]) null
					), acc);
				} catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			} else {
				ReflectionUtils.makeAccessible(readMethod);
				return readMethod.invoke(getWrappedInstance(), (Object[]) null);
			}
		}

		@Override
		public void setValue(@Nullable Object value) throws Exception {
			// 获取写方法
			Method writeMethod = (this.pd instanceof GenericTypeAwarePropertyDescriptor ? ((GenericTypeAwarePropertyDescriptor) this.pd).getWriteMethodForActualAccess() : this.pd.getWriteMethod());
			// 如果安全控制器不为空
			if (System.getSecurityManager() != null) {
				// 执行特权操作
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					// 使写方法可访问
					ReflectionUtils.makeAccessible(writeMethod);
					return null;
				});
				// 再次执行特权操作
				try {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> writeMethod.invoke(
							// 获取包装实例
							getWrappedInstance(),
							// 传入值
							value
					), acc);
				} catch (PrivilegedActionException ex) {
					// 抛出异常
					throw ex.getException();
				}
			} else {
				// 使写方法可访问
				ReflectionUtils.makeAccessible(writeMethod);
				// 执行写方法
				writeMethod.invoke(getWrappedInstance(), value);
			}
		}
	}

}
