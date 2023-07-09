package indi.shui4.thinking.spring.bean.lifecycle;

import indi.shui4.thinking.spring.ioc.overview.domain.SuperUser;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.ObjectUtils;

import static indi.shui4.thinking.spring.bean.lifecycle.MyInstantiationAwareBeanPostProcessor.SUPER_USER;


/**
 * Bean 实例化生命周期示例
 *
 * @author shui4
 */
@SuppressWarnings("squid:S125")
public class BeanInstantiationLifecycleDemo {


	public static void main(String[] args) {
//		executeBeanFactory();
		executeApplicationContext();
	}


	public static void executeBeanFactory() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		// 添加BeanPostProcessor 实现（示例）
		// - 方法1
		// beanFactory.addBeanPostProcessor(new MyInstantiationAwareBeanPostProcessor());
		// - 方法2
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

class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

	public static final String FIELD_DESCRIPTION = "description";
	public static final String SUPER_USER = "superUser";

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

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		if (ObjectUtils.nullSafeEquals("userHolder", beanName) && UserHolder.class.equals(bean.getClass())) {
			MutablePropertyValues propertyValues;
			if (pvs instanceof MutablePropertyValues) {
				propertyValues = ((MutablePropertyValues) pvs);
			} else {
				propertyValues = new MutablePropertyValues();

			}
			// 假设 <property name="number" value="1"/> 配置的话，那么在 PropertyValues 就包含一个 PropertyValues(number=1)
			// 等价于 <property name="number" value="1"/>
			propertyValues.addPropertyValue("number", "1");
			// 原始配置 <property name="description" value="test"/>

			if (propertyValues.contains(FIELD_DESCRIPTION)) {
				// 因为 value 是不可变的
//					PropertyValue description = propertyValues.getPropertyValue("description");
				propertyValues.removePropertyValue(FIELD_DESCRIPTION);
				propertyValues.addPropertyValue(FIELD_DESCRIPTION, "The User Holder V2");
			}
			return propertyValues;
		}
		return null;
	}
}