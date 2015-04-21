package cy.crbook;

import java.util.List;

import android.os.AsyncTask;
import android.util.Log;
import cy.crbook.util.MyReadingPostProcess;

public class AsyncMyReadingProcess extends AsyncTask<Void, Void, Integer>{

	public static final int ADD_OP=1;
	public static final int DEL_OP=2;
	
	private static final String TAG = "AsyncMyReadingProcess";
	
	MyReadingPostProcess mrpp;
	String userId;
	List<String> ids;
	int op;
	CRApplication myApp;
	
	AsyncMyReadingProcess(String userId, List<String>ids, int op, 
			MyReadingPostProcess mrpp, CRApplication myApp){
		this.op = op;
		this.userId=userId;
		this.ids =ids;
		this.mrpp=mrpp;
		this.myApp = myApp;
	}
	
	@Override
	protected Integer doInBackground(Void... arg0) {
		int ret=0;
		if (this.op==ADD_OP){
			ret = myApp.getRemotePersistManager().addMyReadings(userId, ids);
		}else if (this.op==DEL_OP){
			ret = myApp.getRemotePersistManager().deleteMyReadings(userId, ids);
		}else{
			Log.e(TAG, "operation type not supported:" + op);
		}
		return Integer.valueOf(ret);
	}
	
	@Override
    protected void onPostExecute(Integer v) {
		if (mrpp!=null){
			mrpp.myReadingPostProcess(v.intValue());
		}
    }

}
