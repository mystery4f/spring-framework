package indi.shui4.thinking.spring.tool.pojo;


import org.springframework.context.annotation.Bean;

/**
 * IUser
 *
 * @author shui4
 */
public interface IUser {
	@Bean("iUser")
	void print();
}
