package org.springframework;


import org.springframework.config.Config;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.model.A;
import org.springframework.model.Person;

public class Main {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		A bean = context.getBean(A.class);
		System.out.println(bean);
	}


	public static void main1(String[] args) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
		Person person = (Person)applicationContext.getBean("person");
		Person person1 = (Person)applicationContext.getBean("person");
		System.out.println(person.toString());
		System.out.println(person1.toString());
	}
}