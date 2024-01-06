package indi.shui4.thinking.spring.metadata;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

import java.util.Map;

/**
 * @author shui4
 */

@SuppressWarnings("unchecked")
public class XmlBasedYamlPropertySourceDemo {
	public static void main(String[] args) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions("META-INF/yaml-property-source-context.xml");
		Map<String, Object> yamlMap = beanFactory.getBean("yamlMap", Map.class);
		System.out.println(yamlMap);
	}
}
