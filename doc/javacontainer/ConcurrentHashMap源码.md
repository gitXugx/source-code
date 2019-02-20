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
    //最小并发区间
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
                            //遍历链表部分是覆盖老值还是添加新值
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
                                //添加新值
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        //判断该节点是否为红黑树
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            //如果返回值不为空 则为修改，如果为空则为添加
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
                    //判断是否需要树化
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        //是否需要扩容和basecount更新
        addCount(1L, binCount);
        return null;
    }


}
```

put操作使用的是 `死循环+CAS+synchronized` 来进行添加元素，添加的时候分为4中情况：
1. 若该槽中没有数据则进行cas直接进行设置，不涉及锁操作。
2. 如果正在扩容，则尝试协助扩容，之后接着添加元素
3. 该槽中是链表，进行锁住头节点，进行cas设值
4. 该槽中TreeBin，进行锁住头节点，通过cas锁进行设值


**initTable**

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
            //小于0证明有人在初始化
            if ((sc = sizeCtl) < 0)
                //尝试让出cpu给其他线程更多的机会(自己也有可能重新获取cpu片段)
                Thread.yield(); // lost initialization race; just spin
            //根据cas设置初始化标识。-1
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    //也有可能线程进入到这里面进行一个重新判断，以防重复初始化
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    //为正数时，为tab的扩容阈值
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }
}
```

通过多层检验来保证只有一个线程初始化数组成功。同时最后设置tab扩容阈值


**treeifyBin树化**
```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {
    private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                //如果tab容量小于最小树化的容量，则尝试扩容，一般重新扩容后就会更具高低位进行重新hash
                tryPresize(n << 1);
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                //锁住头节点
                synchronized (b) {
                    //防止多线程下重复树化
                    if (tabAt(tab, index) == b) {
                        TreeNode<K,V> hd = null, tl = null;
                        //进行遍历所有节点，进行创建树节点
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p =
                                new TreeNode<K,V>(e.hash, e.key, e.val,
                                                  null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        //重新设置该槽
                        setTabAt(tab, index, new TreeBin<K,V>(hd));
                    }
                }
            }
        }
    }
}
```

树化也是通过 `cas+synchronized`来进行设置。


**addCount**

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {
    private final void addCount(long x, int check) {
        CounterCell[] as; long b, s;
        //CAS 更新 baseCount 如果成功则去判断是否需要扩容
        if ((as = counterCells) != null ||
            !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            CounterCell a; long v; int m;
            boolean uncontended = true;
            //并发情况下 更新 失败会进入fullAddCount 
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
                !(uncontended =
                  U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                //cas死循环更新baseCount 或counterCells数组
                fullAddCount(x, uncontended);
                return;
            }
            if (check <= 1)
                return;
            //是进行一个baseCount + counterCells得到一个总的count
            s = sumCount();
        }
        if (check >= 0) {
            Node<K,V>[] tab, nt; int n, sc;
            //s是添加后的容量，sizeCtl是扩容的阈值，看是否需要扩容
            while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
                   (n = tab.length) < MAXIMUM_CAPACITY) {
                int rs = resizeStamp(n);
                if (sc < 0) {
                    //表明正在有扩容的，判断是否需要协助扩容
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                        transferIndex <= 0)
                        break;
                    //通过cas设置协助扩容成功
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        //进行协助扩容
                        transfer(tab, nt);
                }
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                             (rs << RESIZE_STAMP_SHIFT) + 2))
                    //进行扩容                         
                    transfer(tab, null);
                s = sumCount();
            }
        }
    }
}
```
addCount主要 `BASECOUNT` 加1，判断是否需要扩容


```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {
    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        //默认让线程分配均匀，如果得到的槽少于16，就使用16(一个线程扩容16个槽)
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        //扩容时备用的数组，为空时证明还没扩容，则初始化
        if (nextTab == null) {            // initiating
            try {
                @SuppressWarnings("unchecked")
                //原有的基础上扩容2倍
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                //因为这里再多线程的情况下，有可能创建多个数组，导致内存溢出问题
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            //转移的结束索引
            transferIndex = n;
        }
        int nextn = nextTab.length;
        //创建转移节点，里面包含最新的数组，以便查询，里面的hash置是MOVED
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
        //是否推进
        boolean advance = true; 
        boolean finishing = false; // to ensure sweep before committing nextTab
        // i是转移的最大区间，bound是转移的最小区间
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;
            while (advance) {
                int nextIndex, nextBound;
                //--i >= bound 证明该区间内的槽还未遍历完，--i后继续遍历
                if (--i >= bound || finishing)
                    advance = false;  
                //transferIndex 小于0 说明没有要转移的区间了 
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                }
                //设置转移的角标，例如 stride =16 ， nextIndex = 32 ，把TRANSFERINDEX设置成16 ， 转移先转移  nextIndex - 1 ;设置剩余区间的最大角标
                else if (U.compareAndSwapInt
                         (this, TRANSFERINDEX, nextIndex,
                          nextBound = (nextIndex > stride ?
                                       nextIndex - stride : 0))) {
                    //设置区间最小角标                       
                    bound = nextBound;
                    //设置区间的最大角标
                    i = nextIndex - 1;
                    //分配完槽后就需要下面的迁移
                    advance = false;
                }
            }
            //如果i角标超出预期角标 ，判断是否转移完成
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {
                    nextTable = null;
                    //使用转移后的数组
                    table = nextTab;
                    //设置扩容阈值
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                //设置SIZECTL -1 标识该线程扩容帮助扩容完成
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    //不相等说明还有帮助扩容的线程
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        //结束，让其他线程进行置空 nextTable和 使用转移后的数组
                        return;
                    //如果所有线程都转移完毕，则设置   finishing = true进行扩容完毕 
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            }
            //如果原数组该槽位null，则试着设置为fwd，该节点不用转移，只需要标志正在转移即可
            else if ((f = tabAt(tab, i)) == null)
                advance = casTabAt(tab, i, null, fwd);
            //说明其他线程正在处理，或者已经处理完    
            else if ((fh = f.hash) == MOVED)
                advance = true; // already processed
            else {
                //枷锁进行转移该槽
                synchronized (f) {
                    //多线程下再次判断是否转移的该槽未被修改
                    if (tabAt(tab, i) == f) {
                        Node<K,V> ln, hn;
                        //如果hash大于0是链表结构
                        if (fh >= 0) {
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
                            //遍历
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                //设置下个元素的是在高为还是在低位
                                int b = p.hash & n;
                                // 如果节点的 hash 值和首节点的 hash 值取于结果不同
                                if (b != runBit) {
                                    //用于下面判断尾节点是高位还是低位链表
                                    runBit = b;
                                    //用于下面的循环防止多余的遍历。因为在这里已经处理过最后一个了
                                    lastRun = p;
                                }
                            }
                            //低位链表
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            //高位链表
                            else {
                                hn = lastRun;
                                ln = null;
                            }
                            //遍历槽，分为高低位两个链表
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }
                            //设置迁移的元素
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            //设置原数组该槽已迁移完
                            setTabAt(tab, i, fwd);
                            //继续下一个槽迁移
                            advance = true;
                        }
                        //红黑树迁移
                        else if (f instanceof TreeBin) {
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> lo = null, loTail = null;
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            //遍历
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K,V> p = new TreeNode<K,V>
                                    (h, e.key, e.val, null, null);
                                //分为高位和低位两个链表    
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            //看树是否退化，或者树化
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                (lc != 0) ? new TreeBin<K,V>(hi) : t;
                            //设置迁移元素    
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            //设置原槽迁移完成
                            setTabAt(tab, i, fwd);
                            //继续下一个槽的遍历
                            advance = true;
                        }
                    }
                }
            }
        }
    }
}
```

扩容可以说是很复杂的操作，但是思想也是基于 `cas+synchronized` 方式来进行达到最优并发性能。




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





