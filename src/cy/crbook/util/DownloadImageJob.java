package cy.crbook.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import cy.cfs.CallbackOp;
import cy.crbook.CRApplication;
import cy.crbook.FileCache;
import cy.readall.R;

public class DownloadImageJob implements ComparableJob {
	
	private static final String TAG = DownloadImageJob.class.getSimpleName();
	
	//
	CRApplication myApp;
	
	//image
	int lwidth;
	int lheight;
	String fileKey;
	String url;
	String referer;
	
	//post processing
	CallbackOp dipp;
	Object ppParam;//post process parameter passed to dipp
	int priority;
	int saveMode;
	
	public DownloadImageJob(CallbackOp dipp, Object ppParam, CRApplication context, String mergeKey,
			String fileKey, int width, int height, String url, String referer, int priority, int saveMode) {
		
		this.myApp = context;
		this.lwidth = width;
    	this.lheight = height;
    	this.fileKey = fileKey;
    	this.url = url;
    	this.referer = referer;
    	
    	this.dipp = dipp;
    	this.ppParam = ppParam;
    	this.priority = priority;
    	this.saveMode = saveMode;
    }
	
	public String toString(){
		return String.format("width:%d, height:%d, fileKey:%s, url:%s, referer:%s, callback:%s, saveMode:%s, priority:%d", 
				lwidth, lheight, fileKey, url, referer, 
				dipp.getClass().getSimpleName(), myApp.getSaveMode(myApp.getSaveMode()), priority);
	}

	@Override
	public void run(){
		DownloadImageJobManager.singleton.notifyStart(fileKey);
		try{
			Bitmap bmp = ImageUtil.getBitmap(myApp, referer, url, lwidth, lheight);
	    	if (bmp!=null){
	    		if (saveMode==CRApplication.SAVE_TO_FILE){
	    			FileCache.saveFile(bmp, fileKey);
	    		}else if (saveMode == CRApplication.SAVE_TO_CLOUD){
	    			FileCache.saveCloud(bmp, fileKey, myApp);
	    		}
	    		Log.i(TAG, String.format("success downloaded job: %s",  this.toString()));
	    		dipp.onSuccess(ppParam, bmp);
	    	}else{
	    		Log.e(TAG, String.format("failed to download job: %s",  this.toString()));
	    		bmp = BitmapFactory.decodeResource(myApp.getResources(), R.drawable.empty_cover);
	    		dipp.onFailure(ppParam, bmp);
	    	}
		}catch(Throwable e){
			dipp.onFailure(ppParam, null);
			Log.e(TAG, "", e);
		}finally{
			DownloadImageJobManager.singleton.notifyEnd(fileKey);
		}
	}

	@Override
	public int compareTo(ComparableJob another) {
		return priority-another.getPriority();
	}

	@Override
	public int getPriority() {
		return priority;
	}
}