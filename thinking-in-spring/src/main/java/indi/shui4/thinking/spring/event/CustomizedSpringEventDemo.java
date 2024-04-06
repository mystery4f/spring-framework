package indi.shui4.thinking.spring.event;

import org.springframework.context.support.GenericApplicationContext;

/**
 * @author shui4
 */
public class CustomizedSpringEventDemo {

	public static void main(String[] args) {
		final var applicationContext = new GenericApplicationContext();
		applicationContext.addApplicationListener(new MySpringEventListener());
		applicationContext.refresh();
		applicationContext.publishEvent(new MySpringEvent("Hello"));
		applicationContext.stop();
		applicationContext.close();
	}

}
