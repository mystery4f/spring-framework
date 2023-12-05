/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.NullSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * 用于解析XML bean定义的Bean定义读取器。
 * 将实际的XML文档读取委托给{@link BeanDefinitionDocumentReader}接口的实现。
 *
 * <p>通常应用于{@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
 * 或{@link org.springframework.context.support.GenericApplicationContext}。
 *
 * <p>该类加载DOM文档并将BeanDefinitionDocumentReader应用于它。
 * 文档读取器将使用给定的bean工厂注册每个bean定义，与后者的{@link org.springframework.beans.factory.support.BeanDefinitionRegistry}接口的实现进行交互。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 26.11.2003
 * @see #setDocumentReaderClass
 * @see BeanDefinitionDocumentReader
 * @see DefaultBeanDefinitionDocumentReader
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * 表示应禁用验证。
	 */
	public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

	/**
	 * 表示应自动检测验证模式。
	 */
	public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

	/**
	 * 表示应使用DTD验证。
	 */
	public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

	/**
	 * 表示应使用XSD验证。
	 */
	public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;

	/** 该类的常量实例。 */
	private static final Constants constants = new Constants(XmlBeanDefinitionReader.class);

	/**
	 * 验证模式，默认为自动验证
	 */
	private int validationMode = VALIDATION_AUTO;

	/**
	 * 是否启用命名空间感知，默认为false
	 */
	private boolean namespaceAware = false;

	/**
	 * BeanDefinitionDocumentReader的Class对象，默认为DefaultBeanDefinitionDocumentReader
	 */
	private Class<? extends BeanDefinitionDocumentReader> documentReaderClass =
			DefaultBeanDefinitionDocumentReader.class;

	/**
	 * 问题报告器，默认为FailFastProblemReporter
	 */
	private ProblemReporter problemReporter = new FailFastProblemReporter();

	/**
	 * 读取器事件监听器，默认为EmptyReaderEventListener
	 */
	private ReaderEventListener eventListener = new EmptyReaderEventListener();

	/**
	 * 源提取器，默认为NullSourceExtractor
	 */
	private SourceExtractor sourceExtractor = new NullSourceExtractor();

	/**
	 * 命名空间处理器解析器，可为空
	 */
	@Nullable
	private NamespaceHandlerResolver namespaceHandlerResolver;

	/**
	 * 文档加载器，默认为DefaultDocumentLoader
	 */
	private DocumentLoader documentLoader = new DefaultDocumentLoader();

	/**
	 * 实体解析器，可为空
	 */
	@Nullable
	private EntityResolver entityResolver;

	/**
	 * 错误处理器，默认为SimpleSaxErrorHandler
	 */
	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	/**
	 * XML验证模式探测器
	 */
	private final XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();

	/**
	 * 当前正在加载的资源集合的线程本地变量
	 */
	private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
			new NamedThreadLocal<Set<EncodedResource>>("XML bean definition resources currently being loaded"){
				@Override
				protected Set<EncodedResource> initialValue() {
					return new HashSet<>(4);
				}
			};


	/**
	 * 为给定的bean工厂创建新的XmlBeanDefinitionReader
	 * @param registry 要加载bean定义的BeanFactory，以BeanDefinitionRegistry的形式
	 */
	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}

	/**
	 * 设置是否使用XML验证。默认为true。
	 * <p>如果关闭验证，则此方法会打开命名空间感知，以便在这种情况下仍然正确处理模式命名空间。
	 * @see #setValidationMode
	 * @see #setNamespaceAware
	 */
	public void setValidating(boolean validating) {
		this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
		this.namespaceAware = !validating;
	}

	/**
	 * 通过名称设置要使用的验证模式。默认为{@link #VALIDATION_AUTO}。
	 * @see #setValidationMode
	 */
	public void setValidationModeName(String validationModeName) {
		setValidationMode(constants.asNumber(validationModeName).intValue());
	}

	/**
	 * 设置要使用的验证模式。默认为{@link #VALIDATION_AUTO}。
	 * <p>请注意，这只是激活或停用验证本身。
	 * 如果要关闭模式文件的验证，可能需要显式激活模式命名空间支持：参见{@link #setNamespaceAware}。
	 */
	public void setValidationMode(int validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * 返回要使用的验证模式。
	 */
	public int getValidationMode() {
		return this.validationMode;
	}

	/**
	 * 设置XML解析器是否应该支持XML命名空间。默认为"false"。
	 * <p>当模式验证处于活动状态时，通常不需要此选项。
	 * 但是，如果没有验证，为了正确处理模式命名空间，必须将其切换为"true"。
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * 返回XML解析器是否应该支持XML命名空间。
	 */
	public boolean isNamespaceAware() {
		return this.namespaceAware;
	}

	/**
	 * 指定要使用的{@link org.springframework.beans.factory.parsing.ProblemReporter}。
	 * <p>默认实现为{@link org.springframework.beans.factory.parsing.FailFastProblemReporter}，表现为快速失败行为。
	 * 外部工具可以提供替代实现，用于收集错误和警告以在工具UI中显示。
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * 指定要使用的{@link ReaderEventListener}。
	 * <p>默认实现是EmptyReaderEventListener，它会丢弃每个事件通知。
	 * 外部工具可以提供替代实现来监视在BeanFactory中注册的组件。
	 */
	public void setEventListener(@Nullable ReaderEventListener eventListener) {
		this.eventListener = (eventListener != null ? eventListener : new EmptyReaderEventListener());
	}

	/**
	 * 指定要使用的{@link SourceExtractor}。
	 * <p>默认实现是{@link NullSourceExtractor}，它简单地返回{@code null}作为源对象。
	 * 这意味着在正常运行时执行期间，不会附加额外的源元数据到bean配置元数据中。
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new NullSourceExtractor());
	}

	/**
	 * 指定要使用的{@link NamespaceHandlerResolver}。
	 * <p>如果未指定，则将通过{@link #createDefaultNamespaceHandlerResolver()}创建一个默认实例。
	 */
	public void setNamespaceHandlerResolver(@Nullable NamespaceHandlerResolver namespaceHandlerResolver) {
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}

	/**
	 * 指定要使用的{@link DocumentLoader}。
	 * <p>默认实现是{@link DefaultDocumentLoader}，它使用JAXP加载{@link Document}实例。
	 */
	public void setDocumentLoader(@Nullable DocumentLoader documentLoader) {
		this.documentLoader = (documentLoader != null ? documentLoader : new DefaultDocumentLoader());
	}

	/**
	 * 设置要用于解析的SAX实体解析器。
	 * <p>默认情况下，将使用{@link ResourceEntityResolver}。可以覆盖为自定义实体解析，例如相对于某个特定基本路径。
	 */
	public void setEntityResolver(@Nullable EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	/**
	 * 返回要使用的EntityResolver，如果未指定，则构建默认解析器。
	 */
	protected EntityResolver getEntityResolver() {
		if (this.entityResolver == null) {
			// 确定要使用的默认EntityResolver。
			ResourceLoader resourceLoader = getResourceLoader();
			if (resourceLoader != null) {
				this.entityResolver = new ResourceEntityResolver(resourceLoader);
			}
			else {
				this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
			}
		}
		return this.entityResolver;
	}

	/**
	 * 设置{@code org.xml.sax.ErrorHandler}接口的实现，用于自定义处理XML解析错误和警告。
	 * <p>如果未设置，将使用默认的SimpleSaxErrorHandler，它只是使用视图类的记录器实例记录警告，并重新抛出错误以终止XML转换。
	 * @see SimpleSaxErrorHandler
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 指定要使用的{@link BeanDefinitionDocumentReader}实现，负责实际读取XML bean定义文档。
	 * <p>默认为{@link DefaultBeanDefinitionDocumentReader}。
	 * @param documentReaderClass 所需的BeanDefinitionDocumentReader实现类
	 */
	public void setDocumentReaderClass(Class<? extends BeanDefinitionDocumentReader> documentReaderClass) {
		this.documentReaderClass = documentReaderClass;
	}

	/**
	 * 从指定的XML文件加载bean定义。
	 * @param resource XML文件的资源描述符
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 加载或解析错误时
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * 从指定的XML文件中加载bean定义。
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Loading XML bean definitions from " + encodedResource);
		}

		Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();

		// 在一个线程中，资源不能循环加载
		if (!currentResources.add(encodedResource)) {
			throw new BeanDefinitionStoreException(
					"Detected cyclic loading of " + encodedResource + " - check your import definitions!");
		}

		try (InputStream inputStream = encodedResource.getResource().getInputStream()) {
			InputSource inputSource = new InputSource(inputStream);
			if (encodedResource.getEncoding() != null) {
				inputSource.setEncoding(encodedResource.getEncoding());
			}
			return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from " + encodedResource.getResource(), ex);
		}
		finally {
			currentResources.remove(encodedResource);
			if (currentResources.isEmpty()) {
				this.resourcesCurrentlyBeingLoaded.remove();
			}
		}
	}

	/**
	 * 从指定的XML文件中加载bean定义。
	 */
	public int loadBeanDefinitions(InputSource inputSource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
	}

	/**
	 * 从指定的XML文件中加载bean定义。
	 */
	public int loadBeanDefinitions(InputSource inputSource, @Nullable String resourceDescription)
			throws BeanDefinitionStoreException {

		return doLoadBeanDefinitions(inputSource, new DescriptiveResource(resourceDescription));
	}


	/**
	 * 实际从指定的XML文件中加载bean定义。
	 */
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {

		try {
			Document doc = doLoadDocument(inputSource, resource);
			int count = registerBeanDefinitions(doc, resource);
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + count + " bean definitions from " + resource);
			}
			return count;
		}
		catch (BeanDefinitionStoreException ex) {
			throw ex;
		}
		catch (SAXParseException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
		}
		catch (SAXException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"XML document from " + resource + " is invalid", ex);
		}
		catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Parser configuration exception parsing XML from " + resource, ex);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"IOException parsing XML document from " + resource, ex);
		}
		catch (Throwable ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Unexpected exception parsing XML document from " + resource, ex);
		}
	}

	/**
	 * 实际使用配置的DocumentLoader加载指定的文档。
	 */
	protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
		return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
				getValidationModeForResource(resource), isNamespaceAware());
	}

	/**
	 * 确定指定{@link Resource}的验证模式。
	 */
	protected int getValidationModeForResource(Resource resource) {
		int validationModeToUse = getValidationMode();
		if (validationModeToUse != VALIDATION_AUTO) {
			return validationModeToUse;
		}
		int detectedMode = detectValidationMode(resource);
		if (detectedMode != VALIDATION_AUTO) {
			return detectedMode;
		}
		// Hmm, we didn't get a clear indication... Let's assume XSD,
		// since apparently no DTD declaration has been found up until
		// detection stopped (before finding the document's root tag).
		return VALIDATION_XSD;
	}

	/**
	 * 检测要对由提供的{@link Resource}标识的XML文件执行哪种类型的验证。如果文件有{@code DOCTYPE}定义，则使用DTD验证，否则假定使用XSD验证。
	 * <p>如果您想自定义{@link #VALIDATION_AUTO}模式的解析方式，请覆盖此方法。
	 */
	protected int detectValidationMode(Resource resource) {
		if (resource.isOpen()) {
			throw new BeanDefinitionStoreException(
					"传入的Resource [" + resource + "] 包含一个打开的流：" +
							"无法自动确定验证模式。要么传入一个能够创建新流的Resource，要么在XmlBeanDefinitionReader实例上明确指定validationMode。");
		}

		InputStream inputStream;
		try {
			inputStream = resource.getInputStream();
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"无法确定 [" + resource + "] 的验证模式：无法打开InputStream。" +
							"您是否尝试直接从SAX InputSource加载而没有在XmlBeanDefinitionReader实例上指定validationMode？", ex);
		}

		try {
			return this.validationModeDetector.detectValidationMode(inputStream);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("无法确定 [" +
					resource + "] 的验证模式：在读取InputStream时发生错误。", ex);
		}
	}

	/**
	 * 注册给定DOM文档中包含的bean定义。由{@code loadBeanDefinitions}调用。
	 * <p>创建解析器类的新实例，并在其上调用{@code registerBeanDefinitions}。
	 * @param doc DOM文档
	 * @param resource 资源描述符（用于上下文信息）
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 解析错误时抛出
	 * @see #loadBeanDefinitions
	 * @see #setDocumentReaderClass
	 * @see BeanDefinitionDocumentReader#registerBeanDefinitions
	 */
	public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		int countBefore = getRegistry().getBeanDefinitionCount();
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}

	/**
	 * 创建用于从XML文档中实际读取bean定义的{@link BeanDefinitionDocumentReader}。
	 * <p>默认实现实例化指定的“documentReaderClass”。
	 * @see #setDocumentReaderClass
	 */
	protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
		return BeanUtils.instantiateClass(this.documentReaderClass);
	}

	/**
	 * 创建{@link XmlReaderContext}以传递给文档阅读器。
	 */
	public XmlReaderContext createReaderContext(Resource resource) {
		return new XmlReaderContext(resource, this.problemReporter, this.eventListener,
				this.sourceExtractor, this, getNamespaceHandlerResolver());
	}

	/**
	 * 如果之前未设置，则延迟创建默认的NamespaceHandlerResolver。
	 * @see #createDefaultNamespaceHandlerResolver()
	 */
	public NamespaceHandlerResolver getNamespaceHandlerResolver() {
		if (this.namespaceHandlerResolver == null) {
			this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
		}
		return this.namespaceHandlerResolver;
	}

	/**
	 * 创建{@link NamespaceHandlerResolver}的默认实现，如果未指定则使用。
	 * <p>默认实现返回{@link DefaultNamespaceHandlerResolver}的实例。
	 * @see DefaultNamespaceHandlerResolver#DefaultNamespaceHandlerResolver(ClassLoader)
	 */
	protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
		ClassLoader cl = (getResourceLoader() != null ? getResourceLoader().getClassLoader() : getBeanClassLoader());
		return new DefaultNamespaceHandlerResolver(cl);
	}

}
