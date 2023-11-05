package indi.shui4.thinking.spring.bean.lifecycle;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * Bean 实例化生命周期示例
 *
 * @author shui4
 */
@SuppressWarnings("squid:S125")
public class BeanInstantiationLifecycleDemo {
	public static final String SUPER_USER = "superUser";


	public static void main(String[] args) {
//		executeBeanFactory();
		System.out.println("-------------------------------------------------");
		executeApplicationContext();
	}


	public static void executeBeanFactory() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		// 添加BeanPostProcessor 实现（示例）
		//- 方法1
		beanFactory.addBeanPostProcessor(new MyInstantiationAwareBeanPostProcessor());
		// - 方法2：该方式在 BeanFactory中无效
		// <bean class="indi.shui4.thinking.spring.bean.lifecycle.MyInstantiationAwareBeanPostProcessor"/>

		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		String[] locations = {"classpath:META-INF/dependency-lookup-context.xml", "classpath:META-INF/bean-constructor-dependency-injection.xml"};
		beanDefinitionReader.loadBeanDefinitions(locations);
		// 构造器注入按照类型注入，resolveDependency
		System.out.println(beanFactory.getBean("userHolder"));
		System.out.println(beanFactory.getBean(SUPER_USER));

	}

	public static void executeApplicationContext() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		// 添加BeanPostProcessor 实现（示例）
		String[] locations = {"classpath:META-INF/dependency-lookup-context.xml", "classpath:META-INF/bean-constructor-dependency-injection.xml"};
		applicationContext.setConfigLocations(locations);
		// 启动应用上下文
		applicationContext.refresh();
		// 构造器注入按照类型注入，resolveDependency
		System.out.println(applicationContext.getBean("userHolder"));
		System.out.println(applicationContext.getBean(SUPER_USER));
		// 关闭应用上下文
		applicationContext.close();
	}

}

