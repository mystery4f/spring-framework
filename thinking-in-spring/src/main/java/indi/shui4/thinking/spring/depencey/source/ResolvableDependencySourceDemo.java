package indi.shui4.thinking.spring.depencey.source;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.annotation.PostConstruct;

/**
 * ResolvableDependencySourceDemo
 *
 * @author shui4
 */
public class ResolvableDependencySourceDemo {

	@Autowired
	private String value;

	public static void main(String[] args) {

		// 创建 BeanFactory 容器
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		// 注册 Configuration Class（配置类） -> Spring Bean
		applicationContext.register(ResolvableDependencySourceDemo.class);

		applicationContext.addBeanFactoryPostProcessor(beanFactory -> {
			// 注册 Resolvable Dependency
			beanFactory.registerResolvableDependency(String.class, "Hello,World");
		});

		// 启动 Spring 应用上下文
		applicationContext.refresh();

		// 显示地关闭 Spring 应用上下文
		applicationContext.close();
	}

	@PostConstruct
	public void init() {
		System.out.println(value);
	}

}
