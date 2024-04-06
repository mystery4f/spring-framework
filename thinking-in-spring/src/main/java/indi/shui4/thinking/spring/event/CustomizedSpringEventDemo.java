package indi.shui4.thinking.spring.event;

import cn.hutool.core.lang.Console;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.NonNull;

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

	private static class MySpringEventListener implements ApplicationListener<MySpringEvent> {

		@Override
		public void onApplicationEvent(@NonNull MySpringEvent event) {
			Console.log("threadName={},threadId={},event={}",
					Thread.currentThread().getName(),
					Thread.currentThread().getId(),
					event
			);
		}
	}

	private static class MySpringEvent extends ApplicationEvent {
		public MySpringEvent(Object source) {
			super(source);
		}
	}
}
