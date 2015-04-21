package cy.crbook.util;

public class Increment {
	private static int seed=0;
	
	public static synchronized int getInt(){
		seed++;
		return seed;
	}

}
