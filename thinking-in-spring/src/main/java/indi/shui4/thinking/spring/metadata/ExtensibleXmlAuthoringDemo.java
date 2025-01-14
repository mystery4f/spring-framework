package indi.shui4.thinking.spring.metadata;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * Spring XML 元素扩展示例
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since
 */
public class ExtensibleXmlAuthoringDemo {

	public static void main(String[] args) {

		// 创建 IoC 底层容器
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		// 创建 XML 资源的 BeanDefinitionReader
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		// 记载 XML 资源
		reader.loadBeanDefinitions("META-INF/users-context.xml");
		// 获取 User Bean 对象
		User user = beanFactory.getBean(User.class);
		System.out.println(user);
	}
}
