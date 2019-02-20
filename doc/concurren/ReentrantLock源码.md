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
            //比较当前状态state是AQS的成员变量默认0，设置为1标识已被独占
            if (compareAndSetState(0, 1))
                //把当前线程设置为独占线程，表示该线程已拿到锁
                setExclusiveOwnerThread(Thread.currentThread());
            else
                //获得锁，调用AQS的模板方法
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


主要使用 `CAS` 和volatile修饰的state来实现锁。
