package indi.shui4.thinking.spring.ioc.overview.container;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * {@link ApplicationContext} 作为 Ioc 容器示例
 *
 * @author shui4
 */
public class AnnotationApplicationContextAsIocContainerDemo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(AnnotationApplicationContextAsIocContainerDemo.class);
		applicationContext.refresh();
		lookupByCollectionType(applicationContext);
		applicationContext.close();
	}

	/**
	 * 依赖查找集合对象
	 *
	 * @param beanFactory beanFactory
	 */
	private static void lookupByCollectionType(ApplicationContext
													   beanFactory) {
		System.out.println(" 按集合类型依赖查找:" + beanFactory.getBeansOfType(User.class));
	}

	@Bean
	public User user() {
		User user = new User();
		user.setId("1");
		user.setName("shui4");
		return user;
	}
}
