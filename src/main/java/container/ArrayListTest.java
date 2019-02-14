package container;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();

        objectObjectHashMap.put(null , 111);
        LinkedHashMap<Object, Object> objectObjectLinkedHashMap = new LinkedHashMap<Object, Object>(16 ,0.75f , true);
        objectObjectLinkedHashMap.put(2, 5);
        objectObjectLinkedHashMap.put(6, 2);
        objectObjectLinkedHashMap.put(9, 2);
        objectObjectLinkedHashMap.put(3, 2);
        objectObjectLinkedHashMap.forEach((x ,y) -> System.out.println(x +":" +y));

        System.out.println("-----------");
        objectObjectLinkedHashMap.get(9);
        objectObjectLinkedHashMap.forEach((x ,y) -> System.out.println(x +":" +y));

    }

}
