package indi.shui4.thinking.spring.bean.lifecycle;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

/**
 * Bean 元信息配置示例
 *
 * @author shui4
 */
@SuppressWarnings("deprecation")
public class BeanMetadataConfigurationDemo {
	public static void main(String[] args) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		PropertiesBeanDefinitionReader propertiesBeanDefinitionReader = new PropertiesBeanDefinitionReader(beanFactory);
		String location = "META-INF/user.properties";
		// 加载 properties 资源
		// 指定字符集 UTF-8
		EncodedResource encodedResource = new EncodedResource(new ClassPathResource(location), "UTF-8");
		int beanNumbers = propertiesBeanDefinitionReader.loadBeanDefinitions(encodedResource);
		System.out.println(" 加载 properties 资源，beanNumbers=" + beanNumbers);
		User user = beanFactory.getBean("user", User.class);
		System.out.println(user);
	}
}
