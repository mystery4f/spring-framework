package indi.shui4.thinking.spring.beans;

import java.beans.*;
import java.util.stream.Stream;

/**
 * BeanInfo
 *
 * @author shui4
 */
public class BeanInfoDemo {
	public static void main(String[] args) throws IntrospectionException {
		BeanInfo beanInfo = Introspector.getBeanInfo(Person.class, Object.class);
		// PropertyDescriptor 在Spring中应用非常多
		Stream.of(beanInfo.getPropertyDescriptors()).forEach(propertyDescriptor -> {
			Class<?> propertyType = propertyDescriptor.getPropertyType();
			String name = propertyType.getName();
			// age 添加 PropertyEditor
			if ("age".equals(name)) {
				propertyDescriptor.setPropertyEditorClass(StringToIntegerPropertyEditor.class);
			}
			System.out.println(propertyDescriptor);
		});
	}


	static class StringToIntegerPropertyEditor extends PropertyEditorSupport {
		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			Integer value = Integer.valueOf(text);
			setValue(value);
		}
	}
}
