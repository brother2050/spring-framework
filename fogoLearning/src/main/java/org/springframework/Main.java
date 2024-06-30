package org.springframework;


import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.model.Person;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
		Person person = (Person)applicationContext.getBean("person");
		System.out.println(person.toString());
	}
}