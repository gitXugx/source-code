package container;

import java.util.Arrays;

/**
 * @author ：ex-xugaoxiang001
 * @description ：
 * @copyright ：	Copyright 2019 yowits Corporation. All rights reserved.
 * @create ：2019/1/21 10:12
 */
public class ArrayListTest {
    public  static void main(String[] args){
        Person person = new Person();
        person.setName("aaa");
        Person[] people = new Person[]{person , new Person() , new Person()};

        Person[] people1 = Arrays.copyOf(people, 3);

        Person[] person2 = new Person[10];
        System.out.println(people.length);
        System.out.println(person2.length);
        Person person1 =  people1[0];
        System.out.println(person1.i);
        person1.setName("bbbb");
        System.out.println(people[0]);
        System.out.println(people1[0]);
    }

}
