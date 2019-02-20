# AbstractQueuedSynchronizer源码
> 该类提供了基于先进先出的阻塞队列和

## AbstractQueuedSynchronizer

**声明极字段**
```java

public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements java.io.Serializable {
    //等待队列的头部
    private transient volatile Node head;
    //等待队列的尾部
    private transient volatile Node tail;
    //同步的状态
    private volatile int state;

```



