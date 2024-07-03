/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.context.support;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.support.ClassHintUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 通用 ApplicationContext 实现，
 * 它包含单个内部 {@link org.springframework.beans.factory.support.DefaultListableBeanFactory} 实例，
 * 并且不采用特定的 Bean 定义格式。实现 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 接口，
 * 以便允许对其应用任何 Bean 定义读取器。
 *
 * <p>典型的用法是通过 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 接口注册各种 Bean 定义，
 * 然后调用 {@link #refresh（）} 以使用应用程序上下文语义初始化这些 Bean
 * （@link处理 {org.springframework.context.ApplicationContextAware}、
 * 自动检测 {@link org.springframework.beans.factory.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors} 等）。
 *
 * <p>与为每次刷新创建新的内部 BeanFactory 实例的其他 ApplicationContext 实现相比，
 * 此上下文的内部 BeanFactory 从一开始就可用，以便能够在其上注册 Bean 定义。{@link #refresh（）} 只能调用一次。
 *
 * <p>此 ApplicationContext 实现适用于提前处理，
 * 使用 {@link #refreshForAotProcessing} 作为常规 {@link #refresh（）} 的替代方法。
 *
 * <p>Usage example:
 *
 * <pre class="code">
 * GenericApplicationContext ctx = new GenericApplicationContext();
 * XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
 * xmlReader.loadBeanDefinitions(new ClassPathResource("applicationContext.xml"));
 * PropertiesBeanDefinitionReader propReader = new PropertiesBeanDefinitionReader(ctx);
 * propReader.loadBeanDefinitions(new ClassPathResource("otherBeans.properties"));
 * ctx.refresh();
 *
 * MyBean myBean = (MyBean) ctx.getBean("myBean");
 * ...</pre>
 *
 * 对于 XML Bean 定义的典型情况，还可以使用 {@link ClassPathXmlApplicationContext}
 * 或 {@link FileSystemXmlApplicationContext}，它们更易于设置，但不太灵活，
 * 因为您可以只对 XML Bean 定义使用标准资源位置，而不是混合任意 Bean 定义格式。
 * 对于应该以可刷新方式读取特定 Bean 定义格式的自定义应用程序上下文实现，
 * 请考虑从 {@link AbstractRefreshableApplicationContext} 基类派生。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 1.1.2
 * @see #registerBeanDefinition
 * @see #refresh()
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
 */
public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {

	private final DefaultListableBeanFactory beanFactory;

	@Nullable
	private ResourceLoader resourceLoader;

	private boolean customClassLoader = false;

	private final AtomicBoolean refreshed = new AtomicBoolean();


	/**
	 * Create a new GenericApplicationContext.
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext() {
		this.beanFactory = new DefaultListableBeanFactory();
	}

	/**
	 * 使用给定的 DefaultListableBeanFactory 创建新的 GenericApplicationContext。
	 * @param beanFactory 要用于此上下文的 DefaultListableBeanFactory 实例
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	/**
	 * 使用给定的父级创建新的 GenericApplicationContext。
	 * @param parent the parent application context
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}

	/**
	 * 使用给定的 DefaultListableBeanFactory 创建新的 GenericApplicationContext。
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 * @param parent the parent application context
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory, ApplicationContext parent) {
		this(beanFactory);
		setParent(parent);
	}


	/**
	 * 设置此应用程序上下文的父级，并相应地设置内部 BeanFactory 的父级。
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		super.setParent(parent);
		this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory());
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		super.setApplicationStartup(applicationStartup);
		this.beanFactory.setApplicationStartup(applicationStartup);
	}

	/**
	 * 设置是否应允许通过注册具有相同名称的不同定义来覆盖 Bean 定义，
	 * 并自动替换前者。否则，将引发异常。默认值为“true”。
	 * @since 3.0
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.beanFactory.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
	}

	/**
	 * 设置是否允许 Bean 之间的循环引用 - 并自动尝试解析它们。
	 * <p>默认值为“true”。关闭此选项可在遇到循环引用时引发异常，从而完全禁止它们。
	 * @since 3.0
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.beanFactory.setAllowCircularReferences(allowCircularReferences);
	}

	/**
	 * 设置要用于此上下文的 ResourceLoader。
	 * 如果设置，上下文会将所有 {@code getResource} 调用委托给给定的 ResourceLoader。
	 * 如果未设置，将应用默认资源加载。
	 * <p>指定自定义 ResourceLoader 的主要原因是以特定方式解析资源路径（不带 URL 前缀）。
	 * 默认行为是将此类路径解析为类路径位置。若要将资源路径解析为文件系统位置，请在此处指定 FileSystemResourceLoader。
	 * <p>还可以传入完整的 ResourcePatternResolver，上下文将自动检测该解析程序，并用于 {@code getResources} 调用。否则，将应用默认资源模式匹配。
	 * @see #getResource
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.core.io.FileSystemResourceLoader
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see #getResources
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	//---------------------------------------------------------------------
	// ResourceLoader / ResourcePatternResolver override if necessary
	//---------------------------------------------------------------------

	/**
	 * 如果设置了此实现，则此实现将委托给此上下文的 {@code ResourceLoader}，否则将回退到默认的超类行为。
	 * <p>从 Spring Framework 5.3.22 开始，此方法也遵循已注册的
	 * {@linkplain #getProtocolResolvers() protocol resolvers} when a custom
	 * {@code ResourceLoader} has been set.
	 * @see #setResourceLoader(ResourceLoader)
	 * @see #addProtocolResolver(ProtocolResolver)
	 */
	@Override
	public Resource getResource(String location) {
		if (this.resourceLoader != null) {
			for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
				Resource resource = protocolResolver.resolve(location, this);
				if (resource != null) {
					return resource;
				}
			}
			return this.resourceLoader.getResource(location);
		}
		return super.getResource(location);
	}

	/**
	 * This implementation delegates to this context's ResourceLoader if it
	 * implements the ResourcePatternResolver interface, falling back to the
	 * default superclass behavior otherwise.
	 * @see #setResourceLoader
	 */
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		if (this.resourceLoader instanceof ResourcePatternResolver resourcePatternResolver) {
			return resourcePatternResolver.getResources(locationPattern);
		}
		return super.getResources(locationPattern);
	}

	@Override
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		super.setClassLoader(classLoader);
		this.customClassLoader = true;
	}

	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		if (this.resourceLoader != null && !this.customClassLoader) {
			return this.resourceLoader.getClassLoader();
		}
		return super.getClassLoader();
	}


	//---------------------------------------------------------------------
	// Implementations of AbstractApplicationContext's template methods
	//---------------------------------------------------------------------

	/**
	 * Do nothing: We hold a single internal BeanFactory and rely on callers
	 * to register beans through our public methods (or the BeanFactory's).
	 * @see #registerBeanDefinition
	 */
	@Override
	protected final void refreshBeanFactory() throws IllegalStateException {
		if (!this.refreshed.compareAndSet(false, true)) {
			throw new IllegalStateException(
					"GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
		}
		this.beanFactory.setSerializationId(getId());
	}

	@Override
	protected void cancelRefresh(Throwable ex) {
		this.beanFactory.setSerializationId(null);
		super.cancelRefresh(ex);
	}

	/**
	 * Not much to do: We hold a single internal BeanFactory that will never
	 * get released.
	 */
	@Override
	protected final void closeBeanFactory() {
		this.beanFactory.setSerializationId(null);
	}

	/**
	 * Return the single internal BeanFactory held by this context
	 * (as ConfigurableListableBeanFactory).
	 */
	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Return the underlying bean factory of this context,
	 * available for registering bean definitions.
	 * <p><b>NOTE:</b> You need to call {@link #refresh()} to initialize the
	 * bean factory and its contained beans with application context semantics
	 * (autodetecting BeanFactoryPostProcessors, etc).
	 * @return the internal bean factory (as DefaultListableBeanFactory)
	 */
	public final DefaultListableBeanFactory getDefaultListableBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		assertBeanFactoryActive();
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry
	//---------------------------------------------------------------------

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
	}

	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		this.beanFactory.removeBeanDefinition(beanName);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		return this.beanFactory.getBeanDefinition(beanName);
	}

	@Override
	public boolean isBeanDefinitionOverridable(String beanName) {
		return this.beanFactory.isBeanDefinitionOverridable(beanName);
	}

	@Override
	public boolean isBeanNameInUse(String beanName) {
		return this.beanFactory.isBeanNameInUse(beanName);
	}

	@Override
	public void registerAlias(String beanName, String alias) {
		this.beanFactory.registerAlias(beanName, alias);
	}

	@Override
	public void removeAlias(String alias) {
		this.beanFactory.removeAlias(alias);
	}

	@Override
	public boolean isAlias(String beanName) {
		return this.beanFactory.isAlias(beanName);
	}


	//---------------------------------------------------------------------
	// AOT processing
	//---------------------------------------------------------------------

	/**
	 * Load or refresh the persistent representation of the configuration up to
	 * a point where the underlying bean factory is ready to create bean
	 * instances.
	 * <p>This variant of {@link #refresh()} is used by Ahead of Time (AOT)
	 * processing that optimizes the application context, typically at build time.
	 * <p>In this mode, only {@link BeanDefinitionRegistryPostProcessor} and
	 * {@link MergedBeanDefinitionPostProcessor} are invoked.
	 * @param runtimeHints the runtime hints
	 * @throws BeansException if the bean factory could not be initialized
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 * @since 6.0
	 */
	public void refreshForAotProcessing(RuntimeHints runtimeHints) {
		if (logger.isDebugEnabled()) {
			logger.debug("Preparing bean factory for AOT processing");
		}
		prepareRefresh();
		obtainFreshBeanFactory();
		prepareBeanFactory(this.beanFactory);
		postProcessBeanFactory(this.beanFactory);
		invokeBeanFactoryPostProcessors(this.beanFactory);
		this.beanFactory.freezeConfiguration();
		PostProcessorRegistrationDelegate.invokeMergedBeanDefinitionPostProcessors(this.beanFactory);
		preDetermineBeanTypes(runtimeHints);
	}

	/**
	 * Pre-determine bean types in order to trigger early proxy class creation.
	 * @see org.springframework.beans.factory.BeanFactory#getType
	 * @see SmartInstantiationAwareBeanPostProcessor#determineBeanType
	 */
	private void preDetermineBeanTypes(RuntimeHints runtimeHints) {
		List<SmartInstantiationAwareBeanPostProcessor> bpps =
				PostProcessorRegistrationDelegate.loadBeanPostProcessors(
						this.beanFactory, SmartInstantiationAwareBeanPostProcessor.class);

		for (String beanName : this.beanFactory.getBeanDefinitionNames()) {
			Class<?> beanType = this.beanFactory.getType(beanName);
			if (beanType != null) {
				ClassHintUtils.registerProxyIfNecessary(beanType, runtimeHints);
				for (SmartInstantiationAwareBeanPostProcessor bpp : bpps) {
					Class<?> newBeanType = bpp.determineBeanType(beanType, beanName);
					if (newBeanType != beanType) {
						ClassHintUtils.registerProxyIfNecessary(newBeanType, runtimeHints);
						beanType = newBeanType;
					}
				}
			}
		}
	}


	//---------------------------------------------------------------------
	// Convenient methods for registering individual beans
	//---------------------------------------------------------------------

	/**
	 * Register a bean from the given bean class, optionally providing explicit
	 * constructor arguments for consideration in the autowiring process.
	 * @param beanClass the class of the bean
	 * @param constructorArgs custom argument values to be fed into Spring's
	 * constructor resolution algorithm, resolving either all arguments or just
	 * specific ones, with the rest to be resolved through regular autowiring
	 * (may be {@code null} or empty)
	 * @since 5.2 (since 5.0 on the AnnotationConfigApplicationContext subclass)
	 */
	public <T> void registerBean(Class<T> beanClass, Object... constructorArgs) {
		registerBean(null, beanClass, constructorArgs);
	}

	/**
	 * Register a bean from the given bean class, optionally providing explicit
	 * constructor arguments for consideration in the autowiring process.
	 * @param beanName the name of the bean (may be {@code null})
	 * @param beanClass the class of the bean
	 * @param constructorArgs custom argument values to be fed into Spring's
	 * constructor resolution algorithm, resolving either all arguments or just
	 * specific ones, with the rest to be resolved through regular autowiring
	 * (may be {@code null} or empty)
	 * @since 5.2 (since 5.0 on the AnnotationConfigApplicationContext subclass)
	 */
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass, Object... constructorArgs) {
		registerBean(beanName, beanClass, (Supplier<T>) null,
				bd -> {
					for (Object arg : constructorArgs) {
						bd.getConstructorArgumentValues().addGenericArgumentValue(arg);
					}
				});
	}

	/**
	 * Register a bean from the given bean class, optionally customizing its
	 * bean definition metadata (typically declared as a lambda expression).
	 * @param beanClass the class of the bean (resolving a public constructor
	 * to be autowired, possibly simply the default constructor)
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.0
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 */
	public final <T> void registerBean(Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
		registerBean(null, beanClass, null, customizers);
	}

	/**
	 * Register a bean from the given bean class, optionally customizing its
	 * bean definition metadata (typically declared as a lambda expression).
	 * @param beanName the name of the bean (may be {@code null})
	 * @param beanClass the class of the bean (resolving a public constructor
	 * to be autowired, possibly simply the default constructor)
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.0
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 */
	public final <T> void registerBean(
			@Nullable String beanName, Class<T> beanClass, BeanDefinitionCustomizer... customizers) {

		registerBean(beanName, beanClass, null, customizers);
	}

	/**
	 * Register a bean from the given bean class, using the given supplier for
	 * obtaining a new instance (typically declared as a lambda expression or
	 * method reference), optionally customizing its bean definition metadata
	 * (again typically declared as a lambda expression).
	 * @param beanClass the class of the bean
	 * @param supplier a callback for creating an instance of the bean
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.0
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 */
	public final <T> void registerBean(
			Class<T> beanClass, Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		registerBean(null, beanClass, supplier, customizers);
	}

	/**
	 * Register a bean from the given bean class, using the given supplier for
	 * obtaining a new instance (typically declared as a lambda expression or
	 * method reference), optionally customizing its bean definition metadata
	 * (again typically declared as a lambda expression).
	 * <p>This method can be overridden to adapt the registration mechanism for
	 * all {@code registerBean} methods (since they all delegate to this one).
	 * @param beanName the name of the bean (may be {@code null})
	 * @param beanClass the class of the bean
	 * @param supplier a callback for creating an instance of the bean (in case
	 * of {@code null}, resolving a public constructor to be autowired instead)
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.0
	 */
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		ClassDerivedBeanDefinition beanDefinition = new ClassDerivedBeanDefinition(beanClass);
		if (supplier != null) {
			beanDefinition.setInstanceSupplier(supplier);
		}
		for (BeanDefinitionCustomizer customizer : customizers) {
			customizer.customize(beanDefinition);
		}

		String nameToUse = (beanName != null ? beanName : beanClass.getName());
		registerBeanDefinition(nameToUse, beanDefinition);
	}


	/**
	 * {@link RootBeanDefinition} subclass for {@code #registerBean} based
	 * registrations with flexible autowiring for public constructors.
	 */
	@SuppressWarnings("serial")
	private static class ClassDerivedBeanDefinition extends RootBeanDefinition {

		public ClassDerivedBeanDefinition(Class<?> beanClass) {
			super(beanClass);
		}

		public ClassDerivedBeanDefinition(ClassDerivedBeanDefinition original) {
			super(original);
		}

		@Override
		@Nullable
		public Constructor<?>[] getPreferredConstructors() {
			Constructor<?>[] fromAttribute = super.getPreferredConstructors();
			if (fromAttribute != null) {
				return fromAttribute;
			}
			Class<?> clazz = getBeanClass();
			Constructor<?> primaryCtor = BeanUtils.findPrimaryConstructor(clazz);
			if (primaryCtor != null) {
				return new Constructor<?>[] {primaryCtor};
			}
			Constructor<?>[] publicCtors = clazz.getConstructors();
			if (publicCtors.length > 0) {
				return publicCtors;
			}
			return null;
		}

		@Override
		public RootBeanDefinition cloneBeanDefinition() {
			return new ClassDerivedBeanDefinition(this);
		}
	}

}
