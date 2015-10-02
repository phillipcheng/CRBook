package cy.crbook.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import snae.tmc.TMURL;

import cy.cfs.CFS;
import cy.cfs.CallbackOp;
import cy.common.entity.Reading;
import cy.crbook.CRApplication;
import cy.crbook.FileCache;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

public class ImageUtil {

	private static final String TAG = ImageUtil.class.getSimpleName();
	
	public static final String ASSET="asset:";
	public static final String CONTENT="content:";
	
	public static final int IMAGE_URL_TYPE_ASSET=0;
	public static final int IMAGE_URL_TYPE_STORAGE=1;
	public static final int IMAGE_URL_TYPE_CONTENT=2;
	public static final int IMAGE_URL_TYPE_URL=3;
	public static final int IMAGE_URL_TYPE_UNKNOWN=10;

	private static int getImageUriType(String uri){
		if (uri!=null){
			File imgFile = new File(uri);
			if (uri.startsWith(ASSET)){
				return IMAGE_URL_TYPE_ASSET;
			}else if (imgFile.exists()){
				return IMAGE_URL_TYPE_STORAGE;
			}else if (uri.startsWith(CONTENT)){
				return IMAGE_URL_TYPE_CONTENT;
			}else{
				return IMAGE_URL_TYPE_URL;
			}
		}else{
			return IMAGE_URL_TYPE_UNKNOWN;
		}
	}
	
	private static InputStream getInputStream(CRApplication context, String referer, String bgUri) throws IOException{
		int type = getImageUriType(bgUri);
		if (type == IMAGE_URL_TYPE_ASSET){
			return context.getAssets().open(bgUri.substring(ASSET.length()));
		}else if (type == IMAGE_URL_TYPE_STORAGE){
			return new FileInputStream(new File(bgUri));
		}else if (type == IMAGE_URL_TYPE_CONTENT){
			return context.getContentResolver().openInputStream(Uri.parse(bgUri));
		}else if (type == IMAGE_URL_TYPE_URL) {
			//from url
			int toM=2000;
			HttpURLConnection connection = null;
			try {
				TMURL tmUrl = context.getTMURL(bgUri);
		        connection = (HttpURLConnection) tmUrl.getHttpUrlConnection();
		        if (referer!=null && !"".equals(referer)){
		        	connection.setRequestProperty("Referer", referer);
		        }
		        connection.setDoInput(true);
		        connection.connect();
		        if (connection.getResponseCode()==HttpURLConnection.HTTP_OK){
		        	connection.setConnectTimeout(toM);
		        	InputStream input = connection.getInputStream();
		        	return input;
		        }else if (connection.getResponseCode()==HttpURLConnection.HTTP_UNAUTHORIZED){
		        	context.sneQuotaUsedup();
		        	Log.e(TAG, String.format("not ok http code: %d", connection.getResponseCode()));
		        	return null;
		        }else{
		        	Log.e(TAG, String.format("not ok http code: %d", connection.getResponseCode()));
		        	return null;
		        }
		    } catch (IOException ioe) {
		    	Log.e(TAG, "image not found from uri:" + bgUri, ioe);
		        return null;
		    } finally{
		    }
		}else{
			Log.e(TAG, "unknown type:" + type);
			return null;
		}
	}

	/*
	 * width < height for portrait mode, this is the screen width and height
	 */
	public static Bitmap getBitmap(CRApplication context, String referer, String bgUri, int lwidth, int lheight){
		if (bgUri!=null){
			 int scale=1;
			 boolean rotate=false;
			 InputStream is = null;
			try {
				BitmapFactory.Options o = new BitmapFactory.Options();
		        o.inJustDecodeBounds = true;
		        is = getInputStream(context, referer, bgUri);
		        if (is==null){
		        	return null;
		        }
		        
		        BitmapFactory.decodeStream(is,null,o);		        
		        if (o.outWidth>o.outHeight){
		        	rotate = true;
		        	//rotate = false;
		        }
		        if (rotate){
		        	int tmp = lwidth;
		        	lwidth=lheight;
		        	lheight=tmp;
		        }
		       
		        while(o.outWidth/scale/2>=lwidth && o.outHeight/scale/2>=lheight)
		            scale*=2;
			}catch(IOException e){
				Log.e(TAG, "io exception.", e);
				return null;
			}finally{
				try {
					if (is!=null)
						is.close();
					else
						return null;
				} catch (IOException e) {
					Log.e(TAG, "close stream exception.", e);
				}
			}
			
	        try{
		        BitmapFactory.Options o2 = new BitmapFactory.Options();
		        o2.inSampleSize=scale;
		        is = getInputStream(context, referer, bgUri);
		        Bitmap bmp = BitmapFactory.decodeStream(is, null, o2);
		        if (bmp!=null){
			        bmp = Bitmap.createScaledBitmap(bmp, lwidth, lheight, true);
			        if (rotate){
			        	Matrix mat = new Matrix();
						mat.postRotate(90f);
			        	bmp = Bitmap.createBitmap(bmp, 0, 0, lwidth, lheight, mat, true);
			        }
		        }
		        return bmp;
			}catch(IOException e){
				Log.e(TAG, "io exception.", e);
				return null;
			}finally{
				try {
					if (is!=null)
						is.close();
				} catch (IOException e) {
					Log.e(TAG, "close stream exception.", e);
				}
			}
		}else{
			return null;
		}
	}
	
	/**
	 * 
	 * @param coverUri
	 * @param r
	 * @param pageNum = -1 for volume cover
	 * @param dpp
	 * @param dppParam
	 * @param context
	 * @param width
	 * @param height
	 * @param isHighPriority
	 * @return
	 */
	public static Bitmap getImageFromUrl(String coverUri, Reading r, int pageNum, 
			CallbackOp dpp, Object dppParam, CRApplication myApp, int width, int height, 
			boolean isHighPriority){
		
		Bitmap bm = null;
		int type = ImageUtil.getImageUriType(coverUri);
		if (type == ImageUtil.IMAGE_URL_TYPE_URL){
			String fileKey = FileCache.generateKey(r, pageNum);
			Log.d(TAG, String.format("get image from url %s, read mode: %s",  coverUri, myApp.getReadMode(myApp.getReadMode())));
			//try file first anyway
			File f = new File(fileKey);
			if (f.exists()||myApp.getLocalMode()){
				bm = BitmapFactory.decodeFile(f.getAbsolutePath());
				dpp.onSuccess(dppParam, bm);
			}else{
				Log.e(TAG, String.format("file %s not found in 'save to file' mode.", fileKey));
				if (myApp.getReadMode()==CRApplication.READ_FROM_CLOUD){//get from cloud
					fileKey = fileKey.substring(FileCache.getCacheRoot().length());
					CFS.asyncGetContent(myApp.getUserid(), fileKey, dppParam, dpp, myApp, width, height);
				}else if (myApp.getReadMode() == CRApplication.READ_INTERNET){//get from internet
					DownloadImageJobManager.singleton.submitDownloadImageJob(dpp, dppParam, myApp, r.getCat(), fileKey, 
							width, height, coverUri, myApp.getTemplate(r.getId()).getReferer(), false);
				}else if (myApp.getReadMode() == CRApplication.READ_FROM_FILE){
					Log.e(TAG, String.format("read mode from file but failed for url: %s", coverUri));
				}
			}
		}else{
			bm = ImageUtil.getBitmap(myApp, myApp.getTemplate(r.getId()).getReferer(), coverUri, 
					width, height);
	        dpp.onSuccess(dppParam, bm);
		}
		return bm;
	}

}
