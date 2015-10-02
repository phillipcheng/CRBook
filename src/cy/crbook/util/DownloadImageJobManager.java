package cy.crbook.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import cy.cfs.CallbackOp;
import cy.crbook.CRApplication;
import cy.readall.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;


public class DownloadImageJobManager {
	
	private static final String TAG = DownloadImageJobManager.class.getSimpleName();
	
	//notification
	NotificationManager notificationManager;
	Notification notification = null;
	int noteid;
	String notificationText;
	CRApplication myApp;
	String contentTitle="Downloading...";
	
	public static DownloadImageJobManager singleton;

	private Map<String, Future> submittedFutures = new ConcurrentHashMap<String, Future>();
	private Map<String, DownloadImageJob> submittedJobs = new ConcurrentHashMap<String, DownloadImageJob>();
	
	public DownloadImageJobManager(CRApplication myApp){
		this.myApp = myApp;
		notificationManager = (NotificationManager) myApp.getSystemService(Context.NOTIFICATION_SERVICE);
        notification = new Notification(R.drawable.icon_cr, contentTitle, System.currentTimeMillis());
        noteid = Increment.getInt();
	}
	
	public void notifyStart(String fileKey){
		notificationText+="," + fileKey;
		notification.setLatestEventInfo(myApp, contentTitle, submittedFutures.size() + ":" + notificationText, null);
		notificationManager.notify(noteid, notification);
		Log.i(TAG, "download fileKey:" +fileKey + " started..");
	}
	
	public void notifyEnd(String fileKey){
		submittedFutures.remove(fileKey);
		String removeString = "," + fileKey;
		notificationText = notificationText.substring(0, notificationText.indexOf(removeString)) + 
			notificationText.substring(notificationText.indexOf(removeString) + removeString.length(), notificationText.length());
		notification.setLatestEventInfo(myApp, contentTitle, submittedFutures.size() + ":" + notificationText, null);
		notificationManager.notify(noteid, notification);
		Log.i(TAG, "download fileKey:" +fileKey + " finished..");
	}
	
	private void submitJob(String fileKey, DownloadImageJob dij){
		Log.i(TAG, "job sumbitted:" + dij);
		Future f = myApp.submit(dij);
		submittedJobs.put(fileKey, dij);//replace it
		submittedFutures.put(fileKey, f);//replace it
	}
	
	public void submitDownloadImageJob(CallbackOp dipp, Object ppParam, CRApplication myApp, String mergeKey,
			String fileKey, int width, int height, String url, String referer, boolean isHighPriority){
		int priority;
		if (isHighPriority){
			priority = CRApplication.PRIORITY_DOWNLOAD_HIGH;
		}else{
			priority = CRApplication.PRIORITY_DOWNLOAD_NORMAL;
		}
		int saveMode=this.myApp.getSaveMode();
		DownloadImageJob dij = new DownloadImageJob(dipp, ppParam, myApp, mergeKey, fileKey, width, height, 
				url, referer, priority, saveMode);
		DownloadImageJob existJob = submittedJobs.get(fileKey);
		if (existJob==null){
			if (width==0 || height==0){
				Log.e(TAG, "image with fileKey:" + fileKey +", mergeKey:" + mergeKey + " has problem in width and height.");
			}
			submitJob(fileKey, dij);	
		}else{	
			Future f = submittedFutures.get(fileKey);
			if (f!=null)
				f.cancel(true);
			submitJob(fileKey, dij);
		}
	}
	
	public void cancelAll(){
		for (String key: submittedFutures.keySet()){
			Future f = submittedFutures.get(key);
			f.cancel(true);		
		}
	}
	
}