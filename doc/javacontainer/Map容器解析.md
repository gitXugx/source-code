# Map容器解析
> 以 `key-value` 的形式存储数据的容器

- [ ] TreeMap

[toc]

## HashMap 
> HashMap是容器的一种, 其中数据主要以 `key-value` 键值对的形式进行存储， 底层实现是哈希表。
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
        //HashMap支持null键和null值。null键都存在0角标
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

**get方法的实现**

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
                //如果第一个相等就直接返回node
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            if ((e = first.next) != null) {
                if (first instanceof TreeNode)
                    //如果是树结构，则查找树，返回node
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                //链表遍历查找node
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
}
```

## Hashtable
它的功能和 `HashMap` 基本一致也是 `key-value` 键值对，但是 `HashMap` 是线程不安全的，在多线程环境下会出现数据不一致的情况(JDK 8 以修复扩容死循环问题)，`Hashtable`  基本上每个方法都加了 `synchronized` 来保证多线程情况下的安全，`Hashtable` 的 `value` 是不能存在 `null` 同时也不支持 `null` 键，很多实现也是比较原始的，像hash槽的定位是直接使用 `e.hash & tab.length` 来确定位置，也没有使用树化来解决拉链法的缺点。


### 字段以及声明

```java

public class Hashtable<K,V> extends Dictionary<K,V> implements Map<K,V>, Cloneable, java.io.Serializable {
    //hash表，可以看到这里只是链表结构
    private transient Entry<?,?>[] table;
    private transient int count;
    //扩容阈值
    private int threshold;
    //加载因子
    private float loadFactor;
    private transient int modCount = 0;
    private static final long serialVersionUID = 1421746759512286392L;
}
```
可以从字段中看出 `Hashtable` 没有树化的设计，比HashMap的设计简单很多

### 方法

**添加 put 方法**

```java
public class Hashtable<K,V> extends Dictionary<K,V> implements Map<K,V>, Cloneable, java.io.Serializable {

   public synchronized V put(K key, V value) {
        // value不能为空，因为作为hashtable常用来存放properties value一般不为空
        // 还有可能是没有把null留有对应的index，在hashmap中是有固定的index存放null值
        if (value == null) {
            throw new NullPointerException();
        }
        // Makes sure the key is not already in the hashtable.
        Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        //这里直接取hash取模hash表长度而不留出第一位作为存储null值
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        Entry<K,V> entry = (Entry<K,V>)tab[index];
        //如果有值，进行替换
        for(; entry != null ; entry = entry.next) {
            if ((entry.hash == hash) && entry.key.equals(key)) {
                V old = entry.value;
                entry.value = value;
                return old;
            }
        }
        //添加新值
        addEntry(hash, key, value, index);
        return null;
    }

    private void addEntry(int hash, K key, V value, int index) {
        modCount++;
        Entry<?,?> tab[] = table;
        //达到阈值进行扩容
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
            tab = table;
            hash = key.hashCode();
            index = (hash & 0x7FFFFFFF) % tab.length;
        }

        // Creates the new entry.
        @SuppressWarnings("unchecked")
        Entry<K,V> e = (Entry<K,V>) tab[index];
        tab[index] = new Entry<>(hash, key, value, e);
        count++;
    }
}
```
这边只是和 `HashMap` 有一些细节不同，其他大致相同。`null` 值不能添加，直接取模数组长度等

**rehash 方法**

```java
public class Hashtable<K,V> extends Dictionary<K,V> implements Map<K,V>, Cloneable, java.io.Serializable {
    protected void rehash() {
        int oldCapacity = table.length;
        Entry<?,?>[] oldMap = table;
        //计算出新的容量
        int newCapacity = (oldCapacity << 1) + 1;
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            if (oldCapacity == MAX_ARRAY_SIZE)
                // Keep running with MAX_ARRAY_SIZE buckets
                return;
            newCapacity = MAX_ARRAY_SIZE;
        }
        //创建新的hash表
        Entry<?,?>[] newMap = new Entry<?,?>[newCapacity];

        modCount++;
        threshold = (int)Math.min(newCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
        table = newMap;
        //循环老的hash表，重新hash到新的hash表中
        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry<K,V> old = (Entry<K,V>)oldMap[i] ; old != null ; ) {
                Entry<K,V> e = old;
                old = old.next;
                //重新hash到新的hash表中
                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = (Entry<K,V>)newMap[index];
                newMap[index] = e;
            }
        }
    }
}
```

重hash基本上就是判断是否到阈值，然后进行扩容，遍历老数组进行一个重新hash


## LinkedHashMap
上面的 `HashMap` 和 `Hashtable` 他们都是无序的，存储的时候是通过hash来进行确定数组中槽的位置，而 `LinkedHashMap` 可以是说是空间换需求的方式，来通过链表和hash表的结合来达到有序的存储和遍历。其底层实现是hash表和双向链表，使用模板方法设计模式。

### 字段以及声明
```java
public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V> {

   private static final long serialVersionUID = 3801124242820219131L;
    //链表头
    transient LinkedHashMap.Entry<K,V> head;
    //链表尾
    transient LinkedHashMap.Entry<K,V> tail;
    //排序模式accessOrder，属性时布尔，对于访问操作其值为true,也就是当get（Object key）时，将最新访问的元素放到双向链表的第一位。.插入操作为false，读取和插入的顺序一致。
    final boolean accessOrder;
}
```

### 方法

**构造方法**

```java
public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V> {
    //这些构造方法主要都是调用HashMap的构造方法，不同的是 accessOrder = false
    public LinkedHashMap() {
        super();
        accessOrder = false;
    }
    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }

    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }

    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }
}
```

构造函数基本上调用父类的初始化，加上 `accessOrder = false`

**节点 Entry的实现**

```java
//对之前HashMap中的Node节点进行一个扩展
static class Entry<K,V> extends HashMap.Node<K,V> {
    //链表的前指针和后指针
    Entry<K,V> before, after;
    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
```

对 `HashMap` 中node节点进行扩展，使其支持插入和遍历的顺序，node中的链表是给hash碰撞使用的。这里的是给顺序使用的

**重写HashMap的部分方法**

```java
public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V> {
    //删除节点
    void afterNodeRemoval(Node<K,V> e) { // unlink
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        //把当前节点的前节点和后节点置为null断开链
        p.before = p.after = null;
        //让前节点的后节点指向后节点，让后节点的前节点指向前节点。 b.after = a , a.before = b
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }
    //插入之后的操作
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        //因为removeEldestEntry它默认返回的是false 所以下面移除操作不会执行，我们可以继承LinkedHashMap来实现缓存，来移除最前端的数据
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }
    //访问后操作
    void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMap.Entry<K,V> last;
        //如果是插入访问则不会进入到该方法，如果是访问顺序会把访问的数据放到链表头部
        if (accessOrder && (last = tail) != e) {
            LinkedHashMap.Entry<K,V> p =
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            //放到尾部 a.before = b b.after = a last.after = p tail = p
            p.after = null;
            if (b == null)
                head = a;
            else
                b.after = a;
            if (a != null)
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }
    //添加节点
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
            new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }
}    
```

通过重写父类 `HashMap` 的方法来实现有序Hash

**get方法**

```java
public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V> {

    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            //如果是访问顺序需要调整双向链表的顺序
            afterNodeAccess(e);
        return e.value;
    }
}
```
**遍历**

```java
public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V> {

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null)
            throw new NullPointerException();
        int mc = modCount;
        //直接从链表头遍历，保证顺序性
        for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
            action.accept(e.key, e.value);
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }
}
```



## TreeMap 
> TreeMap是容器的一种, 其中数据主要以 `key-value` 键值对的形式进行存储， 底层实现是红黑树。

### 字段以及声明

### 方法

```java
public class TreeMap<K,V> extends AbstractMap<K,V>  implements NavigableMap<K,V>, Cloneable, java.io.Serializable {
    //这个是插入后，红黑树的平衡case
    private void fixAfterInsertion(Entry<K,V> x) {
        x.color = RED;
        //父节点是红色，才会进行平衡
        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<K,V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Entry<K,V> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

}
```