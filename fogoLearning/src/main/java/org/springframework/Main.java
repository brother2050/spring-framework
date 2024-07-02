package org.springframework;


import org.springframework.config.Config;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.model.Cat;
import org.springframework.model.Person;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		Cat bean = context.getBean(Cat.class);
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