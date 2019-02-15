package container;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author ：ex-xugaoxiang001
 * @description ：
 * @copyright ：	Copyright 2019 yowits Corporation. All rights reserved.
 * @create ：2019/2/15 16:27
 */
public class MyUnsafe {

    static Unsafe unsafe = null;
    private static Field getUnsafe = null;
    static {
        try {
            getUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            getUnsafe.setAccessible(true);
            unsafe = (Unsafe) getUnsafe.get(null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
