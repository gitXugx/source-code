package container;

import java.util.Arrays;

/**
 * @author ：ex-xugaoxiang001
 * @description ：
 * @copyright ：	Copyright 2019 yowits Corporation. All rights reserved.
 * @create ：2019/2/15 16:28
 */
public class UnsafeTest {

    public  static void main(String[] args){
        String[] strings = new String[209];
        Object o = new Object();
        int scale = MyUnsafe.unsafe.arrayIndexScale(String[].class);
        System.out.println("arrayIndexScale ---1. String[].class = "+ scale);

        System.out.println("scale & (scale - 1) = "+(scale & (scale - 1)));

        int i1 = Integer.numberOfLeadingZeros(scale);
        System.out.println("numberOfLeadingZeros = " + (31- i1));

        int i2 = 31 - i1;

        int i = MyUnsafe.unsafe.arrayBaseOffset(String[].class);

        System.out.println("arrayBaseOffset ---2. String[].class = "+ i);

        System.out.println("first element is :" + MyUnsafe.unsafe.getObject(strings, i));

        MyUnsafe.unsafe.putObject(strings, (1 << i2) +i , "100");

        System.out.println("after set ,first element is :" + MyUnsafe.unsafe.getObject(strings, (1 << i2) +i ));

        System.out.println(Arrays.asList(strings));
    }

}
