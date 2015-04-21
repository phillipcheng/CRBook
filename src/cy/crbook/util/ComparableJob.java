package cy.crbook.util;

public interface ComparableJob extends Runnable, Comparable<ComparableJob>{
	
	public int getPriority();

}
