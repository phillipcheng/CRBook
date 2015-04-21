package cy.crbook;

import java.util.Date;

import cy.common.persist.RemotePersistManager;
import cy.crbook.util.StringResultPostProcess;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncLogon extends AsyncTask<Void, Void, String>{
	
	private static final String TAG = "AsyncLogon";
	String deviceId;
	String user;
	String pass;
	StringResultPostProcess lpp;
	CRApplication myApp;
	
	AsyncLogon(String deviceId, String user, String pass, StringResultPostProcess lpp, CRApplication myApp){
		this.deviceId = deviceId;
		this.user = user;
		this.pass = pass;
		this.lpp = lpp;
		this.myApp = myApp;
	}
	
	@Override
	protected String doInBackground(Void... params) {
		if (deviceId!=null && !"".equals(deviceId)){
			String stime = RemotePersistManager.SDF_SERVER_DTZ.format(new Date());
			return myApp.getRemotePersistManager().login(deviceId, user, pass, stime);
		}else{
			Log.i(TAG, "no androidId can't login.");
			return null;
		}
	}
	
	@Override
    protected void onPostExecute(String v) {
		if (v==null){
			//android id not found
		}else if (RemotePersistManager.LOGIN_FAILED.equals(v)){
			//login failed
		}else{
			//success, store user, pass and session-id
			if (user!=null && !"".equals(user)){
				myApp.setUserid(user);
				myApp.setPassword(pass);
				myApp.getLoginMenu().setTitle(user);
			}
			myApp.setSessionId(v);
		}
		if (lpp!=null){
			lpp.postProcess(v);
		}
    }
}