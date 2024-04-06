package indi.shui4.thinking.spring.event;

import cn.hutool.core.lang.Console;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@link org.springframework.context.ApplicationListener} 示例
 *
 * @author shui4
 */
@EnableAsync
public class ApplicationListenerDemo implements ApplicationEventPublisherAware {
	public static void main(String[] args) {
//		GenericApplicationContext applicationContext = new GenericApplicationContext();
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.addApplicationListener(event -> System.out.println("ApplicationListener 收到事件：" + event.getClass() + "source=" + event.getSource()));
		applicationContext.register(ApplicationListenerDemo.class);
		applicationContext.register(MyApplicationListener.class);
		applicationContext.refresh();
		applicationContext.start();
		applicationContext.close();
	}

	@EventListener
	public void onApplicationContext(ApplicationEvent event) {
		println("@EventListener 收到事件：" + event.getClass());
	}

	private static void println(Object printable) {
		Console.log("[threadName={}],{}", Thread.currentThread().getName(), printable);
	}

	@EventListener(ContextRefreshedEvent.class)
	@Order(2)
	public void onApplicationContext(ContextRefreshedEvent event) {
		println("@Order(2) ContextRefreshedEvent");
	}

	@EventListener(ContextRefreshedEvent.class)
	@Order(1)
	public void onApplicationContext1(ContextRefreshedEvent event) {
		println("@Order(1) ContextRefreshedEvent");
	}

	@EventListener(ContextStartedEvent.class)
	public void onApplicationContext(ContextStartedEvent event) {
		println("ContextStartedEvent");
	}

	@EventListener(ContextStartedEvent.class)
	@Async
	public void onApplicationContextAsync(ContextStartedEvent event) {
		println("ContextStartedEvent(async)");
	}

	@EventListener(ContextClosedEvent.class)
	public void onApplicationContext(ContextClosedEvent event) {
		println("ContextClosedEvent");
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		// 发送 PayloadApplicationEvent
		applicationEventPublisher.publishEvent(new ApplicationEvent("Hello") {
		});
	}

	static class MyApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			Console.log("MyApplicationListener onApplicationEvent ContextRefreshedEvent");
		}
	}
}
