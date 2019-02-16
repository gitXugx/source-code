package container;

/**
 * @author ：ex-xugaoxiang001
 * @description ：
 * @copyright ：	Copyright 2019 yowits Corporation. All rights reserved.
 * @create ：2019/2/15 16:28
 */
public class UnsafeTest {

    public  static void main(String[] args){
        long[] strings = new long[209];
        Object o = new Object();
        int scale = MyUnsafe.unsafe.arrayIndexScale(long[].class);
        System.out.println("arrayIndexScale ---1. String[].class = "+ scale);

        System.out.println("scale & (scale - 1) = "+(scale & (scale - 1)));

        int i1 = Integer.numberOfLeadingZeros(scale);
        System.out.println("numberOfLeadingZeros = " + (31- i1));

        System.out.println("addressSize = "+ MyUnsafe.unsafe.addressSize());


        int i2 = 31 - i1;

        int i = MyUnsafe.unsafe.arrayBaseOffset(long[].class);

        System.out.println("arrayBaseOffset ---2. String[].class = "+ i);

        System.out.println("first element is :" + MyUnsafe.unsafe.getObject(strings, i));

        MyUnsafe.unsafe.putObject(strings, (0 << i2) +i , 100L);

        System.out.println("after set ,first element is :" + MyUnsafe.unsafe.getObject(strings, (0 << i2) +i ));

        System.out.println(strings[0]);
    }

}
