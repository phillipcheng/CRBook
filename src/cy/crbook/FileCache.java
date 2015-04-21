package cy.crbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

import org.cld.util.StringUtil;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import cy.common.entity.Book;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.crbook.util.CRBookUtils;

import cy.cfs.CFS;

public class FileCache {

	//extStorageDir.getPath() + / + FILE_CACHE_ROOT+ / + catPath + / + filename.suffix
	
	public static final String TAG="FileCache";
	
	private static String FILE_CACHE_ROOT = "Books/cache";
	public static final String img_suffix="jpg";
	
	public static HashMap<String, String> fileKeyMap = new HashMap<String, String>();
	
	public static void setCacheRoot(String cacheRoot){
		FILE_CACHE_ROOT = cacheRoot;
	}
	public static String getCacheRoot(){
		File extStorageDir = Environment.getExternalStorageDirectory();
		return extStorageDir.getPath() + File.separatorChar + FILE_CACHE_ROOT;
	}
	/**
	 * @param r: volume or book can't be null
	 * @param pageNum : the page number, -1 for cover
	 * @return the key
	 */
	public static String generateKey(Reading r, int pageNum){
		//gen reading key for mem-cache
		String rKey ="";
		//high priority should be given to page background, not book cover
		if (r instanceof Volume){
			rKey = r.getCat() + "|" + r.getId();
		}else{
			rKey = r.getId() + "|" + pageNum; 
		}
		
		if (fileKeyMap.containsKey(rKey)){
			return fileKeyMap.get(rKey);
		}
		
		String fileKey;
		
		File extStorageDir = Environment.getExternalStorageDirectory();
		//catPath includes current r's id
		String catPath = null;
		catPath = CRBookUtils.getBookCachePath(r);
		if (pageNum>-1){//for page not cover		
			//page, the (pageNum + 1).jpg under the book folder
			fileKey = extStorageDir.getPath() + File.separatorChar + FILE_CACHE_ROOT+ File.separatorChar + catPath + 
					File.separatorChar + StringUtil.getStringFromNum(pageNum+1, 3) + "." + img_suffix;
		}else{
			//for cover
			if (r instanceof Volume){
				//for volume, cover.jpg under volume folder
				fileKey = extStorageDir.getPath() + File.separatorChar + FILE_CACHE_ROOT+ File.separatorChar + catPath + 
						File.separatorChar + "cover." + img_suffix;
			}else{
				//for book, the 000.jpg under the book folder, to make sure not conflict with page images
				fileKey = extStorageDir.getPath() + File.separatorChar + FILE_CACHE_ROOT+ File.separatorChar + catPath + 
						File.separatorChar + "000." + img_suffix;
			}	
		}
		
		fileKeyMap.put(rKey, fileKey);
		return fileKey;
	}
	
	public static void saveFile(Bitmap bmp, String fileKey){
		FileOutputStream out=null;
		try {
			File f = new File(fileKey);
			File p = f.getParentFile();
			if (!p.exists()){
				p.mkdirs();
			}
			out = new FileOutputStream(fileKey);
			bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
		} catch (Exception e) {
		    Log.e(TAG, "not able to write:" + fileKey, e);
		} finally {
	       try{
	    	   if (out!=null)
	    		   out.close();
	       } catch(Throwable ignore) {
	    	   
	       }
		}
	}
	
	public static void saveCloud(Bitmap bmp, String fileKey, CRApplication myApp){
		Log.i(TAG, fileKey + " to save to cloud.");
		fileKey = fileKey.substring(FileCache.getCacheRoot().length());
		CFS.asyncSaveImageFile(myApp.getUserid(), fileKey, bmp, null, myApp);
	}
	
	
	
	public static int getCachedPages(Book b){
		String catPath = null;
		catPath = CRBookUtils.getBookCachePath(b);
		String dir = getCacheRoot() + File.separatorChar + catPath;
		File bookDir = new File(dir);
		if (bookDir.exists() && bookDir.isDirectory()){
			return bookDir.list().length;
		}else{
			return 0;
		}
	}
}
