package indi.shui4.thinking.spring.event;

import cn.hutool.core.lang.Console;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.GenericApplicationContext;

import java.util.concurrent.Executors;

import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

/**
 * 异步事件处理 示例
 *
 * @author shui4
 */
@SuppressWarnings("AlibabaThreadPoolCreation")
public class AsyncEventHandlerDemo {
	public static void main(String[] args) {
		final var applicationContext = new GenericApplicationContext();
		applicationContext.addApplicationListener(new MySpringEventListener());
		applicationContext.addApplicationListener((ApplicationListener<MySpringEvent>) event -> {
			throw new RuntimeException("故意抛出异常");
		});
		applicationContext.refresh();
		final var applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
				ApplicationEventMulticaster.class
		);
		final var taskExecutor = Executors.newSingleThreadExecutor();
		if (applicationEventMulticaster instanceof SimpleApplicationEventMulticaster) {
			SimpleApplicationEventMulticaster eventMulticaster = (SimpleApplicationEventMulticaster) applicationEventMulticaster;
			eventMulticaster.setTaskExecutor(taskExecutor);
			eventMulticaster.addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> {
				if (!taskExecutor.isShutdown()) {
					taskExecutor.shutdown();
				}
			});
			eventMulticaster.setErrorHandler(t -> {
				Console.error("异常了", t.getCause());
			});
		}
		applicationContext.publishEvent(new MySpringEvent("Hello,World"));
		applicationContext.close();
	}
}
