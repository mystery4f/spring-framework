package indi.shui4.thinking.spring.bean.definition;

import indi.shui4.thinking.spring.bean.factory.DefaultUserFactory;
import indi.shui4.thinking.spring.bean.factory.UserFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * 特殊的 Bean 实例化 示例
 *
 * @author shui4
 */
public class SpecialBeanInstallationDemo {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:META-INF/special-bean-creation-context.xml");
        // 通过 ApplicationContext 获取实现 AutowireCapableBeanFactory
        AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        demoServiceLoader();
        ServiceLoader<UserFactory> userFactoryServiceLoader = beanFactory.getBean("userFactoryServiceLoader", ServiceLoader.class);
        ServiceLoader<UserFactory> userFactoryServiceLoader2 = beanFactory.getBean("userFactoryServiceLoader", ServiceLoader.class);
        displayServiceLoader(userFactoryServiceLoader);
        displayServiceLoader(userFactoryServiceLoader2);
        System.out.println(userFactoryServiceLoader == userFactoryServiceLoader2);
        // 创建  UserFactory 实现，通过 AutowireCapableBeanFactory
        beanFactory.createBean(DefaultUserFactory.class);
    }

    private static void demoServiceLoader() {
        ServiceLoader<UserFactory> serviceLoader = ServiceLoader.load(UserFactory.class, Thread.currentThread()
                .getContextClassLoader());
        displayServiceLoader(serviceLoader);
    }

    private static void displayServiceLoader(ServiceLoader<UserFactory> serviceLoader) {
        Iterator<UserFactory> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            UserFactory next = iterator.next();
            System.out.println(next);
        }
    }
}
