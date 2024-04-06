package indi.shui4.thinking.spring.event;

import cn.hutool.core.lang.Console;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 层次性 Spring 事件传播示例
 *
 * @author shui4
 */
public class HierarchicalSpringEventPropagateDemo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext parentApplicationContext = new AnnotationConfigApplicationContext();
		parentApplicationContext.setId("parent-context");
		parentApplicationContext.register(MyListener.class);
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.setId("current-context");
		applicationContext.setParent(parentApplicationContext);
		applicationContext.register(MyListener.class);

		parentApplicationContext.refresh();
		parentApplicationContext.start();

		applicationContext.refresh();    // ContextRefreshedEvent
		applicationContext.start();        // ContextStartedEvent
		applicationContext.stop();        // ContextStoppedEvent
		// ! 如果先 parentApplicationContext 关闭，后面的 applicationContext 关闭 不会再触发 parent，原因：已经被关闭
		applicationContext.close();        // ContextClosedEvent
		parentApplicationContext.close();
	}


	//	static class MyListener implements ApplicationListener<ContextRefreshedEvent> {
	static class MyListener implements ApplicationListener<ApplicationContextEvent> {
		// * 加上 static，因为 MyListener 多实例
		private static final Set<ApplicationContextEvent> processedEvents = new LinkedHashSet<>();

		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
			if (!processedEvents.add(event)) {
				return;
			}

			Console.log("监听到 ContextRefreshedEvent 事件,event={},currentContext={}",
					event.getClass().getSimpleName(),
					event.getApplicationContext().getId()
			);
		}
	}
}
