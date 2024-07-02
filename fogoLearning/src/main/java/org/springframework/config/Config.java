package org.springframework.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.model.Cat;

@Configurable
public class Config {
	@Bean
	public Cat getCat() {
		return new Cat();
	}
}
