package cy.crbook;

import cy.crbook.util.SignupPostProcess;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncSignup extends AsyncTask<Void, Void, String>{
	
	private static final String TAG = "AsyncSignup";
	
	String user;
	String pass;
	SignupPostProcess spp;
	CRApplication myApp;
	
	AsyncSignup(String user, String pass, SignupPostProcess pp, CRApplication myApp){
		this.user = user;
		this.pass = pass;
		this.spp = pp;
	}
	
	@Override
	protected String doInBackground(Void... params) {
		return myApp.getRemotePersistManager().addUser(user, pass);
	}
	
	@Override
    protected void onPostExecute(String v) {
		if (spp!=null){
			spp.signupPostProcess(v);
		}
    }
}