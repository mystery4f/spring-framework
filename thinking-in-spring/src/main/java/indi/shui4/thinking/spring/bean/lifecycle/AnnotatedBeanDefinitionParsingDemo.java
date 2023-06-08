package indi.shui4.thinking.spring.bean.lifecycle;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.stereotype.Component;

/**
 * 注解 BeanDefinition 解析示例
 *
 * @author shui4
 */
public class AnnotatedBeanDefinitionParsingDemo {
	public static void main(String[] args) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		// 基于 Java 注释 AnnotatedBeanDefinitionReader 的实现
		AnnotatedBeanDefinitionReader annotatedBeanDefinitionReader = new AnnotatedBeanDefinitionReader(beanFactory);
		annotatedBeanDefinitionReader.setBeanNameGenerator(AnnotationBeanNameGenerator.INSTANCE);
		int beanDefinitionCountBefore = beanFactory.getBeanDefinitionCount();
		// 注册当前类（非 @Component class）
		annotatedBeanDefinitionReader.registerBean(AnnotatedBeanDefinitionParsingDemo.class);
		int beanDefinitionCountAfter = beanFactory.getBeanDefinitionCount();
		System.out.println(" 已加载 BeanDefinition 数量：" + (beanDefinitionCountAfter - beanDefinitionCountBefore));
		// 普通的 Class 作为 Component 注册到 Spring IoC 容器后，通常 Bean 名称为 annotatedBeanDefinitionParsingDemo
		System.out.println(beanFactory.getBean("annotatedBeanDefinitionParsingDemo"));
	}   

}
