package indi.shui4.thinking.spring.depencey.injection;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 *  "construct" Autowiring 依赖构造器注入
 * @author shui4
 */
public class AutoWiringConstructDependencyConstructInjectionDemo {
    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        reader.loadBeanDefinitions("classpath:/META-INF/autowiring-dependency-construct.xml");
        System.out.println(beanFactory.getBean(UserHolder.class));
    }

//    @Bean
//    @Primary
    public UserHolder userHolder(User user) {
        return new UserHolder(user);
    }

}
