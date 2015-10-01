package cy.crbook.wsclient;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import snae.tmc.TMURL;
import cy.common.entity.Book;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.common.util.WSUtil;
import cy.crbook.CRApplication;
import cy.crbook.persist.SQLitePersistManager;
import cy.crbook.util.ImportReadingTask;

public class RestClientPersistManager {
	
	private static final String TAG = "RestClientPersistManager";
	private static final String WS_NAME = "crbookws/services/crbookrs";
	
	private Context context;
	private CRApplication myApp;
	
	public RestClientPersistManager(Context context, SQLitePersistManager localPersistManager){
		this.context = context;
		this.myApp = (CRApplication)context;
		this.localPersistManager = localPersistManager;
	}
	
	private SQLitePersistManager localPersistManager;
	
	private String getStringRspFromUrl(String url) throws Exception{
		TMURL tmUrl = myApp.getTMURL(url);
		HttpURLConnection con = tmUrl.getHttpUrlConnection();
		int code = con.getResponseCode();
		if (code == HttpURLConnection.HTTP_OK){
			InputStream is = con.getInputStream();
			String result = WSUtil.getStringFromInputStream(is);
			return result;
		}else if (code == HttpURLConnection.HTTP_UNAUTHORIZED){
			Log.w(TAG, "no quota left on SNE");
			myApp.sneQuotaUsedup();
		}else{
			Log.e(TAG, "unexpected response code:" + code);
		}
		return null;
	}
	
	private String getEncodedURL(String url){
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return url;
		}
	}
	
	private void submitSave(List<? extends Reading> vl){
        if (vl!=null && vl.size()!=0){
	        ImportReadingTask irt = new ImportReadingTask("Syncing...", vl.size()+"", context, localPersistManager, vl);
	        Log.i(TAG, "syncing.."+vl);
	        myApp.submit(irt);
        }
	}
	
	private void submitSave(Reading r){
        List<Reading> rl = new ArrayList<Reading>();
        rl.add(r);
        submitSave(rl);
	}
	
	private List<Volume> getVolumesByParam(String param, String userId, int type, int offset, int limit) {
		if (myApp.getLocalMode()){
			return new ArrayList<Volume>();
		}
		userId = WSUtil.convertToEmptyParam(userId);

		try {
	        String strUrl = this.myApp.getWsUrl() + WS_NAME + "/crbookrs/volumes/" + param + 
	        		"/" + offset + "/" + limit + "/" + userId + "/" + type;
	        String result = getStringRspFromUrl(strUrl);
	        if (result!=null){
	        	List<Volume> vl = Volume.fromTopJSONListString(result);
	        	submitSave(vl);
	        	return vl;
	        }else{
	        	return null;
	        }
		}catch(Exception e){
			Log.e(TAG, "", e);
			return null;
		}
	}

	public List<Volume> getVolumesByPCat(String pcat, int offset, int limit) {
		//get volumes by pcat is not linked to my books, both user id and type not mattered
		return getVolumesByParam("cat/" + getEncodedURL(pcat), "", -1, offset, limit);
	}
	
	public List<Volume> getVolumesLike(String param, String userId, int type, int offset, int limit) {
		param = WSUtil.convertToEmptyParam(param);
		return getVolumesByParam("like/" + getEncodedURL(param), userId, type, offset, limit);
	}
	
	//param: volumesCount/cat/
	private long getCountByParam(String param, String userId, int type){
		if (myApp.getLocalMode()){
			return 0;
		}
		userId = WSUtil.convertToEmptyParam(userId);
		try {
	        String strUrl = this.myApp.getWsUrl() + WS_NAME + "/crbookrs/" + param + "/" + userId + "/" + type;
	        String result = getStringRspFromUrl(strUrl); 
	        if (result!=null){
	        	return Long.parseLong(result);
	        }else{
	        	return 0;
	        }
		}catch(Exception e){
			Log.e(TAG, "", e);
			return 0;
		}
	}
	
	public long getVCByPCat(String pcat) {
		//not linked to my reading, both userId and type does not matter
		return getCountByParam("volumesCount/cat/" + pcat, "", -1);
	}
	
	public long getVCLike(String param, String userId, int type) {
		param = WSUtil.convertToEmptyParam(param);
		return getCountByParam("volumesCount/like/" + getEncodedURL(param), userId, type);
	}

	public Volume getVolumeById(String id) {
		if (myApp.getLocalMode()){
			return null;
		}
		
		try {
	        String strUrl = this.myApp.getWsUrl() + WS_NAME + "/crbookrs/volumes/" + getEncodedURL(id);
	        String result = getStringRspFromUrl(strUrl);
	        if (result!=null){
		        Volume v = new Volume();
		        v.fromTopJSONString(result);
		        submitSave(v);
		        return v;
	        }else{
	        	return null;
	        }
		}catch(Exception e){
			Log.e(TAG, "", e);
			return null;
		}
	}

	private List<Book> getBooksByParam(String param, String userId, int type, int offset,int limit){
		if (myApp.getLocalMode()){
			return new ArrayList<Book>();
		}
		userId = WSUtil.convertToEmptyParam(userId);
		try {
	        String strUrl = this.myApp.getWsUrl() + WS_NAME + "/crbookrs/books/" + param + "/" 
	        			+ offset + "/" + limit + "/" + userId + "/" + type;
	        String result = getStringRspFromUrl(strUrl);
	        if (result!=null){
	        	List<Book> bl = Book.fromTopJSONListString(result);
	        	submitSave(bl);
	        	return bl;
	        }else{
	        	return null;
	        }
		}catch(Exception e){
			Log.e(TAG, "", e);
			return null;
		}
	}
	public List<Book> getBooksByName(String name, String userId, int type, int offset, int limit) {
		name = WSUtil.convertToEmptyParam(name);
		return getBooksByParam("name/"+getEncodedURL(name), userId, type, offset, limit);
	}

	public List<Book> getBooksByCat(String catId, int offset, int limit){
		//not linked to my reading, type -1 not used
		return getBooksByParam("cat/"+getEncodedURL(catId), "", -1, offset, limit);
	}

	public Book getBookById(String id) {
		if (myApp.getLocalMode()){
			return null;
		}
		
		try {
	        String strUrl = this.myApp.getWsUrl() + WS_NAME + "/crbookrs/books/" + getEncodedURL(id);
	        String result = getStringRspFromUrl(strUrl);
	        if (result!=null){
	        	Book b = new Book();
	        	b.fromTopJSONString(result);
	        	Log.i(TAG, "book:" + b);
	        	submitSave(b);
	        	return b;
	        }else{
	        	return null;
	        }
		}catch(Exception e){
			Log.e(TAG, "", e);
			return null;
		}
	}
	
	public long getBCByCat(String catId) {
		//not linked to my reading, both userId and type not mattered
		return getCountByParam("booksCount/cat/" + getEncodedURL(catId), "", -1);
	}

	public long getBCByName(String name, String userId, int type) {
		name = WSUtil.convertToEmptyParam(name);
		return getCountByParam("booksCount/name/" + getEncodedURL(name), userId, type);
	}

	//return RemotePersistManager.LoginFailed or sessionid
	public String login(String device, String userId, String password, String stime) {
		try {
			password = WSUtil.convertToEmptyParam(password);
			userId = WSUtil.convertToEmptyParam(userId);
			String strUrl = this.myApp.getWsUrl() + WS_NAME + "/crbookrs/login/" + device + "/" + userId + "/" + password + "/" + stime;
	        return getStringRspFromUrl(strUrl);
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return null;
		}
	}

	public boolean logout(String sessionId, String etime) {
		try {
			String strUrl = this.myApp.getWsUrl() + WS_NAME + "/crbookrs/logout/" + sessionId + "/" + etime;
	        String ret = getStringRspFromUrl(strUrl);
	        if (ret!=null){
	        	return Boolean.parseBoolean(ret);
	        }else{
	        	return false;
	        }
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return false;
		}
	}

	public String addUser(String user, String pass) {
		try {
			String strUrl = this.myApp.getWsUrl() + WS_NAME + "/crbookrs/signup/" + user + "/" + pass;
			return getStringRspFromUrl(strUrl);
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return e.toString();
		}
	}
	
	public boolean removeUser(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	private int intReturnFromPutMethod(String url, String userId, List<String> ids){
		String idsString = "";
		for (int i=0; i<ids.size(); i++){
			if (i>0){
				idsString += "," + ids.get(i);
			}else{
				idsString += ids.get(i);
			}
		}
		HttpClient httpclient = new DefaultHttpClient();
		HttpPut httpput = new HttpPut(url);
		try{
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		    nameValuePairs.add(new BasicNameValuePair("userId", userId));
		    nameValuePairs.add(new BasicNameValuePair("rids", idsString));
		    httpput.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		    HttpResponse response = httpclient.execute(httpput);
	        String ret= EntityUtils.toString(response.getEntity());
	        return Integer.parseInt(ret);
		}catch (Exception e) {
			Log.e(TAG, "", e);
			return 0;
		}
	}
	public int addMyReadings(String userId, List<String> ids){
		return intReturnFromPutMethod(this.myApp.getWsUrl() + WS_NAME + "/crbookrs/myreadings/add/", userId, ids);
	}
	
	public int deleteMyReadings(String userId, List<String> ids){
		return intReturnFromPutMethod(this.myApp.getWsUrl() + WS_NAME + "/crbookrs/myreadings/delete/", userId, ids);
	}
}
