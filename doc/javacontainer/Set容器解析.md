# Set容器解析
> 存储不重复数据的容器，实现有 `HashSet` 、`LinkedHashSet` 、`TreeSet`等。

[toc]

## HashSet
> 底层实现是 Hash表，使用的都是 `HashMap` 的实现，使用其 `key` 来当存储的元素

### 字段以及声明

```java
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    static final long serialVersionUID = -5024744406713321676L;
    //底层主要由HashMap实现
    private transient HashMap<E,Object> map;
    // HashMap的value都是PRESENT
    private static final Object PRESENT = new Object();

    public HashSet() {
        map = new HashMap<>();
    }
    //该构造方法是给 LinkedHashSet 使用的
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }
}    
```

1. `HashSet(int initialCapacity, float loadFactor, boolean dummy)` 该构造方法是给 `LinkedHashSet` 使用，实现插入和访问有序的Set集合
2. 其他构造方法都是给用户使用的，创建插入和访问无序的Set集合

### 方法

其他方法基本上都是调用 `HashMap` 的实现

```java
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    //添加方法，添加的value是PRESENT
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }
    //移除方法
    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }
    //清除方法
    public void clear() {
        map.clear();
    }
}
```

## LinkedHashSet
> 底层实现是 Hash表加链表的实现，使用的都是 `LinkedHashMap` 的实现，使用其 `key` 来当存储的元素

```java
public class LinkedHashSet<E>
    extends HashSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2851667679971038690L;

    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }

    public LinkedHashSet() {
        super(16, .75f, true);
    }

    public LinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }
}

```

可以看到调用的都是 `HashSet` 的 `HashSet(int initialCapacity, float loadFactor, boolean dummy)` 构造方法来实现有序的存储。


## TreeSet

```java

public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, java.io.Serializable {
    private transient NavigableMap<E,Object> m;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    TreeSet(NavigableMap<E,Object> m) {
        this.m = m;
    }

    public TreeSet() {
        this(new TreeMap<E,Object>());
    }

    public TreeSet(Comparator<? super E> comparator) {
        this(new TreeMap<>(comparator));
    }

    public TreeSet(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    public TreeSet(SortedSet<E> s) {
        this(s.comparator());
        addAll(s);
    }
}
```
通过构造方法可以看到 `TreeSet` 的实现是 `TreeMap`。 也是把 `key` 作为添加的元素,  `value` 是 `Object PRESENT = new Object();`

## 总结

1. 从实现来看Set是通过 `Hash表` 来实现的不重复元素，简单方便
2. 实现只用到了 `Map` 的 `key` 而没有使用到 `value` ，导致一部分空间的浪费
