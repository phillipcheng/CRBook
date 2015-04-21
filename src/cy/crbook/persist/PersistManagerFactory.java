package cy.crbook.persist;

import android.content.Context;
import cy.common.persist.LocalPersistManager;
import cy.common.persist.RemotePersistManager;
import cy.crbook.wsclient.RestClientPersistManager;

public class PersistManagerFactory {
	public static int PER_TYPE_SQLITE=0;
	public static int PER_TYPE_JDBC=1;
	public static int PER_TYPE_KSOAP=2;
	public static int PER_TYPE_RESTFUL=3;
	
	public static RestClientPersistManager getRemotePersistManager(Context context, SQLitePersistManager localPersistManager){
		return new RestClientPersistManager(context, localPersistManager);
	}
	
	public static SQLitePersistManager getLocalPersistManager(Context context){
		
		DBHelper dbhelper = new DBHelper(context);
		return new SQLitePersistManager(dbhelper);
		
	}
}
