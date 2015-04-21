package cy.crbook.picbook;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cld.util.StringUtil;

import android.content.Context;
import android.os.AsyncTask;

import cy.cfs.CallbackOp;
import cy.common.entity.Book;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.common.persist.LocalPersistManager;
import cy.common.persist.RemotePersistManager;
import cy.crbook.CRApplication;
import cy.crbook.FileCache;
import cy.crbook.persist.SQLitePersistManager;
import cy.crbook.util.DownloadImageJobManager;
import cy.crbook.wsclient.RestClientPersistManager;

class BookDownloadStatus{
	List<Integer> sucessPages;
	List<Integer> failPages;
	
	public BookDownloadStatus(){
		sucessPages = new ArrayList<Integer>();
		failPages = new ArrayList<Integer>();
	}
}

public class BookDownload implements CallbackOp{
	private CRApplication myApp;
	
	public BookDownload(CRApplication myApp){
		this.myApp = myApp;
	}
	
	private Map<String, BookDownloadStatus> downloadStatus = new HashMap<String, BookDownloadStatus>();//success and failure
	
	private void downloadBook(Book b){
		downloadStatus.put(b.getId(), new BookDownloadStatus());
		int width=2048;
		int height=2048;
		for (int i=1; i<=b.getTotalPage(); i++){
			String url =myApp.getLocalPersistManager().getPageBgUrl(b, i);
			if (url!=null){
				String fileKey = FileCache.generateKey(b, i);
				File f = new File(fileKey);
				if (!f.exists()){
					DownloadImageJobManager.singleton.submitDownloadImageJob(
							this, new BgImgParam(i, b.getId()), myApp, 
							b.getId(), fileKey, width, height, url, 
							myApp.getTemplate(b.getId()).getReferer(), false);
				}
			}
		}
	}
	
	private void downloadVolume(Volume v){
		String[] vidlist = new String[]{v.getId()};
		
		List<Book> allBList;
		List<Volume> allVList;
		
		List<String> vid = new ArrayList<String>();
		vid.add(v.getId());
		if (!myApp.getLocalMode()){
			List<Book> blist;
			allBList = new ArrayList<Book>();
			int offset=0;
			int limit=50;
			do{
				 blist = myApp.getRemotePersistManager().getBooksByCat(
						 StringUtil.toStringList(vidlist), offset, offset+limit);
				 allBList.addAll(blist);
				 offset+=limit;
			}while (blist.size()>=limit);
			
			List<Volume> vlist;
			allVList = new ArrayList<Volume>();
			offset=0;
			limit=50;
			do{
				vlist = myApp.getRemotePersistManager().getVolumesByPCat(
						StringUtil.toStringList(vidlist), offset, offset+limit);
				allVList.addAll(vlist);
				offset+=limit;
			}while (vlist.size()>=limit);
		}else{
			allBList = myApp.getLocalPersistManager().getBooksByCat(vidlist, -1, 0);
			allVList = myApp.getLocalPersistManager().getVolumesByPCat(vidlist, -1, 0);
		}
		
		downloadAllSync(allVList);
		downloadAllSync(allBList);
	}
	
	private void downloadAllSync(Collection<? extends Reading> rl){
		for (Reading r: rl){
			if (r instanceof Book){
				downloadBook((Book)r);
			}else{
				downloadVolume((Volume)r);
			}
		}
	}
	
	public void downloadAllASync(Collection<Reading> rl){
		(new AsyncTask<Collection<Reading>, Void, Void>(){

			@Override
			protected Void doInBackground(Collection<Reading>... params) {
				downloadAllSync(params[0]);
				return null;
			}
		}).execute(rl);
	}

	@Override
	public void onSuccess(Object request, Object result) {
		BgImgParam req = (BgImgParam)request;
		BookDownloadStatus bdls = downloadStatus.get(req.bookId);
		bdls.sucessPages.add(req.pageNum);
	}

	@Override
	public void onFailure(Object request, Object result) {
		BgImgParam req = (BgImgParam)request;
		BookDownloadStatus bdls = downloadStatus.get(req.bookId);
		bdls.failPages.add(req.pageNum);	
	}
}
