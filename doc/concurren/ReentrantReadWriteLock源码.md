# ReentrantReadWriteLock源码

## ReentrantReadWriteLock

**字段以及声明**

```java
public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    //内部类读锁
    private final ReentrantReadWriteLock.ReadLock readerLock;
    //内部类写锁
    private final ReentrantReadWriteLock.WriteLock writerLock;
    //同步器
    final Sync sync;
}    
```
`ReentrantReadWriteLock` 实现了 `ReadWriteLock` 提供了获取读锁和写锁的接口。`Sync` 同步器依然是 `AQS` 框架的实现。


**构造方法**

```java
public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {

    public ReentrantReadWriteLock() {
        this(false);
    }

    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }
}
```
`ReentrantReadWriteLock` 和 `ReentrantLock` 构造方法创建的锁是类似的，都是默认为非公平锁。不同的是 `ReentrantReadWriteLock` 还实例化了 `ReadLock`、`WriteLock`。

**ReadLock.lock**

```java
public static class ReadLock implements Lock, java.io.Serializable {
    //读锁和写锁使用的是同一个同步器。
    protected ReadLock(ReentrantReadWriteLock lock) {
        sync = lock.sync;
    }
    public void lock() {
        //获取共享读锁,调用的是AQS的acquireShared方法
        sync.acquireShared(1);
    }
}
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements java.io.Serializable {
    //获取共享锁
    public final void acquireShared(int arg) {
        //tryAcquireShared实现是在syn中
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }
    private void doAcquireShared(int arg) {
        //把添加读线程节点到尾部
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                //获取前一个节点
                final Node p = node.predecessor();
                if (p == head) {
                    //如果前一个节点是head节点，再尝试获取锁
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
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
}

abstract static class Sync extends AbstractQueuedSynchronizer {
    protected final int tryAcquireShared(int unused) {
        Thread current = Thread.currentThread();
        int c = getState();
        //写锁被获取，并且获取的不是当前线程，则返回-1。继续执行证明当前线程有写锁或者整体没有写锁
        if (exclusiveCount(c) != 0 &&
            getExclusiveOwnerThread() != current)
            return -1;
        //获取读锁    
        int r = sharedCount(c);
        //当前线程读锁应该持有写锁，才能导致后面阻塞的是一个是读锁。
        if (!readerShouldBlock() &&
            //读锁不能大于最大持有数
            r < MAX_COUNT &&
            //设置共享锁+1
            compareAndSetState(c, c + SHARED_UNIT)) {
            //获取读锁成功，如果为第一个读的    
            if (r == 0) {
                //初始第一个读者
                firstReader = current;
                //第一个读锁计数器
                firstReaderHoldCount = 1;
            //第一个读的是当前线程    
            } else if (firstReader == current) {
                //自增即可
                firstReaderHoldCount++;
            } else {
                //记录当前线程读锁的次数
                HoldCounter rh = cachedHoldCounter;
                //如果拥有者为空或者不是当前线程
                if (rh == null || rh.tid != getThreadId(current))
                    //获取当前线程拥有者，又的话获取，没有的话初始化
                    cachedHoldCounter = rh = readHolds.get();
                //如果HoldCounter是当前线程的
                else if (rh.count == 0)
                    //把读锁拥有者设置成当前线程拥有者
                    readHolds.set(rh);
                //计数器+1，统计锁的计数，是为了在上下文中更方便的释放该线程的读锁
                rh.count++;
            }
            //返回获取成功
            return 1;
        }
        //如果上面获取读锁失败，下面死循环+cas进行获取读锁
        return fullTryAcquireShared(current);
    }

    final int fullTryAcquireShared(Thread current) {
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                //是否有独占
                if (exclusiveCount(c) != 0) {
                    //写锁被其他线程获取，直接返回把当前线程加入到等待队列排队
                    if (getExclusiveOwnerThread() != current)
                        return -1;
                //证明有读锁阻塞了写锁，或者其他线程获取了读锁
                } else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        //当前线程如果多次获得了读锁证明不会出现死锁问题，因为当前读锁阻塞了写锁
                        // assert firstReaderHoldCount > 0;
                     //当前线程不是   
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            //最后一个线程的rh锁的缓冲，有可能不是当前线程
                            if (rh == null || rh.tid != getThreadId(current)) {
                                //获取该线程的计数器
                                rh = readHolds.get();
                                //如果count=0 里面没有读锁，证明在其他线程还有读锁，导致后面的写锁在阻塞
                                if (rh.count == 0)
                                    readHolds.remove();
                            }
                        }
                        //所以把该读锁添加到队列中，让写锁来进行获取锁，以免发生死锁
                        if (rh.count == 0)
                            return -1;
                    }
                }
                //否则该线程以持有锁，接着重入锁
                //重入计数小于最大值
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                //比较设置计数是否成功    
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release
                    }
                    return 1;
                }
            }
        }
}
```











