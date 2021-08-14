package com.force.formula.impl;

public class JvmMetrics {
    public long cpuUsageNanos = 0;
    public long gcTimeMillis = 0;
    public long threadWaitedTime = 0;
    public long threadBlockedTime = 0;
    public long threadSafepointWaitedTime = 0;
    public long threadAllocatedBytes = 0;

    public static JvmMetrics diff(JvmMetrics start, JvmMetrics end) {
        JvmMetrics diff = new JvmMetrics();
        diff.cpuUsageNanos = end.cpuUsageNanos - start.cpuUsageNanos;
        diff.gcTimeMillis = end.gcTimeMillis - start.gcTimeMillis;
        diff.threadWaitedTime = end.threadWaitedTime - start.threadWaitedTime;
        diff.threadBlockedTime = end.threadBlockedTime - start.threadBlockedTime;
        diff.threadSafepointWaitedTime = end.threadSafepointWaitedTime - start.threadSafepointWaitedTime;
        diff.threadAllocatedBytes = end.threadAllocatedBytes - start.threadAllocatedBytes;
        return diff;
    }

    @Override
    public String toString() {
        return "JvmMetrics{" +
                "cpuUsageNanos=" + cpuUsageNanos +
                ", gcTimeMillis=" + gcTimeMillis +
                ", threadWaitedTime=" + threadWaitedTime +
                ", threadBlockedTime=" + threadBlockedTime +
                ", threadSafepointWaitedTime=" + threadSafepointWaitedTime +
                ", threadAllocatedBytes=" + threadAllocatedBytes +
                '}';
    }
}
