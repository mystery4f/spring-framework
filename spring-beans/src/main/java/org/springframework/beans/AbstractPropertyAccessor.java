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

import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of the {@link PropertyAccessor} interface.
 * Provides base implementations of all convenience methods, with the
 * implementation of actual property access left to subclasses.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #getPropertyValue
 * @see #setPropertyValue
 * @since 2.0
 */
public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements ConfigurablePropertyAccessor {

	boolean suppressNotWritablePropertyException = false;
	private boolean extractOldValueForEditor = false;
	private boolean autoGrowNestedPaths = false;

	@Override
	public boolean isExtractOldValueForEditor() {
		return this.extractOldValueForEditor;
	}

	@Override
	public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
		this.extractOldValueForEditor = extractOldValueForEditor;
	}

	@Override
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	@Override
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	@Override
	public void setPropertyValues(Map<?, ?> map) throws BeansException {
		setPropertyValues(new MutablePropertyValues(map));
	}

	@Override
	public void setPropertyValues(PropertyValues pvs) throws BeansException {
		setPropertyValues(pvs, false, false);
	}

	@Override
	public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException {

		List<PropertyAccessException> propertyAccessExceptions = null;
		List<PropertyValue> propertyValues = (pvs instanceof MutablePropertyValues ?
				((MutablePropertyValues) pvs).getPropertyValueList() : Arrays.asList(pvs.getPropertyValues()));

		if (ignoreUnknown) {
			this.suppressNotWritablePropertyException = true;
		}
		try {
			for (PropertyValue pv : propertyValues) {
				// setPropertyValue may throw any BeansException, which won't be caught
				// here, if there is a critical failure such as no matching field.
				// We can attempt to deal only with less serious exceptions.
				try {
					setPropertyValue(pv);
				} catch (NotWritablePropertyException ex) {
					if (!ignoreUnknown) {
						throw ex;
					}
					// Otherwise, just ignore it and continue...
				} catch (NullValueInNestedPathException ex) {
					if (!ignoreInvalid) {
						throw ex;
					}
					// Otherwise, just ignore it and continue...
				} catch (PropertyAccessException ex) {
					if (propertyAccessExceptions == null) {
						propertyAccessExceptions = new ArrayList<>();
					}
					propertyAccessExceptions.add(ex);
				}
			}
		} finally {
			if (ignoreUnknown) {
				this.suppressNotWritablePropertyException = false;
			}
		}

		// If we encountered individual exceptions, throw the composite exception.
		if (propertyAccessExceptions != null) {
			PropertyAccessException[] paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[0]);
			throw new PropertyBatchUpdateException(paeArray);
		}
	}

	@Override
	public void setPropertyValue(PropertyValue pv) throws BeansException {
		setPropertyValue(pv.getName(), pv.getValue());
	}

	/**
	 * 实际上设置一个属性值。
	 *
	 * @param propertyName 要设置值的属性的名称
	 * @param value        新值
	 * @throws InvalidPropertyException 如果不存在这样的属性或者属性不可写
	 * @throws PropertyAccessException  如果属性有效但访问器方法失败或类型不匹配
	 */
	@Override
	public abstract void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

	@Override
	public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) throws BeansException {
		setPropertyValues(pvs, ignoreUnknown, false);
	}

	// Redefined with public visibility.
	@Override
	@Nullable
	public Class<?> getPropertyType(String propertyPath) {
		return null;
	}

	/**
	 * 实际上获取一个属性的值。
	 *
	 * @param propertyName 属性名，用于获取其值
	 * @return 属性的值
	 * @throws InvalidPropertyException 如果没有这样的属性或者属性不可读
	 * @throws PropertyAccessException  如果属性有效，但是访问器方法失败
	 */
	@Override
	@Nullable
	public abstract Object getPropertyValue(String propertyName) throws BeansException;

}
