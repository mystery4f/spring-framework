package indi.shui4.thinking.spring.bean.factory;

import indi.shui4.thinking.spring.ioc.overview.domain.User;

/**
 * @author shui4
 */

public interface UserFactory {

    default User createUser() {
        return null;
    }

}
