package indi.shui4.thinking.spring.ioc.overview.dependency.injection;

import indi.shui4.thinking.spring.ioc.overview.repository.UserRepository;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 依赖注入示例。
 * <p>
 * 案例：
 * <ol>
 *     <li>自动依赖注入。</li>
 *     <li>通过依赖注入获取的{@link BeanFactory}与当前应用的{@link ClassPathXmlApplicationContext}不相等，因为取的是 {@link AbstractApplicationContext#setParent(ApplicationContext)}。</li>
 *     <li>通过依赖查找直接获取{@link BeanFactory}内置依赖获取失败，但{@link Environment}内置依赖却是可以的。</li>
 * </ol>
 * </p>
 *
 * @author shui4
 */
public class DependencyInjectionDemo {
    public static void main(String[] args) {
        BeanFactory beanFactory = new ClassPathXmlApplicationContext("classpath:META-INF/dependency-injection-context.xml");
        UserRepository userRepository = beanFactory.getBean(UserRepository.class);
        System.out.println(userRepository.getUsers());
        //System.out.println(JSONUtil.toJsonPrettyStr(userRepository.getBeanFactory()));
        System.out.println(userRepository.getBeanFactory());
        // false
        System.out.println(userRepository.getBeanFactory() == beanFactory);

        // 这里报错 按依赖查找 找不到，但在 UserRepository.beanFactory 依赖注入的方式是没问题的
        // 依赖注入与依赖查询两者的数据源不同
        // BeanFactory 可以理解为内置依赖，在依赖注入Spring能够注入，查找需要通过ObjectFactory的方式来做
        //System.out.println(beanFactory.getBean(BeanFactory.class));

        System.out.println(userRepository.getUserObjectFactory()
                .getObject());
        System.out.println(userRepository.getApplicationContext() == beanFactory);
        System.out.println(userRepository.getApplicationContextObjectFactory()
                .getObject() == beanFactory);
        System.out.println(userRepository.getBeanFactoryObjectFactory()
                .getObject() == beanFactory);

        // 容器内建Bean，但也不绝对，比如这个内置Bean能够直接获取
        Environment environment = beanFactory.getBean(Environment.class);
        System.out.println("获取Environment类型Bean:" + environment);
    }
}
