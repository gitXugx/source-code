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
    //修改计数， 支持fast-fail
    transient int modCount;
    //hash表
    transient Node<K,V>[] table;
    transient Set<Map.Entry<K,V>> entrySet;
    transient int size;
    //扩容的阈值
    int threshold;
    //加载因子
    final float loadFactor;
}
```

从上往下开始:
`Map` 接口定义 `key-value` 容器的基本操作契约和对java8 `lambda`的支持， 定义了 `Entry<K,V>` 接口
`AbstractMap` 是Map接口的实现。


### 方法

**构造函数**
```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    //使用默认的加载因子
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }
    //把其他map添加到当前map中
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }
    //创建指定容量的Map    
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }
    // 指定加载因子和Map容量
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        //这里扩容阈值直接是容量， 因为在put时候会去计算扩充阈值
        this.threshold = tableSizeFor(initialCapacity);
    }
}
```

**容量计算**

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
}
```































