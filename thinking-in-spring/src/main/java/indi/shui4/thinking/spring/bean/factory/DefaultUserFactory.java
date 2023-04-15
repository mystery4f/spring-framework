package indi.shui4.thinking.spring.bean.factory;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author shui4
 */
public class DefaultUserFactory implements UserFactory, InitializingBean, DisposableBean {
    @Override
    public User createUser() {
        User user = new User();
        user.setId("DefaultUserFactoryImpl");
        user.setName("shui4");
        return user;
    }

    @PostConstruct
    public void init() {
        System.out.println("@PostConstruct:UserFactory 初始化这...");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("@PreDestroy:UserFactory 销毁中");
    }


    public void iniUserFactory() {
        System.out.println("自定义初始化方法:UserFactory 初始化这...");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("InitializationBean#afterPropertiesSet():UserFactory 初始化中...");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("DisposableBean#destroy():UserFactory 销毁中...");
    }

    public void doDestroy() {
        System.out.println("自定义销毁方法:UserFactory 销毁中...");
    }

    @Override
    protected void finalize() throws Throwable {
        System.out.println("当前 UserFactory 对象正在被回收");
    }
}
