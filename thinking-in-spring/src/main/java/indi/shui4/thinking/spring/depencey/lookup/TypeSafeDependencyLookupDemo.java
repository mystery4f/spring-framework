package indi.shui4.thinking.spring.depencey.lookup;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static java.lang.System.*;

/**
 * 类型安全 依赖查找示例
 *
 * @author shui4
 */
public class TypeSafeDependencyLookupDemo {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
            applicationContext.register(TypeSafeDependencyLookupDemo.class);
            applicationContext.refresh();
            // 演示 ApplicationContext#getBean 方法安全性
            displayBeanFactoryGetBean(applicationContext);
            // 演示 ObjectProvider#getBeanProvider 方法安全性
            displayObjectFactoryGetObject(applicationContext);
            // 演示 ObjectProvider#getIfAvailable() 方法安全性
            displayObjectFactoryGetIfAvailableObject(applicationContext);
            // 演示 ListableBeanFactory#getBeansOfType 方法安全性
            displayListableBeanFactoryGetBeansOfType(applicationContext);
            // 演示 ObjectProvider Stream 操作安全性
            displayObjectProviderStreamOps(applicationContext);

        }
    }

    private static void displayBeanFactoryGetBean(ApplicationContext applicationContext) {
        printBeansException("displayBeanFactoryGetBean", () -> applicationContext.getBean(User.class));
    }

    private static void displayObjectFactoryGetObject(AnnotationConfigApplicationContext applicationContext) {
        ObjectProvider<User> objectProvider = applicationContext.getBeanProvider(User.class);
        printBeansException("displayObjectFactoryGetObject", objectProvider::getObject);
    }

    private static void displayObjectFactoryGetIfAvailableObject(AnnotationConfigApplicationContext applicationContext) {
        ObjectProvider<User> objectProvider = applicationContext.getBeanProvider(User.class);
        printBeansException("displayObjectFactoryGetIfAvailableObject", objectProvider::getIfAvailable);
    }

    private static void displayListableBeanFactoryGetBeansOfType(ListableBeanFactory beanFactory) {
        printBeansException("displayListableBeanFactoryGetBeansOfType", () -> {
            beanFactory.getBeansOfType(User.class);
        });
    }

    private static void displayObjectProviderStreamOps(AnnotationConfigApplicationContext applicationContext) {
        ObjectProvider<User> objectProvider = applicationContext.getBeanProvider(User.class);
        printBeansException("displayObjectProviderStreamOps", () -> objectProvider.forEach(out::println));

    }

    private static void printBeansException(String source, Runnable runnable) {
        err.println("===========================");
        err.println("source from : " + source);
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
