package indi.shui4.thinking.spring.bean.lifecycle;

import indi.shui4.thinking.spring.ioc.overview.domain.SuperUser;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.util.ObjectUtils;

/**
 * Bean 实例化生命周期示例
 *
 * @author shui4
 */
public class BeanInstantiationLifecycleDemo {
	public static void main(String[] args) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		// 添加BeanPostProcessor 实现（示例）
		beanFactory.addBeanPostProcessor(new MyInstantiationAwareBeanPostProcessor());
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions("classpath:META-INF/dependency-lookup-context.xml");
		System.out.println(beanFactory.getBean("superUser"));
	}

	public static class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
		@Override
		public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
			if (ObjectUtils.nullSafeEquals("superUser", beanName) && SuperUser.class.equals(beanClass)) {
				// 把配置完成 superUser 覆盖
				return new SuperUser();
			}
			// 保持 Spring IoC 容器实例化操作
			return null;
		}
	}
}