package indi.shui4.thinking.spring.bean.definition;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * {@link BeanDefinition} 构建示例
 *
 * @author shui4
 */
public class BeanDefinitionCreationDemo {

	public static void main(String[] args) {
		// 1. 通过 BeanDefinitionBuilder 构建
		// BeanDefinition 并非 Bean 最终状态，可以自定义修改
		// rootBeanDefinition 的 Bean 不能有 parent
		// BeanDefinitionBuilder.rootBeanDefinition(User.class);
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(User.class)
				// 通过属性设置
				.addPropertyValue("id", 1).addPropertyValue("name", "shui4");

		// 获取 BeanDefinition 对象
		BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();

		// 2. 通过 AbstractBeanDefinition 以及派生类
		GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
		genericBeanDefinition.setBeanClass(User.class);
		// 通过 MutablePropertyValues 批量操作属性
		MutablePropertyValues propertyValues = new MutablePropertyValues().add("id", 1).add("name", "shui4");
//        propertyValues.addPropertyValue("id", 1);
//        propertyValues.addPropertyValue("name", "shui4");
		genericBeanDefinition.setPropertyValues(propertyValues);

	}
}