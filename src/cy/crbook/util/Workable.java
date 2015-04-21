package cy.crbook.util;

import cy.readall.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;


public abstract class Workable implements Runnable {

    private static final String TAG = "Workable";
	//notification
	NotificationManager notificationManager;
	Notification notification = null;

	String taskName;
	String taskParam;
	Context context;
	int noteid;
	boolean isDone=false;
	
	public Workable(String taskName, String taskParam, Context context){
		this.context = context;
		this.taskName = taskName;
		this.taskParam = taskParam;
		
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String tickerText = taskName;
        long time = System.currentTimeMillis();
        notification = new Notification(R.drawable.icon_cr, tickerText, time);
        notification.setLatestEventInfo(context, taskName, taskParam, null);
        noteid = Increment.getInt();
        notificationManager.notify(noteid, notification);
        
	}
	
	public abstract void myRun();
	
	@Override
	public void run(){
        try{
        	myRun();
        }catch(Throwable t){
        	Log.e(TAG, "", t);
        }

    	notificationManager.cancel(noteid);
    	
    	isDone=true;
	}
	
	public boolean isDone(){
		return isDone;
	}
	
	public void updateProgress(String text){
		notification.setLatestEventInfo(context, taskName, text, null);
		notificationManager.notify(noteid, notification);
	}
}
