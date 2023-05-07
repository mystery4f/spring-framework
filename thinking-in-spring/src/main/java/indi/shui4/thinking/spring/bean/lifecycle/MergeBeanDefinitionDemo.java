package indi.shui4.thinking.spring.bean.lifecycle;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * BeanDefinition 合并示例
 *
 * @author shui4
 */
public class MergeBeanDefinitionDemo {
	public static void main(String[] args) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		int beanDefinitionCountBegin = beanFactory.getBeanDefinitionCount();
		// 基于 XML 资源 BeanDefinitionReader 实现
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions("classpath:META-INF/dependency-lookup-context.xml");
		int beanDefinitionCountAfter = beanFactory.getBeanDefinitionCount();
		System.out.println(beanDefinitionCountAfter - beanDefinitionCountBegin);
		System.out.println(beanFactory.getBean("user"));
		System.out.println(beanFactory.getBean("superUser"));
	}
}
