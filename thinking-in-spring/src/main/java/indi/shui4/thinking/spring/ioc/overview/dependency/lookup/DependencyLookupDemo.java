package indi.shui4.thinking.spring.ioc.overview.dependency.lookup;

import indi.shui4.thinking.spring.ioc.overview.annotation.Supper;
import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

/**
 * 依赖查找示例。
 * <p>
 * 查找方式：
 * <ol>
 *     <li>通过名称的方式来查找。</li>
 *     <li>通过类型查找。</li>
 *     <li>通过集合类型查找。</li>
 * </ol>
 * </p>
 *
 * @author shui4
 */
public class DependencyLookupDemo {
    public static void main(String[] args) {
        BeanFactory beanFactory = new ClassPathXmlApplicationContext("classpath:/META-INF/dependency-lookup-context.xml");
        lookupInRealTime(beanFactory);
        lookupInLazy(beanFactory);
        lookupByType(beanFactory);
        lookupByCollectionType(beanFactory);
        lookupByAnnotationType(beanFactory);
    }

    /**
     * 实时查找
     *
     * @param beanFactory beanFactory
     */
    private static void lookupInRealTime(BeanFactory beanFactory) {
        System.out.println("实时查找" + beanFactory.getBean("user"));
    }

    /**
     * 延迟查找
     *
     * @param beanFactory beanFactory
     */
    private static void lookupInLazy(BeanFactory beanFactory) {
        ObjectFactory<User> objectFactory = (ObjectFactory<User>) beanFactory.getBean("objectFactory");
        System.out.println("延迟查找:" + objectFactory.getObject());
    }

    /**
     * 按类型查找
     *
     * @param beanFactory beanFactory
     */
    private static void lookupByType(BeanFactory beanFactory) {
        System.out.println("按类型依赖查找:" + beanFactory.getBean(User.class));
    }

    private static void lookupByCollectionType(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ListableBeanFactory)) {
            return;
        }
        System.out.println("按集合类型依赖查找:" + ((ListableBeanFactory) beanFactory).getBeansOfType(User.class));
    }

    /**
     * 按注解类型依赖查找
     *
     * @param beanFactory beanFactory
     */
    private static void lookupByAnnotationType(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ListableBeanFactory)) {
            return;
        }
        Map<String, Object> beansWithAnnotation = ((ListableBeanFactory) beanFactory).getBeansWithAnnotation(Supper.class);
        System.out.println("按注解类型依赖查找:" + beansWithAnnotation);
    }


}
