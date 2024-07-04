package org.springframework.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
public class B {
	@Autowired
	private A a;

	@Override
	public String toString() {
		return "B";
	}
}
