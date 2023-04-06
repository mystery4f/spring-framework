package indi.shui4.thinking.spring.ioc.overview.container;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * {@link BeanFactory} 作为Ioc容器示例
 *
 * @author shui4
 */
public class BeanFactoryAsIocContainerDemo {
    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        String location = "classpath:META-INF/dependency-lookup-context.xml";
        // 加载配置
        int beanDefinitionsCount = xmlBeanDefinitionReader.loadBeanDefinitions(location);
        System.out.println("Bean 定义加载数量:" + beanDefinitionsCount);
        // 依赖查找集合对象
        lookupByCollectionType(beanFactory);
    }

    private static void lookupByCollectionType(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ListableBeanFactory)) {
            return;
        }
        System.out.println("按集合类型依赖查找:" + ((ListableBeanFactory) beanFactory).getBeansOfType(User.class));
    }
}
