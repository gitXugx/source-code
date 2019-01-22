# ArrayList源码
> 像他的名字一样是一个数组列表, 低层组要由数组实现, 但是提供的功能比数组更强大。

## 字段以及声明

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{
    private static final long serialVersionUID = 8683452581122892189L;
    //默认数组大小
    private static final int DEFAULT_CAPACITY = 10;
    //空数组
    private static final Object[] EMPTY_ELEMENTDATA = {};
    //默认数组, 给空数组做区分
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
    //最大容量
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    transient Object[] elementData; // non-private to simplify nested class access
    private int size;
}

```
从上往下开始:

`AbstractList`  继承 `AbstractCollection` 实现 `List` 他们俩又继承和实现 `Collection` 和 `Iterable` 接口

<div align="center"> ![](https://github.com/gitXugx/source-code/tree/master/doc/images/AbstractListUml.jpg) </div><br>


- `Iterable` 是迭代器, 实现它可以使用 `for-each` 循环, 它其中的方法定义了获取迭代器的抽象工厂方法和java8的新方法。
- `Collection` 是集合类共有的抽象, 其中也有一些默认方法, 是通用实现。
- `AbstractCollection` 是 `Collection`实现, 完善了集合通用操作。
- `List` 继承了 `Collection` 说明 `List` 属于集合类, 它定义了 `List` 集合的基本抽象。
- `AbstractList` 实现了绝大部分 `List` 的细节, 实现了迭代器, 基于内部内实现 `List` 切割功能。
- `RandomAccess` 随机访问标识接口, 代表该类支持随机访问。 在某些情况下, `List` 可以根据该接口来判断使用最优访问方式
- `Cloneable` 使 `List` 支持浅克隆
- `Serializable` 支持序列化, `serialVersionUID` 序列化的唯一编号

## 方法

先构造方法:

```java
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
    }
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }
    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        if ((size = elementData.length) != 0) {
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elementData.getClass() != Object[].class)
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // replace with empty array.
            this.elementData = EMPTY_ELEMENTDATA;
        }
    } 
}    
```

一共有3个构造方法：

- `ArrayList(int initialCapacity)` 根据容量来创建 `ArrayList` 大小。
- `ArrayList()` 创建一个默认的数组
- `ArrayList(Collection<? extends E> c)` 根据传入的数组进行一个拷贝, 该拷贝是浅拷贝。

添加方法:

```java
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
   public boolean add(E e) {
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        elementData[size++] = e;
        return true;
    }
    public void add(int index, E element) {
        rangeCheckForAdd(index);

        ensureCapacityInternal(size + 1);  // Increments modCount!!
        System.arraycopy(elementData, index, elementData, index + 1,
                            size - index);
        elementData[index] = element;
        size++;
    }
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }
   public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount

        int numMoved = size - index;
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew,
                             numMoved);

        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }
}    
```

一共有5个添加元素的方法:
- `add(E e)` 直接添加一个元素
- `add(int index, E element)` 指定位置插入元素
- `addAll(Collection<? extends E> c)` 把一个集合添加到当前集合
- `addAll(int index, Collection<? extends E> c)` 从指定位置添加一个集合

### `add(E e)` 方法

```java
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private void ensureCapacityInternal(int minCapacity) {
        ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
    }

    private static int calculateCapacity(Object[] elementData, int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            return Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        return minCapacity;
    }

    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }
}
```




















