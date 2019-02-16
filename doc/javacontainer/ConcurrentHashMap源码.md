# ConcurrentHashMap源码


## ConcurrentHashMap

**字段以及声明**
```java

public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {
    private static final long serialVersionUID = 7249069246763182397L;

    //最大容量
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    //默认初始化容量
    private static final int DEFAULT_CAPACITY = 16;
    //转换成数组的最大容量
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    //并发级别
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    //加载因子，用来计算扩容阈值
    private static final float LOAD_FACTOR = 0.75f;
    //树化
    static final int TREEIFY_THRESHOLD = 8;
    //树退化
    static final int UNTREEIFY_THRESHOLD = 6;
    //达到树化的最小容量需要64
    static final int MIN_TREEIFY_CAPACITY = 64;
    private static final int MIN_TRANSFER_STRIDE = 16;
    private static int RESIZE_STAMP_BITS = 16;
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;
    //在扩容的时候，扩容好了的槽老数组的hash设置为 MOVED
    static final int MOVED     = -1; // hash for forwarding nodes
    //当节点树化，root节点的hash为 TREEBIN
    static final int TREEBIN   = -2; // hash for roots of trees
    
    static final int RESERVED  = -3; // hash for transient reservations
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    static final int NCPU = Runtime.getRuntime().availableProcessors();

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("segments", Segment[].class),
        new ObjectStreamField("segmentMask", Integer.TYPE),
        new ObjectStreamField("segmentShift", Integer.TYPE)
    };
    //map转数组报的错误
    private static final String oomeMsg = "Required array size too large";
}
```

**构造方法**

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {
    //无参构造方法，使用的时候进行初始化
    public ConcurrentHashMap() {
    }
    //设hi在容量大小，在使用的时候延迟初始化数组
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                   MAXIMUM_CAPACITY :
                   tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }
    //初始化容量大小，然后添加数组
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }
    
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }
    //初始化容量大小，concurrencyLevel还是由系统来进行设置
    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
            MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }
}
```

构造方法都是基本初始化

**put**
```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        //concurrentHashMap key和value都不能为空，
        if (key == null || value == null) throw new NullPointerException();
        //高低位异或使其分布均匀
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                //初始化数组
                tab = initTable();
            // 根据hash取出该位置的值
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                //如果等于null 则直接设置值，若设置成功则跳出循环，否则接着尝试重新读取设值
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            //当前map在扩容，进行一个协助扩容
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                //拿到当前数组的角标的第一个元素的锁，进行一个添加
                synchronized (f) {
                    //看在拿到锁的过程中是否还是该对象
                    if (tabAt(tab, i) == f) {
                        //
                        if (fh >= 0) {
                            //标识是否要树化
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }
}
```

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final long ABASE;
    private static final int ASHIFT;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentHashMap.class;
            SIZECTL = U.objectFieldOffset
                (k.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset
                (k.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset
                (k.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset
                (k.getDeclaredField("cellsBusy"));
            Class<?> ck = CounterCell.class;
            CELLVALUE = U.objectFieldOffset
                (ck.getDeclaredField("value"));
            Class<?> ak = Node[].class;
            //获取该数组的起始偏移量
            ABASE = U.arrayBaseOffset(ak);
            //获取该数组元素的间隔偏移量
            int scale = U.arrayIndexScale(ak);
            //数据类型的大小 Node为引用型所以scale = 4 
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            //numberOfLeadingZeros 返回最高顺序之前的零比特数
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    //获取volatile对象
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        /**
         * ABASE数组的基本偏移量起始位置
         * ASHIFT每个元素的偏移量
         *  ((long)i << ASHIFT) + ABASE  制定i位置的元素 getObjectVolatile获取该值
         * 这里为什么不用table[i]来进行获取，因为即使table被volatile修饰，当内存中的table被修改时才会刷新到其他线程副本中，可能还是不是最新的。
         * 这里使用getObjectVolatile 直接获取内存中制定位置的值来保证每次获取的都是最新的。
         */
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }
    //通过cas 进行一个设置值
   static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }

}
```





