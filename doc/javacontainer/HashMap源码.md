# HashMap源码
> HashMap也是容器的一种, 其中数据主要以 `key-value` 键值对的形式进行存储， 底层实现是哈希表。

## HashMap 
### 字段以及声明

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    private static final long serialVersionUID = 362498820763181265L;
    //默认Map容量大小
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
    //Map的最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;
    //扩充因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    //链表转换为tree的阈值
    static final int TREEIFY_THRESHOLD = 8;
    //扩容时tree转化为链表结构的阈值
    static final int UNTREEIFY_THRESHOLD = 6;

    static final int MIN_TREEIFY_CAPACITY = 64;
    transient int modCount;
    transient Node<K,V>[] table;
    transient Set<Map.Entry<K,V>> entrySet;
    transient int size;
    int threshold;
    final float loadFactor;
}
```

从上往下开始:
`Map` 接口定义 `key-value` 容器的基本操作契约和对java8 `lambda`的支持， 定义了 `Entry<K,V>` 接口
`AbstractMap` 是Map接口的实现。


































