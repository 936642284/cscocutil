package com.yzycoc.util;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @program: cscocutil
 * @description:
 * @author: yzy
 * @create: 2020-10-07 09:36
 * @Version 1.0
 **/
public class BaseLocalThreadPool {
    /**
     * 本线程用的线程池
     */
    private static ThreadLocal<Executor> localExecutorThread;

    /**
     * 线程池仓库，保管保存了的线程池
     */
    private static Map<String, Executor> poolWarehouse;

    ///////////////////////////线程池参数///////////////////////////////

    /**
     * 核心池的大小
     */
    private static int corePoolSize = 0;

    /**
     * 线程池最大线程数，这个参数也是一个非常重要的参数，它表示在线程池中最多能创建多少个线程；
     */
    private static int maximumPoolSize = 500;

    /**
     * 表示线程没有任务执行时最多保持多久时间会终止。
     * 默认情况下，只有当线程池中的线程数大于corePoolSize时，keepAliveTime才会起作用，
     * 直到线程池中的线程数不大于corePoolSize，即当线程池中的线程数大于corePoolSize时，
     * 如果一个线程空闲的时间达到keepAliveTime，则会终止，直到线程池中的线程数不超过corePoolSize。
     * 但是如果调用了allowCoreThreadTimeOut(boolean)方法，在线程池中的线程数不大于corePoolSize时，keepAliveTime参数也会起作用，
     * 直到线程池中的线程数为0；
     */
    private static long keepAliveTime = 5;

    /**
     * unit：参数keepAliveTime的时间单位，有7种取值，在TimeUnit类中有7种静态属性:
     * TimeUnit.DAYS;              //天
     * TimeUnit.HOURS;             //小时
     * TimeUnit.MINUTES;           //分钟
     * TimeUnit.SECONDS;           //秒
     * TimeUnit.MILLISECONDS;      //毫秒
     * TimeUnit.MICROSECONDS;      //微妙
     * TimeUnit.NANOSECONDS;       //纳秒
     */
    private static TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /**
     * 一个阻塞队列，用来存储等待执行的任务，这个参数的选择也很重要，
     * 会对线程池的运行过程产生重大影响，一般来说，这里的阻塞队列有以下几种选择：
     * ArrayBlockingQueue;
     * LinkedBlockingQueue;
     * SynchronousQueue;
     * ArrayBlockingQueue和PriorityBlockingQueue使用较少，一般使用LinkedBlockingQueue和Synchronous。
     * 线程池的排队策略与BlockingQueue有关。
     */
    private static BlockingQueue<Runnable> workQueue = new SynchronousQueue<>();



    /**
     * 线程工厂，主要用来创建线程；
     */
    private static ThreadFactory defaultThreadFactory = Thread::new;


    /**
     * 默认线程池的名称
     */
    private static String defaultName = "default";

    /**
     *  初始化线程池仓库
     */
    static {
        //应当使用线程安全的map
        poolWarehouse = new ConcurrentHashMap<>(4);

//        Map<String, Executor> fixedMap = new HashMap<>(1);
        PoolConfig poolConfig = new PoolConfig();
        poolWarehouse.put(
                defaultName,
                new ThreadPoolExecutor(
                        poolConfig.getCorePoolSize(),
                        poolConfig.getMaximumPoolSize(),
                        poolConfig.getKeepAliveTime(),
                        poolConfig.getTimeUnit(),
                        poolConfig.getWorkQueue(),
                        poolConfig.getDefaultThreadFactory()
                ));
    }

    /**
     * 创建线程池的工厂,无名称，使用默认
     * @return
     */
    public static Executor getThreadPool() {
        return createThreadPool(null);
    }

    /**
     * 创建线程池的工厂
     *
     * @param poolName 创建的线程池的名称
     * @return
     */
    public static Executor getThreadPool(String poolName) {
        return createThreadPool(poolName);
    }

    /**
     * 创建线程池的工厂,无名称，使用默认
     *
     * @return
     */
    public static Executor getThreadPool(PoolConfig poolConfig) {
        return createThreadPool(null, poolConfig);
    }

    /**
     * 创建线程池的工厂
     *
     * @param poolName 创建的线程池的名称
     * @return
     */
    public static Executor getThreadPool(String poolName, PoolConfig poolConfig) {
        return createThreadPool(poolName, poolConfig);
    }


    /**
     * 清除某指定的线程池
     *
     * @param poolName
     * @return
     */
    public static boolean removeThreadPool(String poolName) {
        //移除
        return poolWarehouse.remove(poolName) != null;
    }


    /**
     * 获取本线程中的线程池
     *
     * @return
     */
    public static Executor getLocalThreadPool() {
        return createLocalThreadPool();
    }

    /**
     * 清除本线程中的线程池
     */
    public static boolean removeLocalThreadPool() {
        try {
            localExecutorThread.remove();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 获取线程工厂
     *
     * @return
     */
    public static ThreadFactory getFactory() {
        return defaultThreadFactory;
    }


    /**
     * 创建线程池
     *  通过配置类创建
     */
    private static Executor createThreadPool(String poolName, PoolConfig config) {
        Executor executor;
        config = config == null ? new PoolConfig() : config;
        //如果名称为null，获取默认
        if (poolName == null) {
            executor = poolWarehouse.get(defaultName);
            if (executor == null) {
                executor = createExecutor(config);
                //保存
                poolWarehouse.put(defaultName, executor);
            }
        } else {
            //存在,若有则直接获取，没有则创建
            Executor executorGet = poolWarehouse.get(poolName);
            if (executorGet == null) {
                //获取不到,创建新的并保存
                //创建并赋值
                executorGet = createExecutor(config);
                //保存
                poolWarehouse.put(poolName, executorGet);
            }
            //赋值
            executor = executorGet;
        }

        //返回线程池
        return executor;
    }

    /**
     * 创建线程池, 默认配置
     */
    private static Executor createThreadPool(String poolName){
        return createThreadPool(poolName, null);
    }


    /**
     * 创建本线程同步线程池
     * @return
     */
    private static Executor createLocalThreadPool() {
        Executor executor = localExecutorThread.get();
        if (executor == null) {
            //如果没有，创建
            executor = createExecutor(new PoolConfig());
            localExecutorThread.set(executor);
        }
        return executor;
    }
    /**
     * 创建本线程同步线程池
     *
     * @return
     */
    private static Executor createLocalThreadPool(PoolConfig config) {
        Executor executor = localExecutorThread.get();
        if (executor == null) {
            //如果没有，创建
            executor = createExecutor(config);
            localExecutorThread.set(executor);
        }
        return executor;
    }


    /**
     * 根据配置对象创建一个新的线程
     */
    private static Executor createExecutor(PoolConfig config){
        return new ThreadPoolExecutor(
                config.getCorePoolSize(),
                config.getMaximumPoolSize(),
                config.getKeepAliveTime(),
                config.getTimeUnit(),
                config.getWorkQueue(),
                config.getDefaultThreadFactory()
        );
    }




    /* ——————————————getter & setter———————————— */

    public static int getCorePoolSize() {
        return corePoolSize;
    }
    @Deprecated
    public static void setCorePoolSize(int corePoolSize) {
        BaseLocalThreadPool.corePoolSize = corePoolSize;
    }

    public static int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    @Deprecated
    public static void setMaximumPoolSize(int maximumPoolSize) {
        BaseLocalThreadPool.maximumPoolSize = maximumPoolSize;
    }

    public static long getKeepAliveTime() {
        return keepAliveTime;
    }
    @Deprecated
    public static void setKeepAliveTime(long keepAliveTime) {
        BaseLocalThreadPool.keepAliveTime = keepAliveTime;
    }

    public static TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * 设置清除时间的类型
     * @param timeUnit  时间类型
     */
    @Deprecated
    public static void setTimeUnit(TimeUnit timeUnit) {
        //如果为null,则抛出异常
        BaseLocalThreadPool.timeUnit = Objects.requireNonNull(timeUnit);
    }

    public static BlockingQueue<Runnable> getWorkQueue() {
        return workQueue;
    }

    /**
     * 设置线程等候阻塞队列
     */
    @Deprecated
    public static void setWorkQueue(BlockingQueue<Runnable> workQueue) {
        //如果为null,则抛出异常
        BaseLocalThreadPool.workQueue = Objects.requireNonNull(workQueue);
    }

    /**
     * 设置默认线程工厂
     * @param defaultThreadFactory  线程工厂
     */
    @Deprecated
    public static void setDefaultThreadFactory(ThreadFactory defaultThreadFactory) {
        //如果为null,则抛出异常
        BaseLocalThreadPool.defaultThreadFactory = Objects.requireNonNull(defaultThreadFactory);

    }


    /**
     * 配置类，提供几个参数
     */
    public static class PoolConfig{
        /**
         * 核心池的大小
         */
        private int corePoolSize = 0;

        /**
         * 线程池最大线程数，这个参数也是一个非常重要的参数，它表示在线程池中最多能创建多少个线程；
         */
        private int maximumPoolSize = 500;

        /**
         * 表示线程没有任务执行时最多保持多久时间会终止。
         * 默认情况下，只有当线程池中的线程数大于corePoolSize时，keepAliveTime才会起作用，
         * 直到线程池中的线程数不大于corePoolSize，即当线程池中的线程数大于corePoolSize时，
         * 如果一个线程空闲的时间达到keepAliveTime，则会终止，直到线程池中的线程数不超过corePoolSize。
         * 但是如果调用了allowCoreThreadTimeOut(boolean)方法，在线程池中的线程数不大于corePoolSize时，keepAliveTime参数也会起作用，
         * 直到线程池中的线程数为0；
         */
        private long keepAliveTime = 5;

        /**
         * unit：参数keepAliveTime的时间单位，有7种取值，在TimeUnit类中有7种静态属性:
         * TimeUnit.DAYS;              //天
         * TimeUnit.HOURS;             //小时
         * TimeUnit.MINUTES;           //分钟
         * TimeUnit.SECONDS;           //秒
         * TimeUnit.MILLISECONDS;      //毫秒
         * TimeUnit.MICROSECONDS;      //微妙
         * TimeUnit.NANOSECONDS;       //纳秒
         */
        private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

        /**
         * 一个阻塞队列，用来存储等待执行的任务，这个参数的选择也很重要，
         * 会对线程池的运行过程产生重大影响，一般来说，这里的阻塞队列有以下几种选择：
         * ArrayBlockingQueue;
         * LinkedBlockingQueue;
         * SynchronousQueue;
         * ArrayBlockingQueue和PriorityBlockingQueue使用较少，一般使用LinkedBlockingQueue和Synchronous。
         * 线程池的排队策略与BlockingQueue有关。
         */
        private BlockingQueue<Runnable> workQueue = new SynchronousQueue<>();



        /**
         * 线程工厂，主要用来创建线程；
         */
        private ThreadFactory defaultThreadFactory = Thread::new;

        //**************** 构造 & setter & getter ****************//

        public PoolConfig(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit timeUnit, BlockingQueue<Runnable> workQueue, ThreadFactory defaultThreadFactory) {
            this.corePoolSize = corePoolSize;
            this.maximumPoolSize = maximumPoolSize;
            this.keepAliveTime = keepAliveTime;
            this.timeUnit = timeUnit;
            this.workQueue = workQueue;
            this.defaultThreadFactory = defaultThreadFactory;
        }

        public PoolConfig() {
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public long getKeepAliveTime() {
            return keepAliveTime;
        }

        public void setKeepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        public BlockingQueue<Runnable> getWorkQueue() {
            return workQueue;
        }

        public void setWorkQueue(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
        }

        public ThreadFactory getDefaultThreadFactory() {
            return defaultThreadFactory;
        }

        public void setDefaultThreadFactory(ThreadFactory defaultThreadFactory) {
            this.defaultThreadFactory = defaultThreadFactory;
        }
    }
}
