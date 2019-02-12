# HashMap源码
> HashMap也是容器的一种, 其中数据主要以 `key-value` 键值对的形式进行存储， 底层实现是哈希表。

## HashMap 
### 字段以及声明

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    private static final long serialVersionUID = 362498820763181265L;
    //默认Map容量大小
    //Map的最大容量，因为是有符号位移，所以最大是2^30次方
    static final int MAXIMUM_CAPACITY = 1 << 30;
    //扩充因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    //链表转换为tree的阈值
    static final int TREEIFY_THRESHOLD = 8;
    //扩容时tree转化为链表结构的阈值
    static final int UNTREEIFY_THRESHOLD = 6;
    //树化必须hash表容量大于64才会进行树化
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
使用向右位移，取到合适的 `2^n` 次方，因为计算数据存放的时候 `(n - 1) & hash` ，在不为 `2^n` 的时候，最后一位进行与的时候总是 `0`，造成严重的hash碰撞

**hash计算**

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
}
```
为什么不直接用 `h & (length - 1);` 而是使用 `(h = key.hashCode()) ^ (h >>> 16)` ，当数组很小的时候，`hashCode` 只是低16位参与了运算，而高位未参与运算，导致分配的槽不均匀，如果使用 `(h = key.hashCode()) ^ (h >>> 16)` 先与自己的最高位异或，然后再与数组长度取取模，不论数组大小 `hashCode` 的高低位都会参与运算，分配要比之前均匀。


**put添加**

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
            Node<K,V>[] tab; Node<K,V> p; int n, i;
            //table没有初始化进行初始化
            if ((tab = table) == null || (n = tab.length) == 0)
                n = (tab = resize()).length;
            //如果该位置为null，确定该key在hash表中位置
            if ((p = tab[i = (n - 1) & hash]) == null)
                tab[i] = newNode(hash, key, value, null);
            else {
                Node<K,V> e; K k;
                //如果hash和key都一样则替换以前的value
                if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                    e = p;
                //看该节点时不是红黑树，如果时树结构则进行添加到树上
                else if (p instanceof TreeNode)
                    //根据hash来比较是添加到树的那个节点
                    e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
                else {
                    //去遍历链表
                    for (int binCount = 0; ; ++binCount) {
                        if ((e = p.next) == null) {
                            //添加节点
                            p.next = newNode(hash, key, value, null);
                            //达到转换为树的条件
                            if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                                treeifyBin(tab, hash);
                            break;
                        }
                        //已经有该key了
                        if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                            break;
                        //继续循环    
                        p = e;
                    }
                }
                if (e != null) { // existing mapping for key
                    V oldValue = e.value;
                    if (!onlyIfAbsent || oldValue == null)
                        e.value = value;
                    //空实现    
                    afterNodeAccess(e);
                    return oldValue;
                }
            }
            ++modCount;
            if (++size > threshold)
                resize();
            //空实现 
            afterNodeInsertion(evict);
            return null;
        }
}
```


**treeifyBin树化**

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
   final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        //树化的条件必须hash表容量>64 否则扩容
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K,V> hd = null, tl = null;
            do {
                //链表对象转的树对象
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null)
                //转换成红黑树根据hash来比较
                hd.treeify(tab);
        }
    }
}
```
当hash表容量比较小的时候本身就容易发生hash碰撞，而树最小的结构是 `8` ，所以这里判断hash表必须大于 `64` 才会进行树化

**扩容**

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        //已初始化
        if (oldCap > 0) {
            //如果容量大于最大容量，则把扩展阈值设置为最大值
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            //容量扩大为1倍，如果小于最大容量和老容量大于等于默认容量新的阈值扩大1倍
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        //再传入初始容量的时候才会进行容量等于扩容阈值  this.threshold = tableSizeFor(initialCapacity);
        else if (oldThr > 0) 
            newCap = oldThr;
        // 默认值创建的hashmap初始化
        else {              
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        //新容量大于最大容量，或者老容量小于默认容量的时候，初始化新的阈值
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    //原始数组要不可触及，被GC回收
                    oldTab[j] = null;
                    if (e.next == null)
                        //该槽中只有一个直接放到新的hash表中
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        //遍历树结构存放到新的hash表中
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        //链表结构数据的迁移到新的hash表中
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            //链表的节点，分为low位和high位，0是存储再low位，1是存储在high位。详细讲
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            //hash&容量时， low和 high位正好相差一个原始容量。
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
}
```

1. 原 `table` 已初始化的情况下扩容
2. 原 `table` 使用有参构造函数初始化的情况下
3. 原 `table` 使用无参构造函数初始化的情况下

**链表的重新Hash**

`loHead 、loTail 、hiHead 、hiTail` 这4个变量代表low位链表和high位链表，在 `tab[(n - 1) & hash]` 时候是 `n - 1` 为了防止hash碰撞严重，前面**容量计算**有讲，如果hash值直接&n会得到正好相差一个容量的值。
例如: key1(0100)，key2(10100) 容量为16(10000)  `1111 & 0100 = 4` , `10100 & 1111 = 4` ，当他们直接& 容量时会正好相差一个容量, 当容量扩大2倍时, `10100 & 11111 = 20` 正好在20位置。




















