package cy.crbook;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.cld.util.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;

import snae.tmc.TMHttpClient;
import snae.tmc.TMURL;
import snae.tmc.TMURLManager;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.provider.Settings.Secure;

import cy.common.entity.Book;
import cy.common.entity.Volume;
import cy.common.persist.RemotePersistManager;
import cy.crbook.persist.PersistManagerFactory;
import cy.crbook.persist.SQLitePersistManager;
import cy.crbook.util.ComparableJob;
import cy.crbook.util.DownloadImageJobManager;
import cy.crbook.util.FIFORunnableEntry;
import cy.crbook.util.GenernalPostProcess;
import cy.crbook.util.ImageUtil;
import cy.crbook.util.MyComparableFutureTask;
import cy.crbook.wsclient.RestClientPersistManager;

public class CRApplication extends Application {

	private static final String TAG = "CRApplication";
	private static final String APP_PREF_KEY="crapp_pref";
	
	private SQLitePersistManager localPersistManager = null;
	private RestClientPersistManager remotePersistManager = null;
	
	//
	private int pageSize=20;//limit
	//all the reading definition coming from local or remote web service.
	private boolean localMode=true;
	//
	public static final int NO_SAVE=0;
	public static final int SAVE_TO_FILE=1;
	public static final int SAVE_TO_CLOUD=2;
	private int saveMode=NO_SAVE;
	//
	public static final int READ_INTERNET=0;
	public static final int READ_FROM_FILE=1;
	public static final int READ_FROM_CLOUD=2;
	private int readMode=READ_INTERNET;
	
	public String getReadMode(int mode){
		if (mode == READ_INTERNET){
			return "internet";
		}else if (mode==READ_FROM_FILE){
			return "file";
		}else if (mode==READ_FROM_CLOUD){
			return "cloud";
		}else{
			return "unsupported";
		}
	}
	public String getSaveMode(int mode){
		if (mode == NO_SAVE){
			return "no save";
		}else if (mode==SAVE_TO_FILE){
			return "file";
		}else if (mode==SAVE_TO_CLOUD){
			return "cloud";
		}else{
			return "unsupported";
		}
	}
	//
	private int threadNum=5;
	//show my-reading or all-reading
	private boolean myReadingMode=false;
	
	//
	private boolean loggedIn;
	private String userid=null;
	private String password=null;
	private String sessionId=null;
	private String deviceId = null;

	//
	private String wsUrl = "http://52.1.96.115:8080/";

	MenuItem loginMenu=null;
	
	//sne (smart network engine) connection config
	public static String KEY_SNEHOST="snehost";
	public static String KEY_SNEPORT="sneport";
	private String sneHost="";
	private int snePort=0;
	private TMURLManager tmMgr;
	
	public TMURL getTMURL(String url) throws MalformedURLException{
		if (tmMgr==null){
			return new TMURL(url);
		}else{
			return tmMgr.getUrl(url);
		}
	}
	
	public void setProxyCBText(Button sneProxyBtn){
		if (getTmurlMgr()==null){
			sneProxyBtn.setText("Disconnected from SNE.");
		}else{
			int status = getTmurlMgr().getStatus();
			if (status==TMHttpClient.STATUS_CONNECTED){
				sneProxyBtn.setText("Connected to SNE.");
			}else if (status==TMHttpClient.STATUS_DISCONNECTED){
				sneProxyBtn.setText("Disconnected from SNE.");
			}else if (status==TMHttpClient.STATUS_CONNECTING){
				sneProxyBtn.setText("Connectting to SNE...");
			}else if (status==TMHttpClient.STATUS_DISCONNECTING){
				sneProxyBtn.setText("Disconnectting from SNE...");
			}else{
				sneProxyBtn.setText(getTmurlMgr().getFailedReason());
			}
		}
	}
	
	public void setupSNE(String proxyHost, int proxyPort, String user, final Button sneBtn){
		this.sneHost = proxyHost;
		this.snePort = proxyPort;
		this.userid = user;
		if (user==null || "".equals(user)){
			sneBtn.setText("Login before use SNE.");
			return;
		}
		if (tmMgr==null
				||tmMgr.getStatus()==TMHttpClient.STATUS_DISCONNECTED
				||tmMgr.getStatus()==TMHttpClient.STATUS_ERROR){
			//re-create http client
			tmMgr= new TMURLManager(sneHost, snePort);
			
			(new AsyncTask<Object, Void, Void>(){
				@Override
				protected Void doInBackground(Object... params) {
					TMURLManager tmMgr = (TMURLManager)params[0];
					String userId = (String)params[1];
					tmMgr.start(userId);
					return null;
				}
				
				@Override
			    protected void onPostExecute(Void v) {
					setProxyCBText(sneBtn);
			    }
			}).execute(new Object[]{tmMgr, getUserid()});
		}else{
			//disconnect
			(new AsyncTask<TMURLManager, Void, Void>(){
				@Override
				protected Void doInBackground(TMURLManager... params) {
					params[0].end();
					return null;
				}
				
				@Override
			    protected void onPostExecute(Void v) {
					setProxyCBText(sneBtn);
			    }
			}).execute(tmMgr);
		}
	}
	
	public void sneQuotaUsedup(){
		if (tmMgr!=null){
			synchronized(tmMgr){
				tmMgr.end();
			}
		}
	}
	
	//cached site configuration, referer, contentXPath, etc
	public static Map<String, Volume> siteConfMap = new HashMap<String, Volume>(); //site-id to site-conf map
	
	public void loadPreference(){
		SharedPreferences prefs = getSharedPreferences(APP_PREF_KEY, Context.MODE_PRIVATE);
		String jsonPref = prefs.getString(APP_PREF_KEY, null);
		if (jsonPref!=null){
			JSONObject jobj;
			try {
				jobj = new JSONObject(jsonPref);
				sneHost = jobj.getString(KEY_SNEHOST);
				snePort = jobj.getInt(KEY_SNEPORT);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void savePreference(){
		SharedPreferences prefs = getSharedPreferences(APP_PREF_KEY, Context.MODE_PRIVATE);
		JSONObject jobj = new JSONObject();
		try {
			jobj.put(KEY_SNEHOST, sneHost);
			jobj.put(KEY_SNEPORT, snePort);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		prefs.edit().putString(APP_PREF_KEY, jobj.toString()).apply();
	}
	
	public Volume getTemplate(String bookId){
		int idx = bookId.indexOf(".");
		if (idx!=-1){
			String siteId = bookId.substring(0, idx);
			String rootVolId = siteId + "." + siteId;
			if (siteConfMap.containsKey(rootVolId)){
				return siteConfMap.get(rootVolId);
			}else{
				Log.e(TAG, "rootVolId not exist:" + rootVolId);
				return null;
			}
		}else{
			Log.e(TAG, "bookId does not contain dot:" + bookId);
			return null;
		}
	}

	public void setLocalMode(boolean v){
		if (localMode!=v){
			if (v==true){
				//set to local from remote
				logout(null);
			}else if(v==false){
				logon(userid, password);
			}
			localMode = v;
		}
	}
	
	//strange, number lower, priority higher?
	public static int PRIORITY_IMPORT_HIGH=5;
	public static int PRIORITY_DOWNLOAD_HIGH=5;
	public static int PRIORITY_DOWNLOAD_NORMAL=10;
	
	class MyThreadPoolExecutor extends ThreadPoolExecutor {

	    public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
				long keepAliveTime, TimeUnit unit,
				BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}

		@Override
	    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
	        return new MyComparableFutureTask(runnable, value);            
	    }
	}
	
	private PriorityBlockingQueue<Runnable> bqr = new PriorityBlockingQueue<Runnable>(threadNum);
	private ThreadPoolExecutor pool = new MyThreadPoolExecutor(threadNum, threadNum, 1, TimeUnit.MINUTES, bqr);
	
	public Future submit(Runnable rb){
		FIFORunnableEntry<ComparableJob> entry = new FIFORunnableEntry<ComparableJob>((ComparableJob)rb);
		return pool.submit(entry);
	}
	
	class QuitGPP implements GenernalPostProcess{
		@Override
		public void postProcess() {
			Process.killProcess(Process.myPid());
		}		
	}
	
	class AyncLogout extends AsyncTask<Void, Void, Void>{
		GenernalPostProcess gpp;
		
		AyncLogout(GenernalPostProcess gpp){
			this.gpp = gpp;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			if (sessionId!=null && !"".equals(sessionId)){
				String etime = RemotePersistManager.SDF_SERVER_DTZ.format(new Date());
				boolean ret = remotePersistManager.logout(sessionId, etime);
				Log.i(TAG, "logout: sessionId:" + sessionId + ", ret:" + ret);
			}else{
				Log.i(TAG, "no sessionId can't logout.");
			}
			return null;
		}
		
		@Override
        protected void onPostExecute(Void v) {
			if (gpp!=null){
				gpp.postProcess();
			}
        }
	}
	
	public void logon(String user, String password){
		(new AsyncLogon(deviceId, user, password, null, this)).execute();
		(new AsyncTask<Void, Void, Void>(){
			@Override
			protected Void doInBackground(Void... params) {
				buildTemplateCache();
				return null;
			}
		}).execute();
	}
	
	public void logout(GenernalPostProcess gpp){
		(new AyncLogout(gpp)).execute();
	}	

	public void buildTemplateCache() {
		//populate the htmlTemplateCache
		String[] rootId = new String[Volume.ROOT_VOLUMES.size()];
		rootId = Volume.ROOT_VOLUMES.keySet().toArray(rootId);
		String catlist = StringUtil.toStringList(rootId);
		List<Volume> volList = remotePersistManager.getVolumesByPCat(catlist, 0, 50);
		for (Volume v : volList){
			siteConfMap.put(v.getId(), v);
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		localPersistManager = PersistManagerFactory.getLocalPersistManager(this);
		remotePersistManager = PersistManagerFactory.getRemotePersistManager(this, localPersistManager);
		
		//create default books from assets/internal if not yet
		AssetManager am = getAssets();
		try {
			String[] files = am.list(CRConstants.ASSET_SAMPLE_DIR);
			for (int i=0; i<files.length; i++){
				Log.w(TAG, files[i]);
				createBookFromAsset(am, CRConstants.ASSET_SAMPLE_DIR + "/" + files[i]);
			}
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		//go online
		setLocalMode(false);	
		
		DownloadImageJobManager.singleton = new DownloadImageJobManager(this);
	}
	
	public void quit(){
		QuitGPP qgpp = new QuitGPP();
		if (!localMode)
			logout(qgpp);
		else
			qgpp.postProcess();
	}
	
	// e.g. dir = samples/count
	public void createBookFromAsset(AssetManager am, String dir){
		String id = dir.substring(CRConstants.ASSET_SAMPLE_DIR.length()+1);
		Book b = localPersistManager.getBookById(id);
		if (b==null){
			try {
				String[] files;
				files = am.list(dir);
				if (files.length>0){				
					b = new Book();
					b.setCat(Volume.ROOT_VOLUME_SELF);
					b.setId(id);
					b.setName(id);
					List<String> pageUris = new ArrayList<String>();
					for (int i=0; i<files.length;i++){
						String f = files[i];						
						if (f.contains(CRConstants.FILE_COVER)){
							b.setCoverUri(ImageUtil.ASSET + dir + "/" + f);
						}else{
							pageUris.add(ImageUtil.ASSET + dir + "/" + f);
						}						
					}				
					localPersistManager.createBookAndPageUrls(b, pageUris);
				}else{
					Log.e(TAG, "dir:" + dir + " is not a book directory.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			localPersistManager.insertOrUpdateBook(b);
			Log.i(TAG, "book:" + id + " exists.");
		}	
	}

	//setter and getter
	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}
	public int getPageSize(){
		return pageSize;
	}
	public void setPageSize(int ps){
		pageSize = ps;
	}
	
	private boolean isNetworkConnected() {
	  ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	  return cm.getActiveNetworkInfo()!=null;
	}
	
	public boolean getLocalMode(){
		if (!isNetworkConnected()){
			return true;
		}
		return localMode;
	}

	public void setSaveMode(int sm){
		saveMode = sm;
	}
	public int getSaveMode(){
		return saveMode;
	}
	
	public void setReadMode(int rm){
		readMode = rm;
	}
	public int getReadMode(){
		return readMode;
	}
	public void setThreadNum(int n){
		this.threadNum = n;
	}
	public int getThreadNum(){
		return threadNum;
	}
	public boolean isMyReadingMode() {
		return myReadingMode;
	}
	public void setMyReadingMode(boolean myReadingMode) {
		this.myReadingMode = myReadingMode;
	}
	public void setLoginMenu(MenuItem mi){
		loginMenu=mi;
	}
	public MenuItem getLoginMenu(){
		return loginMenu;
	}
	public SQLitePersistManager getLocalPersistManager() {
		return localPersistManager;
	}

	public void setLocalPersistManager(SQLitePersistManager localPersistManager) {
		this.localPersistManager = localPersistManager;
	}

	public RestClientPersistManager getRemotePersistManager() {
		return remotePersistManager;
	}

	public void setRemotePersistManager(
			RestClientPersistManager remotePersistManager) {
		this.remotePersistManager = remotePersistManager;
	}
	
	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getSneHost() {
		return sneHost;
	}

	public void setSneHost(String sneHost) {
		this.sneHost = sneHost;
	}

	public int getSnePort() {
		return snePort;
	}

	public void setSnePort(int snePort) {
		this.snePort = snePort;
	}

	public TMURLManager getTmurlMgr() {
		return tmMgr;
	}

	public void setTmurlMgr(TMURLManager tmurlMgr) {
		this.tmMgr = tmurlMgr;
	}

	public String getWsUrl() {
		return wsUrl;
	}

	public void setWsUrl(String wsUrl) {
		this.wsUrl = wsUrl;
	}
}
