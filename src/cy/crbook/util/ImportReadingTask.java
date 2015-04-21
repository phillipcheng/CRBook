package cy.crbook.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

import com.dropbox.client2.DropboxAPI.Entry;

import cy.common.entity.BPackage;
import cy.common.entity.Book;
import cy.common.entity.Page;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.common.persist.LocalPersistManager;
import cy.common.persist.RemotePersistManager;
import cy.common.util.UncloseableInputStream;
import cy.common.xml.XmlImporter;
import cy.common.xml.XmlWorker;
import cy.crbook.CRApplication;
import cy.crbook.dropbox.DropBoxCompareResult;
import cy.crbook.dropbox.DropBoxDownloadFileTask;
import cy.crbook.dropbox.DropBoxImportPostProcess;
import cy.crbook.persist.SQLitePersistManager;
import android.content.Context;
import android.util.Log;

public class ImportReadingTask extends Workable implements ComparableJob{

	private static final String TAG = "ImportReadingThread";
	Context context;
	
	List<Entry> entries;
	List<? extends Reading> readings;
	
	
	boolean isCancelled=false;
	DropBoxImportPostProcess ipp;
	
	SQLitePersistManager localPersistManager;
	
	public void cancel(){
		isCancelled=true;
	}
	
	public ImportReadingTask(String taskName, String taskParam,
			Context context, SQLitePersistManager pService) {
		super(taskName, taskParam, context);
		this.context = context;
		this.localPersistManager = pService;
	}

	public ImportReadingTask(String taskName, String taskParam,
			Context context, List<Entry> entries, DropBoxImportPostProcess ipp, SQLitePersistManager pService) {
		super(taskName, taskParam, context);
		this.context = context;
		this.entries = entries;
		this.ipp = ipp;
		this.localPersistManager = pService;
	}
	
	public ImportReadingTask(String taskName, String taskParam,
			Context context, SQLitePersistManager pService, List<? extends Reading> readings) {
		super(taskName, taskParam, context);
		this.context = context;
		this.readings = readings;
		this.localPersistManager = pService;
	}
	
	@Override
	public void myRun() {
		
		if (entries != null){
			for (Entry e: entries){
				importFromEntry(e, localPersistManager);
			}
		}
		
		if (readings != null){
			for (Reading r : readings){
				localPersistManager.insertOrUpdateIfNew(r);
			}
		}
		
		if (ipp!=null)
			ipp.importPostProcess(entries);
	}
	
	private boolean importFromZip(File f, long totalBytes, SQLitePersistManager localPersistManager){
		long bytesProcessed=0;
		try {
	        ZipInputStream zin = new ZipInputStream(new FileInputStream(f));
	        InputStream unclosable = new UncloseableInputStream(zin);
	        try {
	            ZipEntry ze = null;
	            while ((ze = zin.getNextEntry()) != null && !isCancelled) {
            		Log.i(TAG,"entry:"+ze.getName());
            		bytesProcessed += ze.getCompressedSize();
            		updateProgress("Processed:" + bytesProcessed + "/" + totalBytes);
	            	if (ze.isDirectory()){
	            		//
	            	}else {
	            		if (ze.getName().endsWith(XmlWorker.BOOK_SUFFIX_1)){
		            		Book b = new Book();
		    				List<Page> pages = new ArrayList<Page>();
		    				try{
		    					XmlWorker.readBookXml(b, pages, unclosable);
		    					localPersistManager.createBookAndPages(b, pages);
		    				}catch(Exception e){
		    					Log.e(TAG, "error when import:", e);
		    				}
		                }else if (ze.getName().endsWith(XmlWorker.VOL_SUFFIX_1)){
		                	Volume c= new Volume();
		    				try{
		    					XmlWorker.readVolumeXml(c, unclosable);
		    					localPersistManager.insertOrUpdateVolume(c);
		    				}catch(Exception e){
		    					Log.e(TAG, "error when import:", e);
		    				}
		                }else if (ze.getName().endsWith(XmlWorker.CSV_SUFFIX_1)){
		                	XmlImporter.importEntitiesFromCSV(unclosable, localPersistManager);
		                }else{
		                	Log.e(TAG, "unknown type:" + ze.getName());
		                }
	            	}
	            	//zin.closeEntry();
                }	
	            return !isCancelled;
	            //
	        }
	        finally {
	            zin.close();
	        }
		}catch(Exception e){
	    	Log.e(TAG, "Unzip exception", e);
	    	return false;
	    }
	}

	//dir: crbook, crvol, zip
	//dir: directory containing crbook and crvol
	protected void importFromEntry(Entry entry, SQLitePersistManager pService){
		File f = new File(DropBoxDownloadFileTask.getDownloadFile(entry));
		if (importFromZip(f, entry.bytes, pService)){
			BPackage pkg = DropBoxCompareResult.convert(entry);
			pService.addPackage(pkg);
		}
	}
	
	@Override
	public int compareTo(ComparableJob another) {
		return CRApplication.PRIORITY_IMPORT_HIGH-another.getPriority();
	}

	@Override
	public int getPriority() {
		return CRApplication.PRIORITY_IMPORT_HIGH;
	}
}
