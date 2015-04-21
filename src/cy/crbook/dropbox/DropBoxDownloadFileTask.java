/*
 * Copyright (c) 2010-11 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */


package cy.crbook.dropbox;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import cy.readall.R;
import cy.cfs.CallbackOp;
import cy.crbook.util.Increment;

/**
 * Here we show getting metadata for a directory and downloading a file in a
 * background thread, trying to show typical exception handling and flow of
 * control for an app that downloads a file from Dropbox.
 */

public class DropBoxDownloadFileTask extends AsyncTask<List<Entry>, Long, List<Entry>> {


    private static final String TAG = "DownloadDropBoxFile";
	private Context mContext;
    private DropboxAPI<?> mApi;

    private FileOutputStream mFos;

    private boolean mCanceled=false;
    private Long mFileLen;
    private String mErrorMsg;
    private Object ppParam;
    
    private CallbackOp dipp;
  
    //notification
	NotificationManager notificationManager;
	Notification notification = null;
	int noteid;
	
	public static String getDownloadFile(Entry e){
		 return Environment.getExternalStorageDirectory().getPath()  + e.parentPath() + e.fileName();
	}
	
    public DropBoxDownloadFileTask(CallbackOp dipp, Object ppParam,
    		Context context, DropboxAPI<?> api) {
        mContext = context;

        mApi = api;
        this.dipp = dipp;
        this.ppParam = ppParam;
        
        //the text that appears first on the status bar
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String tickerText = "Downloading...";
        long time = System.currentTimeMillis();
        notification = new Notification(R.drawable.icon_cr, tickerText, time);
        noteid = Increment.getInt();
    }
    
    @Override
    protected List<Entry> doInBackground(List<Entry>... params) {
    	List<Entry> downloadedList = new ArrayList<Entry>();
        try {
            if (mCanceled) {
                return downloadedList;
            }
            
            //the bold font
            String contentTitle = "Downloading:";
            //the text that needs to change
            String contentText = "files size:";
            
            // download ...
            for (Entry ent: params[0]) {                
                if (mCanceled) {
                    return downloadedList;
                }
                String path = ent.path;
                mFileLen = ent.bytes;
                String cachePath = Environment.getExternalStorageDirectory().getPath()  + 
                		ent.parentPath();
                String cacheFile = cachePath + ent.fileName();
                File dir = new File(cachePath);
                if (!dir.exists()){
                	dir.mkdirs();
                }
                try {
                    mFos = new FileOutputStream(cacheFile);
                } catch (FileNotFoundException e) {
                    mErrorMsg = "Couldn't create a local file to store the image:" + cachePath;
                    return downloadedList;
                }
                contentTitle += path+",";
                contentText += mFileLen + ",";

                notification.setLatestEventInfo(mContext, contentTitle, contentText, null);
                notificationManager.notify(noteid, notification);
                //mApi.getFile(ent.path, null, mFos, null);
                //to be cancellable
                BufferedInputStream br=null;
                BufferedOutputStream bw=null;
                try {
	                br = new BufferedInputStream(mApi.getFileStream(ent.path, null));
	                bw = new BufferedOutputStream(mFos);
	                byte[] buffer = new byte[4096];
	    			int read;
	    			while (true) {
	    				read = br.read(buffer);
	    				if (read <= 0) {
	    					break;
	    				}
	    				bw.write(buffer, 0, read);
	    			}
                }catch(IOException e){
                	Log.e(TAG, "", e);
                }finally {
        			//in finally block:
                	try {
	        			if (bw != null) {
	        				bw.close();
	        			}
	        			if (br != null) {
	        				br.close();
	        			}
                	}catch(IOException ioe){
                		Log.e(TAG, "", ioe);
                	}
        		}
                
                downloadedList.add(ent);
            }
            return downloadedList;

        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Download canceled";
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
                // can't be thumbnailed
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                Log.e(TAG, "user is over quota.");
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = "Unknown error.  Try again.";
        }
        return downloadedList;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
    	Log.i(TAG, progress[0] + "");
    }

    @Override
    protected void onPostExecute(List<Entry> downloadedEntries) {
    	notificationManager.cancel(noteid);
        dipp.onSuccess(ppParam, downloadedEntries);
    }
}
