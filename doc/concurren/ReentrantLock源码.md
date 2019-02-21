# ReentrantLock源码

## ReentrantLock

**字段以及声明**

```java
public class ReentrantLock implements Lock, java.io.Serializable {
    //同步器，可以实现公平同步和非公平同步
    private final Sync sync;
    //基于AQS实现的同步器
    abstract static class Sync extends AbstractQueuedSynchronizer {
    }
    //在此基础上实现的非公平同步器
    static final class NonfairSync extends Sync {
    }
        //在此基础上实现的公平同步器
    static final class FairSync extends Sync {
    }
}
```

字段没有特殊的字段，同步器的主要实现是AQS。

**构造方法**
```java
public class ReentrantLock implements Lock, java.io.Serializable {
    //创建非公平锁
    public ReentrantLock() {
        sync = new NonfairSync();
    }
    //true是公平同步器，false是非公平同步器
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
}
```

构造方法就俩个，初始化同步器。默认的是非公平同步器，带参数的是 `true` 是公平同步器


**lock**

```java
public class ReentrantLock implements Lock, java.io.Serializable {
    //实际上调用的同步器的两个实现
    public void lock() {
        sync.lock();
    }
    //非公平锁不用排对去获取锁。
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        final void lock() {
            //直接尝试获取锁，不需要排队，比较当前状态state是AQS的成员变量默认0，设置为1标识已被独占
            if (compareAndSetState(0, 1))
                //把当前线程设置为独占线程，表示该线程已拿到锁
                setExclusiveOwnerThread(Thread.currentThread());
            else
                //获得锁失败，调用AQS的模板方法，继续尝试获取
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            //尝试获取状态值调用父类syn的方法
            //1.首先尝试再次判断compareAndSetState(0, 1)能否设值成功
            //2.如果是当前线程则状态值加1,可重入的实现
            return nonfairTryAcquire(acquires);
        }
    }

    //公平锁，只能当前线程是该阻塞队列的头元素的下一个元素才有资格获取锁
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                //只有队列中的下一元素才能获取锁
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    //设置为当前线程独占
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                //当前线程再次获得锁
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }
}

```

AQS分为两种同步模式，一种是独占，一种是共享。重入锁使用的是独占模式，这里重入锁有两种实现方式。
1. 公平锁: 维护一个有序的阻塞队列，获取锁的只能是当前持有锁的节点的下一个节点
2. 非公平锁: 虽然维护一个阻塞队列，当获取锁的时候谁都能获取


主要使用 `CAS` 和volatile修饰的state来实现锁。其主要框架是`AQS`来约束，具体获取锁的实现是由子类实现 `tryAcquire`


**acquire**

实现调用父类的模板方法

```java
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements java.io.Serializable {

  public final void acquire(int arg) {
        //尝试获取锁，获取失败会加入阻塞到队列中
        if (!tryAcquire(arg) &&
            //这个代表创建添加独占对象到队列
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            //中断被重置后，获取到了锁，现在把该线程标识为重置状态。让外部知道该线程是中断线程
            selfInterrupt();
    }

    private Node addWaiter(Node mode) {
        //节点里面存的是当前线程和同步模式
        Node node = new Node(Thread.currentThread(), mode);
        // 尝试直接添加到队列尾部，因为一般情况下队列中有排队的情况多，在并发小的情况下可直接添加成功
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        //如果上面队列添加失败，则会死循环添加
        enq(node);
        return node;
    }
    private Node enq(final Node node) {
        for (;;) {
            //首先看是不是空队列，如果是空队列则初始化head和tail
            Node t = tail;
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                //添加队列尾部
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }
    //尝试获取队列的节点，还要检查前面的节点是否已中断，中断的话，剔除前面节点线程
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                //获取当前节点的前一个节点
                final Node p = node.predecessor();
                //如果前一个节点是head则尝试获取锁成功
                if (p == head && tryAcquire(arg)) {
                    //则把当前节点设置为head
                    setHead(node);
                    //断开连接
                    p.next = null; // help GC
                    //获取锁成功
                    failed = false;
                    //前一个节点线程是正常的
                    return interrupted;
                }
                //如果前一个节点线程处于阻塞状态则挂起当前线程
                if (shouldParkAfterFailedAcquire(p, node) &&
                    //挂起当前线程，恢复的时候，并判断当前线程是否被中断，如果中断则恢复中断标识，并从节点中剔除
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                //保证该节点要么被设置为无效，要么状态被重置为0，重置为0的情况下属于中断的状态被重置过。
                cancelAcquire(node);
        }
    }
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        //判断前一个节点的线程的状态，为阻塞
        if (ws == Node.SIGNAL)
            return true;
        //0是处于取消状态，-1是处于阻塞状态，-2处于等待状态 0 处于初始状态   
        if (ws > 0) {
            //如果前一个线程状态为取消，则跳过之前连续取消的节点，然后重新尝试获取锁
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            //如果前一个节点没有被取消，则把前一个节点标记为阻塞状态，然后循环挂起挂起当前线程
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }
    //当前线程中断取消当前线程
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;
        node.thread = null;
        // 跳过前面无效的线程
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        Node predNext = pred.next;
        //把当前节点设置为取消状态
        node.waitStatus = Node.CANCELLED;

        // 如果当前节点是尾节点，则把尾节点设置为有效的节点
        if (node == tail && compareAndSetTail(node, pred)) {
            //并把有效节点下一个节点设置为null
            compareAndSetNext(pred, predNext, null);
        } else {
            int ws;
            //如果pred不是头结点，而且设置为阻塞状态成功时
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                Node next = node.next;
                //如果node的下一个节点不为取消节点则设置为有效节点的下一个节点
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                //pre线程是头线程，或pre线程是不是阻塞状态，或者是无效线程，则unpark离node节点最近有效的节点，去尝试获取锁。顺便检查前一个节点
                unparkSuccessor(node);
            }
            node.next = node; // help GC
        }
    }
    private void unparkSuccessor(Node node) {
        //如果该节点被其他节点设置了，则恢复该节点状态，尝试恢复该节点状态
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);
        Node s = node.next;
        //循环遍历找出离node节点最近的有效next节点
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        //然后unpark该线程
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    public final boolean release(int arg) {
        //释放锁成功，并unpark后续最近的一个节点
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }


}
```
上面是获取lock的具体逻辑。
`addWaiter` 根据并发情况来进行设计，并发的热点是尾部数据，当并发竞争比较小的时候直接尝试添加尾部，并发失败时才去尝试死循环添加，从而有效节约资源。
`acquireQueued` 如果前面是头结点，进行尝试获取锁，如果获取成功当前节点设置为头结点。若失败则挂起当前线程，进行等待状态。如果当前线程中途被中断，unpark后获进行设置为取消状态。并 `unpark` next 离当前节点最近的 `未取消节点`。让其进行竞争锁。


**unlock**

```java
public class ReentrantLock implements Lock, java.io.Serializable {

    public void unlock() {
        sync.release(1);
    }
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        boolean free = false;
        //如果重入锁全部释放则释放成功，否则释放失败
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }
}
```
重入锁使用`AQS`框架开发，使用共享变量state、park、unpark和阻塞队列实现高性能可重入锁。


**tryLock**
```java

public class ReentrantLock implements Lock, java.io.Serializable {

    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
    //尝试获取锁
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        //先直接尝试获取锁    
        return tryAcquire(arg) ||
            //把该节点添加到队列，如果在该时间内未获取到锁，则挂起该线程，返回false
            doAcquireNanos(arg, nanosTimeout);
    }
}
```

让开发人员在获取不到锁，做其他的事情

