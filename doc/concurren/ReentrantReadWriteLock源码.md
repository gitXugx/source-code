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
}

abstract static class Sync extends AbstractQueuedSynchronizer {
    protected final int tryAcquireShared(int unused) {
        Thread current = Thread.currentThread();
        int c = getState();
        //读锁被获取，并且获取的不是当前线程，返回-1，证明当前线程有写锁或者整体没有写锁
        if (exclusiveCount(c) != 0 &&
            getExclusiveOwnerThread() != current)
            return -1;
        //获取共享锁    
        int r = sharedCount(c);
        //当前读锁应该阻塞:1.非公平锁当队列头部下一个是独占锁时，该读锁阻塞，防止写锁饥饿。2.公平锁当前线程不是头节点的下一个节点时应该阻塞
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
        //如果上面读取失败，下面死循环+cas进行获取读锁
        return fullTryAcquireShared(current);
    }

    final int fullTryAcquireShared(Thread current) {
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                //是否有独占
                if (exclusiveCount(c) != 0) {
                    //独占如果不是当前线程直接返回
                    if (getExclusiveOwnerThread() != current)
                        return -1;
                    // else we hold the exclusive lock; blocking here
                    // would cause deadlock.
                } else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                     //当前线程不是   
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0)
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)
                            return -1;
                    }
                }
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











