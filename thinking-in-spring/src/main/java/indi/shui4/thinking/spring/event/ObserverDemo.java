package indi.shui4.thinking.spring.event;

import java.util.EventListener;
import java.util.EventObject;
import java.util.Observable;
import java.util.Observer;

/**
 * {@link java.util.Observer} 示例
 *
 * @author shui4
 */
public class ObserverDemo {
	public static void main(String[] args) {
		final var observable = new EventObservable();
		observable.addObserver(new EventObserver());
		observable.notifyObservers("hello");
	}

	static class EventObserver implements Observer, EventListener {

		@Override
		public void update(Observable o, Object mesage) {
			EventObject eventObject = (EventObject) mesage;
			System.out.println("收到事件：" + eventObject.getSource());
		}
	}

	private static class EventObservable extends Observable {
		@Override
		public void notifyObservers(Object arg) {
			setChanged();
			super.notifyObservers(new EventObject(arg));
		}
	}
}
