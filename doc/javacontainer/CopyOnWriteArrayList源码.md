# CopyOnWriteArrayList源码
> `ArrayList` 、`LinkedArrayList` 在多线程下会出现数据问题，`CopyOnWriteArrayList` 通过重入锁和快照的方式来实现线程安全。

<!-- TOC -->

- [CopyOnWriteArrayList源码](#copyonwritearraylist%E6%BA%90%E7%A0%81)
  - [CopyOnWriteArrayList](#copyonwritearraylist)
    - [字段以及声明](#%E5%AD%97%E6%AE%B5%E4%BB%A5%E5%8F%8A%E5%A3%B0%E6%98%8E)
  - [方法](#%E6%96%B9%E6%B3%95)

<!-- /TOC -->


## CopyOnWriteArrayList


### 字段以及声明

```java

public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 8673264195747942595L;

    /** The lock protecting all mutators */
    final transient ReentrantLock lock = new ReentrantLock();

    /** The array, accessed only via getArray/setArray. */
    private transient volatile Object[] array;

}

```

上面可以看出 `CopyOnWriteArrayList` 和 `ArrayList` 一样支持随机访问，实现List接口，字段中 `lock` 就是当对该数组进行写(插入，删除，更新)操作的时候进行加锁，更是比 `ArrayList` 字段少了很多

## 方法

**add**

```java
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    //添加元素
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        //加锁防止在插入的时候，其他线程也在插入导致数据覆盖问题
        lock.lock();
        try {
            //获取数组
            Object[] elements = getArray();
            int len = elements.length;
            //创建新的数组
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            //用新的数组代替老的数组
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: "+index+
                                                    ", Size: "+len);
            Object[] newElements;
            int numMoved = len - index;
            //如果是在尾部添加则直接copy
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + 1);
            else {
                //如果在中间添加需要copy两次
                newElements = new Object[len + 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index, newElements, index + 1,
                                 numMoved);
            }
            newElements[index] = element;
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }
}
```

这里不需要扩容，每次的添加都需要创建新的数组，然后老数组给多线程下读取，以达到线程安全的目的。相对的空间消耗也变成了2倍


**remove**


```java
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

    public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            E oldValue = get(elements, index);
            int numMoved = len - index - 1;
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, len - 1));
            else {
                Object[] newElements = new Object[len - 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index + 1, newElements, index,
                                 numMoved);
                setArray(newElements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }
}
```

基本和add逻辑一样，先创建副本，然后删除副本元素，进行copy然后设置成新的数组


**get**

```java
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

    private E get(Object[] a, int index) {
        return (E) a[index];
    }
}    
```
不论是不是在多线程下都不需要进行加锁，因为读取的是副本数组


**迭代器的实现**

```java
    static final class COWIterator<E> implements ListIterator<E> {
        /** Snapshot of the array */
        private final Object[] snapshot;
        /** Index of element to be returned by subsequent call to next.  */
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }

        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (! hasNext())
                throw new NoSuchElementException();
            return (E) snapshot[cursor++];
        }
}
```
迭代器的实现也是通过快照的模式来保证多线程的情况下，迭代的正确性。







