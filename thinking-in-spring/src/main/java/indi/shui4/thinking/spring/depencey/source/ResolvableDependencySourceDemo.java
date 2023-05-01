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

	@SuppressWarnings("unused")
	@Autowired
	private String value;

	@SuppressWarnings("squid:S1602")
	public static void main(String[] args) {

		// 创建 BeanFactory 容器
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		// 注册 Configuration Class（配置类） -> Spring Bean
		applicationContext.register(ResolvableDependencySourceDemo.class);
		// 注意：无法在 AbstractApplicationContext#refresh 之后  执行 registerResolvableDependency
		// 只能通过这种事件回调的方式去应用 registerResolvableDependency
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
