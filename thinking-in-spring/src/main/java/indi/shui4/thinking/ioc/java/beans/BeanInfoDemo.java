package indi.shui4.thinking.ioc.java.beans;


import java.beans.*;
import java.util.stream.Stream;

/**
 * {@link java.beans.BeanInfo} 示例
 *
 * @author shui4
 */
public class BeanInfoDemo {
    public static void main(String[] args) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(Person.class, Object.class);
        System.out.println(beanInfo);
        Stream.of(beanInfo.getPropertyDescriptors())
                .forEach(propertyDescriptor -> {
                    // 为“age”字段 / 属性增加 PropertyEditor
                    if ("age".equals(propertyDescriptor.getName())) {
                        propertyDescriptor.setPropertyEditorClass(StringToIntegerPropertyEditor.class);
                    }
                });
    }

    /**
     * StringToIntegerPropertyEditor
     *
     * @author shui4
     */
    private static class StringToIntegerPropertyEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            Integer value = Integer.valueOf(text);
            setValue(value);
        }
    }

}