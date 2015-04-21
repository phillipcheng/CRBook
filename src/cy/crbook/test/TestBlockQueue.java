package cy.crbook.test;

import static org.junit.Assert.*;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Test;

import cy.crbook.util.ComparableJob;
import cy.crbook.util.FIFORunnableEntry;
import cy.crbook.util.MyComparableFutureTask;

public class TestBlockQueue {

	@Test
	public void test() {
		PriorityBlockingQueue<Runnable> bqr = new PriorityBlockingQueue<Runnable>(5);
		
		TestJob[] tjarray = new TestJob[]{
				new TestJob(1, 1), 
				new TestJob(2, 1),
				new TestJob(3, 1), 
				new TestJob(4, 1),
				new TestJob(1, 0)
				};
		
		for (TestJob tj : tjarray){
			 bqr.put(new MyComparableFutureTask(new FIFORunnableEntry<ComparableJob>(tj), null));
		}
		
		MyComparableFutureTask tj;
		while ((tj = (MyComparableFutureTask) bqr.poll())!=null){
			System.out.println((TestJob)(((FIFORunnableEntry)tj.getRunnable()).getEntry()));
		}	
	}

}
