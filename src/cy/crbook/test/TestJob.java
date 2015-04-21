package cy.crbook.test;

import cy.crbook.util.ComparableJob;

public class TestJob implements ComparableJob{

	int priority;
	int id;
	
	public TestJob(int id, int priority){
		this.id = id;
		this.priority = priority;
	}
	
	public String toString(){
		return "id:" + id + ", priority:" + priority;
	}
	
	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int compareTo(ComparableJob another) {
		return priority-another.getPriority();
	}

}
