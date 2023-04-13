package indi.shui4.thinking.spring.bean.factory;

import indi.shui4.thinking.spring.ioc.overview.domain.User;

/**
 * @author shui4
 */
public class DefaultUserFactory implements UserFactory {
    @Override
    public User createUser() {
        User user = new User();
        user.setId("DefaultUserFactoryImpl");
        user.setName("shui4");
        return user;
    }
}
