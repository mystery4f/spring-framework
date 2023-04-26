package indi.shui4.thinking.spring.depencey.source;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ResourceLoader;
import javax.annotation.PostConstruct;

/**
 * 依赖来源示例
 *
 * @author shui4
 */
public class DependencySourceDemo {
	@Autowired
	private BeanFactory beanFactory;
	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private ApplicationContext applicationContext;

	public static void main(String[] args) {
		try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
			applicationContext.register(DependencySourceDemo.class);
			applicationContext.refresh();
			DependencySourceDemo demo = applicationContext.getBean(DependencySourceDemo.class);
		}


	}

	@PostConstruct
	public void init() {
		System.out.println("beanFactory == applicationContext " + (beanFactory == applicationContext));
		System.out.println("beanFactory == applicationContext.getAutowireCapableBeanFactory() " + (beanFactory == applicationContext.getAutowireCapableBeanFactory()));
		System.out.println("resourceLoader == applicationContext " + (resourceLoader == applicationContext));
		System.out.println("applicationEventPublisher == applicationContext " + (applicationEventPublisher == applicationContext));

	}


	@PostConstruct
	public void initByLookup() {
		getBean(BeanFactory.class);
		getBean(ApplicationContext.class);
		getBean(ResourceLoader.class);
		getBean(ApplicationEventPublisher.class);
	}

	private <T> T getBean(Class<T> tClass) {
		try {
			return beanFactory.getBean(tClass);
		} catch (NoSuchBeanDefinitionException e) {
			System.err.println("当前类型" + tClass.getName() + " 无法在 BeanFactory 中查找！");
		}
		return null;
	}


}
