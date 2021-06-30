/**
 * 
 */
package sfdc.formula.impl;

import java.util.*;

import com.google.common.collect.Iterators;

/**
 * Deque doesn't support NULL as a valid value, and we support pushing bare
 * "null" as a value onto the stack, so this is a Deque that accepts and returns
 * null values using a null operator.
 * 
 * @author stamm
 * @since 2021-ish
 */
public class FormulaStack extends AbstractCollection<Object> implements Deque<Object> {
	private static final Object NULL_VALUE = new Object();
	private final ArrayDeque<Object> delegate;
	
	public FormulaStack() {
		this.delegate = new ArrayDeque<>();
	}

	public FormulaStack(int size) {
		this.delegate = new ArrayDeque<>(size);
	}

	// Mask methods
	private static final Object maskNull(Object o) {
		return o == null ? NULL_VALUE : o;
	}

	
	private static final Object unmaskNull(Object o) {
		return o == NULL_VALUE ? null : o;
	}
	
	@Override
	public void addFirst(Object e) {
		delegate.addFirst(maskNull(e));
	}

	@Override
	public void addLast(Object e) {
		delegate.addLast(maskNull(e));
	}

	@Override
	public boolean offerFirst(Object e) {
		return delegate.offerFirst(maskNull(e));
	}

	@Override
	public boolean offerLast(Object e) {
		return delegate.offerLast(maskNull(e));
	}
	@Override
	public Object removeFirst() {
		return unmaskNull(delegate.removeFirst());
	}

	@Override
	public Object removeLast() {
		return unmaskNull(delegate.removeLast());
	}

	@Override
	public Object pollFirst() {
		return unmaskNull(delegate.pollFirst());
	}

	@Override
	public Object pollLast() {
		return unmaskNull(delegate.pollLast());
	}

	@Override
	public Object getFirst() {
		return unmaskNull(delegate.getFirst());
	}

	@Override
	public Object getLast() {
		return unmaskNull(delegate.getLast());
	}

	@Override
	public Object peekFirst() {
		return unmaskNull(delegate.peekFirst());
	}

	@Override
	public Object peekLast() {
		return unmaskNull(delegate.peekLast());
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		return delegate.removeFirstOccurrence(maskNull(o));
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		return delegate.removeLastOccurrence(maskNull(o));
	}

	@Override
	public boolean offer(Object e) {
		return delegate.offer(maskNull(e));
	}

	@Override
	public Object remove() {
		return unmaskNull(delegate.remove());
	}

	@Override
	public Object poll() {
		return unmaskNull(delegate.poll());
	}

	@Override
	public Object element() {
		return unmaskNull(delegate.element());
	}

	@Override
	public Object peek() {
		return unmaskNull(delegate.peek());
	}

	@Override
	public void push(Object e) {
		delegate.push(maskNull(e));
	}

	@Override
	public Object pop() {
		return unmaskNull(delegate.pop());
	}

	@Override
	public Iterator<Object> descendingIterator() {
		return Iterators.transform(delegate.descendingIterator(), (v)->unmaskNull(v));
	}

	@Override
	public Iterator<Object> iterator() {
		return Iterators.transform(delegate.iterator(), (v)->unmaskNull(v));
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return delegate.size();
	}

}
