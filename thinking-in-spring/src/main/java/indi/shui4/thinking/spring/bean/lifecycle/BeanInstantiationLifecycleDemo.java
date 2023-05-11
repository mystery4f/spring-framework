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

	private static final String SUPER_USER = "superUser";

	public static void main(String[] args) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		// 添加BeanPostProcessor 实现（示例）
		beanFactory.addBeanPostProcessor(new MyInstantiationAwareBeanPostProcessor());
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		String[] locations = {"classpath:META-INF/dependency-lookup-context.xml", "classpath:META-INF/bean-constructor-dependency-injection.xml"};
		beanDefinitionReader.loadBeanDefinitions(locations);
		System.out.println(beanFactory.getBean(SUPER_USER));
		// 构造器注入按照类型注入，resolveDependency
		System.out.println(beanFactory.getBean("userHolder"));
	}

	public static class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
		private static boolean isSupperUser(Class<?> beanClass, String beanName) {
			return ObjectUtils.nullSafeEquals(SUPER_USER, beanName) && SuperUser.class.equals(beanClass);
		}

		@Override
		public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
			if (isSupperUser(beanClass, beanName)) {
				// 把配置完成 superUser 覆盖
				// return new SuperUser();
				return null;
			}
			// 保持 Spring IoC 容器实例化操作
			return null;
		}

		@Override
		public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
			if (isSupperUser(bean.getClass(), beanName)) {
				SuperUser user = (SuperUser) bean;
				user.setId(System.currentTimeMillis());
				user.setName("shui4");
				// 对象不允许属性赋值
				return false;
			}
			return true;
		}
	}
}
