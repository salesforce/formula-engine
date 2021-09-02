/**
 * 
 */
package com.force.formula;

import java.time.temporal.ChronoField;

/**
 * Represents a Time
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaTime {
    FormulaTime addHours(int hours);
    FormulaTime addMinutes(int minutes);
    FormulaTime addSeconds(int seconds);

    FormulaTime addMilliseconds(int millis);

    /////////////////////////////////////////////////////////////////////////////
    // Getters

    /**
     * @return the number of millisecond since midnight in GMT TimeZone.  Always will be 0 - 86400000.
     */
    long getTimeInMillis();

    /**
     * @return the number of seconds since midnight in GMT TimeZone.  Always will be 0 - 86400.  Millisecond
     *         values are truncated.
     */
    long getTimeInSeconds();

    /**
     * @return 0 - 23
     */
    int getHourOfDay();

    /**
     * @return 0 - 59
     */
    int getMinute();

    /**
     * @return 0 - 59
     */
    int getSecond();

    /**
     * @return 0 - 999
     */
    int getMillisecond();
    
    /**
     * Default implementation of FormulaTime that wraps java.time.LocalTime
     * @author stamm
     */
    public static class TimeWrapper implements FormulaTime {
    	private final java.time.LocalTime delegate;
    	public TimeWrapper(java.time.LocalTime delegate) {
    		assert delegate != null;
    		this.delegate = delegate;
    	}
    	public java.time.LocalTime getDelegate() {
    		return this.delegate;
    	}
    	
		@Override
		public FormulaTime addHours(int hours) {
			return new TimeWrapper(delegate.plusHours(hours));
		}
		@Override
		public FormulaTime addMinutes(int minutes) {
			return new TimeWrapper(delegate.plusMinutes(minutes));
		}
		@Override
		public FormulaTime addSeconds(int seconds) {
			return new TimeWrapper(delegate.plusSeconds(seconds));
		}
		@Override
		public FormulaTime addMilliseconds(int millis) {
			return new TimeWrapper(delegate.plusNanos(millis * 1000000L));
		}
		@Override
		public long getTimeInMillis() {
			return this.delegate.get(ChronoField.MILLI_OF_DAY);
		}
		@Override
		public long getTimeInSeconds() {
			return this.delegate.get(ChronoField.SECOND_OF_DAY);
		}
		@Override
		public int getHourOfDay() {
			return this.delegate.getHour();
		}
		@Override
		public int getMinute() {
			return this.delegate.getMinute();
		}
		@Override
		public int getSecond() {
			return this.delegate.getSecond();
		}
		@Override
		public int getMillisecond() {
			return this.delegate.get(ChronoField.MILLI_OF_SECOND);
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TimeWrapper) {
				return delegate.equals(((TimeWrapper)obj).delegate);
			} else if (obj instanceof FormulaTime) {
				return getTimeInMillis() == (((FormulaTime)obj).getTimeInMillis());
			}
			return false;
		}
		@Override
		public int hashCode() {
			return delegate.hashCode();
		}
		@Override
		public String toString() {
			return delegate.toString();
		}
    }
}
