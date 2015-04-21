package cy.crbook.util;

import java.util.concurrent.FutureTask;

public class MyComparableFutureTask extends FutureTask implements Comparable<MyComparableFutureTask>{

	private Comparable runComparable;
	private Runnable runnable;
	
	
	public Runnable getRunnable(){
		return runnable;
	}
	
	public Comparable getRunComparable(){
		return runComparable;
	}
	
	public MyComparableFutureTask(Runnable runnable, Object result) {
		super(runnable, result);
		this.runnable= runnable;
		this.runComparable = (Comparable) runnable;
	}

	@Override
	public int compareTo(MyComparableFutureTask another) {
		return runComparable.compareTo(another.getRunComparable());
	}

}
