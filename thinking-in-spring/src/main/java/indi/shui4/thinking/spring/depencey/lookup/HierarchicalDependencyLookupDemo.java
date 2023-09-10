package indi.shui4.thinking.spring.depencey.lookup;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 通过   {@link ObjectProvider}  作为IoC依赖查找
 *
 * @author shui4
 */
public class HierarchicalDependencyLookupDemo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(HierarchicalDependencyLookupDemo.class);
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		DefaultListableBeanFactory parentBeanFactory = createParentBeanFactory();
		beanFactory.setParentBeanFactory(parentBeanFactory);
		System.out.println("当前 BeanFactory 的 Parent BeanFactory" + beanFactory.getParentBeanFactory());
		applicationContext.refresh();
		displayLocalBean(beanFactory, "user");
		displayLocalBean(parentBeanFactory, "user");
		displayContainsBean(beanFactory, "user");
		displayContainsBean(parentBeanFactory, "user");

		applicationContext.close();
	}

	private static DefaultListableBeanFactory createParentBeanFactory() {
		DefaultListableBeanFactory parentBeanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(parentBeanFactory);
		reader.loadBeanDefinitions("classpath:META-INF/dependency-lookup-context.xml");
		return parentBeanFactory;
	}

	private static void displayLocalBean(HierarchicalBeanFactory beanFactory, @SuppressWarnings("SameParameterValue") String beanName) {
		System.out.printf("当前 BeanFactory[%s] 是否包含 bean[name : %s] : %s%n", beanFactory, beanName, beanFactory.containsLocalBean(beanName));

	}

	/**
	 * 双亲委派
	 *
	 * @param beanFactory beanFactory
	 * @param beanName    beanName
	 */
	private static void displayContainsBean(HierarchicalBeanFactory beanFactory, @SuppressWarnings("SameParameterValue") String beanName) {
		System.out.printf("BeanFactory[%s] 是否包含 bean[name : %s] : %s%n", beanFactory, beanName, containsBean(beanFactory, beanName));
	}

	private static boolean containsBean(HierarchicalBeanFactory beanFactory, String beanName) {
		BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
		if (parentBeanFactory instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory parentHierarchicalBeanFactory = (HierarchicalBeanFactory) parentBeanFactory;
			if (containsBean(parentHierarchicalBeanFactory, beanName)) {
				return true;
			}
		}
		return beanFactory.containsLocalBean(beanName);
	}

}
