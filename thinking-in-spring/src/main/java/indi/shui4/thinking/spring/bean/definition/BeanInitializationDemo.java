package indi.shui4.thinking.spring.bean.definition;

import indi.shui4.thinking.spring.bean.factory.DefaultUserFactory;
import indi.shui4.thinking.spring.bean.factory.UserFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * Bean 初始化 Demo
 *
 * @author shui4
 */
public class BeanInitializationDemo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(BeanInitializationDemo.class);
		applicationContext.refresh();
		System.out.println("Spring 应用上下文启动...");
		UserFactory userFactory = applicationContext.getBean(UserFactory.class);
		System.out.println(userFactory);
		System.out.println("Spring 应用上下文准备关闭");
		applicationContext.close();
		System.out.println("Spring 应用上下文已关闭");

	}

	@Bean(initMethod = "iniUserFactory", destroyMethod = "doDestroy")
	@Lazy
	public UserFactory userFactory() {
		return new DefaultUserFactory();
	}
}
