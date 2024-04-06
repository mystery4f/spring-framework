package indi.shui4.thinking.spring.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author shui4
 */
class MySpringEvent extends ApplicationEvent {
	public MySpringEvent(Object source) {
		super(source);
	}
}
