package indi.shui4.thinking.spring.tool.pojo;

import org.springframework.context.annotation.Bean;

/**
 * @author shui4
 */
public abstract class BaseUser implements IUser {


	@Bean("baseUser")
	@Override
	public void print() {

	}
}
