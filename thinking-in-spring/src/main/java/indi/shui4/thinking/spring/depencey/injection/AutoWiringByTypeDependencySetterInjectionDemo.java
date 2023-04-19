package indi.shui4.thinking.spring.depencey.injection;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * "byType" Autowiring 依赖 Setter 方法注入示例
 *
 * @author shui4
 */
public class AutoWiringByTypeDependencySetterInjectionDemo {
    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        reader.loadBeanDefinitions("classpath:/META-INF/autowiring-dependency-setter.xml");
        System.out.println(beanFactory.getBean(UserHolder.class));
    }

//    @Bean
//    @Primary
    public UserHolder userHolder(User user) {
        return new UserHolder(user);
    }

}
