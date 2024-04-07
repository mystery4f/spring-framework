package indi.shui4.thinking.spring.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author shui4
 */
public class CustomizedSpringEventDemo {

	public static void main(String[] args) {
		final var applicationContext = new GenericApplicationContext();
		applicationContext.addApplicationListener(new MySpringEventListener());
		applicationContext.addApplicationListener((ApplicationListener<ApplicationEvent>) event -> {
			System.out.println("ApplicationEvent 监听器：" + event.getClass().getSimpleName());
		});
		applicationContext.refresh();
		applicationContext.publishEvent(new MySpringEvent("Hello"));
		applicationContext.publishEvent(new MySpringEvent2("Hello"));
		applicationContext.stop();
		applicationContext.close();
	}

}
