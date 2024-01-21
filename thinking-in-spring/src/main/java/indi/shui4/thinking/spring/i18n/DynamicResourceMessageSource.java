package indi.shui4.thinking.spring.i18n;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.thread.ThreadUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 动态（更新）资源 {@link MessageSource} 实现
 * <ol>
 *     <li>定位资源位置（Properties 文件）</li>
 *     <li>初始化 Properties 对象</li>
 *     <li>实现 {@link AbstractMessageSource#resolveCode(String, Locale)} 方法</li>
 *     <li>监听资源文件（Java NIO 2 WatchService）</li>
 *     <li>使用线程池处理文件变化</li>
 *     <li>重新装载 Properties 对象</li>
 * </ol>
 *
 * @author shui4
 */
@SuppressWarnings("NullableProblems")
public class DynamicResourceMessageSource extends AbstractMessageSource implements ResourceLoaderAware {

	public static final String ENCODING = "UTF-8";
	public static final String RESOURCE_FILE_NAME = "msg.properties";
	private static final String RESOURCE_PATH = "/META-INF/" + RESOURCE_FILE_NAME;
	private final Properties messageProperties;
	private final ExecutorService executorService;
	private final Resource messagePropertiesResource;
	private ResourceLoader resourceLoader;


	@SuppressWarnings("AlibabaThreadPoolCreation")
	public DynamicResourceMessageSource() {
		this.executorService = Executors.newSingleThreadExecutor();
		this.messagePropertiesResource = getMessagePropertiesResource();
		messageProperties = loadMessageProperties();
		onMessagePropertiesChanged();
	}

	public static void main(String[] args) {
		DynamicResourceMessageSource dynamicResourceMessageSource = new DynamicResourceMessageSource();
		for (int i = 0; i < 10; i++) {
			String message = dynamicResourceMessageSource.getMessage("name", new Object[]{}, Locale.getDefault());
			System.out.println(message);
			ThreadUtil.sleep(1, TimeUnit.SECONDS);
		}
	}

	/**
	 * 获取 Resource
	 *
	 * @return Resource
	 */
	private Resource getMessagePropertiesResource() {
		ResourceLoader resourceLoader1 = getResourceLoader();
		return resourceLoader1.getResource(RESOURCE_PATH);
	}

	/**
	 * 监听资源文件
	 */
	private void onMessagePropertiesChanged() {
		// ? 非文件
		if (!this.messagePropertiesResource.isFile()) {
			return;
		}
		try {
			File messagePropertiesResourceFile = messagePropertiesResource.getFile();
			Path messagePropertiesResourceFilePath = messagePropertiesResourceFile.toPath();
			// 获取文件系统
			FileSystem fileSystem = FileSystems.getDefault();
			WatchService watchService = fileSystem.newWatchService();
			// 注册 WatchService 到 messagePropertiesResourceFilePath，关心修改事件
			messagePropertiesResourceFilePath.getParent().register(watchService, WatchMonitor.ENTRY_MODIFY);
			processMessagePropertiesChanged(watchService);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


	}

	/**
	 * 处理资源文件变化（异步）
	 *
	 * @param watchService watchService
	 */
	@SuppressWarnings("InfiniteLoopStatement")
	private void processMessagePropertiesChanged(WatchService watchService) {
		executorService.submit(() -> {
			while (true) {
				WatchKey watchKey = watchService.take();
				try {
					// ? 非有效
					if (!watchKey.isValid()) {
						continue;
					}
					List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
					watchEvents.forEach(watchEvent -> {
						Watchable watchable = watchKey.watchable();
						// 目录路径（监听的注册目录）
						Path dirPath = (Path) watchable;
						// 事件所关联的对象即注册目录的子文件（或子目录）
						// 事件发生源是相对路径
						Path fileRelativePath = (Path) watchEvent.context();
						if (!fileRelativePath.getFileName().toFile().toString().equals(RESOURCE_FILE_NAME)) {
							return;
						}
						// 处理为绝对路径
						Path filePath = dirPath.resolve(fileRelativePath);
						System.out.println("修改的文件：" + filePath);
						File file = filePath.toFile();
						Properties properties;
						try {
							properties = loadMessageProperties(new FileReader(file));
						} catch (FileNotFoundException e) {
							throw new RuntimeException(e);
						}
						synchronized (messageProperties) {
							messageProperties.clear();
							messageProperties.putAll(properties);
						}

					});
				} finally {
					if (watchKey != null) {
						watchKey.reset();
					}
				}
			}
		});
	}

	public ResourceLoader getResourceLoader() {
		return resourceLoader != null ? this.resourceLoader : new DefaultResourceLoader();
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	/**
	 * 加载 Properties 文件
	 *
	 * @return Properties
	 */
	private Properties loadMessageProperties() {
		final EncodedResource encodedResource = new EncodedResource(this.messagePropertiesResource, ENCODING);
		Reader reader = getReader(encodedResource);
		return loadMessageProperties(reader);
	}

	private Reader getReader(EncodedResource encodedResource) {
		Reader reader;
		try {
			reader = encodedResource.getReader();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return reader;
	}

	/**
	 * 加载 Properties 文件
	 *
	 * @param reader1 reader1
	 * @return Properties
	 */
	private Properties loadMessageProperties(Reader reader1) {
		Properties properties = new Properties();
		try (Reader reader = reader1) {
			properties.load(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return properties;
	}


	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		String messageFormatPattern = messageProperties.getProperty(code);
		if (StringUtils.hasText(messageFormatPattern)) {
			return new MessageFormat(messageFormatPattern, locale);
		}
		return null;
	}

}
