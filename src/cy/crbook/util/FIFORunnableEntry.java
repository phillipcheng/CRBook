package cy.crbook.util;

import java.util.concurrent.atomic.AtomicLong;

public class FIFORunnableEntry<E extends Comparable<? super E>> implements Comparable<FIFORunnableEntry<E>>, Runnable {
	
	final static AtomicLong seq = new AtomicLong();
	final long seqNum;
	final E entry;
	
	
	public FIFORunnableEntry(E entry) {
		seqNum = seq.getAndIncrement();
		this.entry = entry;
	}
	
	public E getEntry() { return entry; }
	
	public int compareTo(FIFORunnableEntry<E> other) {
		int res = entry.compareTo(other.entry);
		if (res == 0 && other.entry != this.entry)
		  res = (seqNum < other.seqNum ? -1 : 1);
		return res;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		((Runnable)entry).run();
	}

}