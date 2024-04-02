package indi.shui4.thinking.spring.generic;

import cn.hutool.core.util.TypeUtil;
import indi.shui4.thinking.spring.ioc.overview.domain.User;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author shui4
 */
public class UserList extends ArrayList<User> {
	public static void main(String[] args) {
		final var userListClass = UserList.class;
		System.out.println(userListClass);
		System.out.println(TypeUtil.getTypeArgument(userListClass, 0).toString());
		System.out.println(Arrays.toString(userListClass.getTypeParameters()));
	}
}
