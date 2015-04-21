package cy.crbook;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import snae.tmc.TMURL;

import android.os.AsyncTask;
import android.util.Log;
import cy.common.util.WSUtil;
import cy.crbook.util.StringResultPostProcess;

public class AsyncGetHtmlPage  extends AsyncTask<String, Void, String>{
	
	private static final String TAG = "AsyncGetHtmlPage";
	StringResultPostProcess lpp;
	CRApplication myApp;
	
	public AsyncGetHtmlPage(CRApplication myApp, StringResultPostProcess lpp){
		this.lpp = lpp;
		this.myApp = myApp;
	}
	
	@Override
	protected String doInBackground(String... params) {
		String result="";
		try {
			URL url = new URL(params[0]);
			InputStream in = url.openStream();
			//TMURL tmurl = myApp.getTMURL(params[0]);
			//HttpURLConnection con = tmurl.getHttpUrlConnection();
	        //InputStream in = con.getInputStream();
	        result = WSUtil.getStringFromInputStream(in, "GBK");
	        in.close();
		}catch(Exception e){
			Log.e(TAG, "error get url:" + params[0], e);
		}
		return result;
	}
	
	@Override
    protected void onPostExecute(String v) {
		if (lpp!=null){
			lpp.postProcess(v);
		}
    }
}