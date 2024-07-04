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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AliasRegistry;

/**
 * 保存 Bean 定义的注册表的接口，例如 RootBeanDefinition 和 ChildBeanDefinition 实例。
 * 通常由在内部使用 AbstractBeanDefinition 层次结构的 BeanFactories 实现。
 *
 * <p>这是 Spring 的 bean factory 包中唯一封装了 bean 定义<i>注册</i>的接口。
 * 标准的 BeanFactory 接口仅涵盖对<i>完全配置的工厂实例</i>的访问。
 *
 * <p>Spring 的 bean 定义读者希望能够实现此接口。
 * Spring 核心中的已知实现者是 DefaultListableBeanFactory 和 GenericApplicationContext。
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.config.BeanDefinition
 * @see AbstractBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @see DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see PropertiesBeanDefinitionReader
 * @since 26.11.2003
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

	/**
	 * Register a new bean definition with this registry.
	 * Must support RootBeanDefinition and ChildBeanDefinition.
	 *
	 * @param beanName       the name of the bean instance to register
	 * @param beanDefinition definition of the bean instance to register
	 * @throws BeanDefinitionStoreException    if the BeanDefinition is invalid
	 * @throws BeanDefinitionOverrideException if there is already a BeanDefinition
	 *                                         for the specified bean name and we are not allowed to override it
	 * @see GenericBeanDefinition
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException;

	/**
	 * Remove the BeanDefinition for the given name.
	 *
	 * @param beanName the name of the bean instance to register
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Return the BeanDefinition for the given bean name.
	 *
	 * @param beanName name of the bean to find a definition for
	 * @return the BeanDefinition for the given name (never {@code null})
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Check if this registry contains a bean definition with the given name.
	 *
	 * @param beanName the name of the bean to look for
	 * @return if this registry contains a bean definition with the given name
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * Return the names of all beans defined in this registry.
	 *
	 * @return the names of all beans defined in this registry,
	 * or an empty array if none defined
	 */
	String[] getBeanDefinitionNames();

	/**
	 * Return the number of beans defined in the registry.
	 *
	 * @return the number of beans defined in the registry
	 */
	int getBeanDefinitionCount();

	/**
	 * 确定给定名称的 Bean 定义是否可覆盖，
	 * 即 {@link #registerBeanDefinition} 是否会成功返回针对同名的现有定义。
	 * //	 * <p>默认实现返回 {@code true}。
	 *
	 * @param beanName the name to check
	 * @return whether the definition for the given bean name is overridable
	 * @since 6.1
	 */
	default boolean isBeanDefinitionOverridable(String beanName) {
		return true;
	}

	/**
	 * 确定给定的 Bean 名称是否已在此注册表中使用，即是否有在此名称下注册的本地 Bean 或别名。
	 *
	 * @param beanName the name to check
	 * @return whether the given bean name is already in use
	 */
	boolean isBeanNameInUse(String beanName);

}
