package io.github.snowflakelite;

/**
 * snowflake lite is a lightweight, distributed unique ID generator.
 * <p> useage: </p>
 * <p> SnowflakeLite snowflakeLite = new SnowflakeLite(0, 0); </p>
 * <p> long id = snowflakeLite.nextId(); </p>
 */
public class SnowflakeLite {
    /**
     * 开始时间截 (2024-12-01)，
     */
    private final long twepoch = 173301120000L;

    /**
     * 机器id所占的位数
     */
    private final long workerIdBits = 5L;

    /**
     * 数据中心id所占的位数
     */
    private final long datacenterIdBits = 5L;

    /**
     * 支持的最大机器id，结果是31
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * 支持的最大数据中心id，结果是31
     */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    /**
     * 序列在id中占的位数，扩至15位
     */
    private final long sequenceBits = 15L;

    /**
     * 机器ID向左移15位
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 数据中心id向左移20位(15+5)
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间截向左移25位(5+5+15)
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 生成序列的掩码，这里为32767
     */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 工作机器ID(0~31)
     */
    private long workerId;

    /**
     * 数据中心ID(0~31)
     */
    private long datacenterId;

    /**
     * 毫秒内序列(0~32767)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    /**
     * Constructors
     *
     * @param workerId     工作ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    public SnowflakeLite(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * Constructors
     *
     * @param workerId     工作ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     * @param lastTimestamp lastTimestamp
     */
    public SnowflakeLite(long workerId, long datacenterId, long lastTimestamp) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        if (lastTimestamp <= 0) {
            throw new IllegalArgumentException(String.format("lastTimestamp[%d] can't less than 0", lastTimestamp));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.lastTimestamp = lastTimestamp;

    }

    // ==============================Methods==========================================

    /**
     * 获得下一个ID
     *
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        boolean offset = false;
        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过。
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp;
            offset = true;
        }

        //如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                if (offset) {
                    timestamp = timestamp + 1;
                } else {
                    //阻塞到下一个毫秒,获得新的时间戳
                    timestamp = tilNextMillisWindow(timestamp);
                }

            }
        }
        //时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        //上次生成ID的时间截
        lastTimestamp = timestamp;

        //移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift) //
                | (datacenterId << datacenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;
    }

    protected long tilNextMillisWindow(long lastTimestamp) {
        try {
            Thread.sleep(10);
            long timestamp = timeGen();
            if (timestamp <= lastTimestamp) {
                // Clock moved backwards again. Offset timestamp Manually
                timestamp = lastTimestamp + 1;
            }
            return timestamp;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(10ms)
     */
    protected long timeGen() {
        return System.currentTimeMillis() / 10;
    }

}
