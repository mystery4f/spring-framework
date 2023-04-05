package indi.shui4.thinking.spring.ioc.overview.dependency.injection;

import indi.shui4.thinking.spring.ioc.overview.repository.UserRepository;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 依赖注入示例。
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
public class DependencyInjectionDemo {
    public static void main(String[] args) {
        BeanFactory beanFactory = new ClassPathXmlApplicationContext("classpath:META-INF/dependency-injection-context.xml");
        UserRepository userRepository = beanFactory.getBean(UserRepository.class);
        System.out.println(userRepository.getUsers());
        //System.out.println(JSONUtil.toJsonPrettyStr(userRepository.getBeanFactory()));
        System.out.println(userRepository.getBeanFactory());
        // false
        System.out.println(userRepository.getBeanFactory() == beanFactory);
        // 依赖查找 找不到，但在 UserRepository.beanFactory 依赖注入的方式是没问题的
        // 它们的数据来源不同
        //System.out.println(beanFactory.getBean(BeanFactory.class));
        System.out.println(userRepository.getUserObjectFactory().getObject());
        System.out.println(userRepository.getApplicationContextObjectFactory().getObject() == beanFactory);
    }


}
