package cy.crbook.picbook;

import java.io.File;
import java.util.Map;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.graphics.drawable.BitmapDrawable;
import cy.cfs.CallbackOp;
import cy.common.entity.Book;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.crbook.CRApplication;
import cy.crbook.FileCache;
import cy.crbook.ReadingGridViewAdapter;
import cy.crbook.RecordHolder;
import cy.crbook.util.DownloadImageJobManager;
import cy.crbook.util.ImageUtil;
import cy.readall.R;

public class ThumbNailRecordHolder extends RecordHolder implements CallbackOp{

	private static final String TAG = "ThumbNailRecordHolder";
	ImageView imageItem; //the image item view
	ImageView overlayRead;
	ImageView overlayDownload;
	ImageView overlayVolume;
	ImageView stateImg;
	
	Activity activity;
	
	public ThumbNailRecordHolder(Map<String, Reading> items, CheckBox chkbox, int position, View row, Reading r, Activity activity){
		super(items, chkbox, position, r);
		
		imageItem = (ImageView) row.findViewById(R.id.item_image);
		overlayRead = (ImageView) row.findViewById(R.id.readImage);
		overlayDownload = (ImageView) row.findViewById(R.id.downloadImage);		
		overlayVolume = (ImageView) row.findViewById(R.id.bookVolumeImage);	
		stateImg = (ImageView) row.findViewById(R.id.stateImage);
		
		this.activity = activity;
	}
	
	public String toString(){
		String ret = super.toString();
		//ret+=", imageItem:" + ((BitmapDrawable)imageItem.getDrawable()).getBitmap();
		//ret+=", overlayVolume:"+ overlayVolume.getVisibility();
		return ret;
	}
	
	public void loadContent(Reading r, int position){
		CRApplication myApp = (CRApplication) activity.getApplication();
		super.loadContent(r, position);
		
		String name = "";
		if (r instanceof Volume){
			name = r.getName() + "(" + ((Volume)r).getBookNum() + ")";
			name = name + "-" + ((Volume)r).getAuthor();
			overlayRead.setVisibility(View.INVISIBLE);
			overlayDownload.setVisibility(View.INVISIBLE);
			overlayVolume.setVisibility(View.VISIBLE);
		}else if (r instanceof Book){
			name = r.getName() + "(" + ((Book)r).getTotalPage() + ")";
			Book b = (Book)r;
			if (b.getRead()>0){
				overlayRead.setVisibility(View.VISIBLE);
			}else{
				overlayRead.setVisibility(View.GONE);
			}
			int downloadedPages = FileCache.getCachedPages(b);
			if (Math.abs(downloadedPages-b.getTotalPage())<3){
				overlayDownload.setVisibility(View.VISIBLE);
			}else{
				overlayDownload.setVisibility(View.GONE);
			}
			overlayVolume.setVisibility(View.GONE);
		}
		setCheckBoxName(chkbox, name, r);
		Bitmap bm = null;
		
		if (r.getState()==Reading.STATE_ONLINE){
			stateImg.setImageResource(R.drawable.cloud);
		}else if (r.getState()==Reading.STATE_OFFLINE){
			stateImg.setImageResource(R.drawable.pad);
		}else if (r.getState()==Reading.STATE_BOTH){
			stateImg.setImageResource(R.drawable.sync);
		}

		//for key generation
		int pageNum=-1;//for volume
		Book b=null;
		
		String coverUri = r.getCoverUri();
		if (coverUri==null || coverUri.equals("")){
			if (r instanceof Book){
				b = (Book)r;
				pageNum=1;
				coverUri = myApp.getLocalPersistManager().getPageBgUrl(b, pageNum);
			}
		}
		
		if (coverUri!=null && !coverUri.equals("")){	
			Bitmap bmp = ImageUtil.getImageFromUrl(coverUri, r, pageNum, this, imageItem, myApp, 
					imageItem.getLayoutParams().width, imageItem.getLayoutParams().height, false);
			if (bmp==null){
				imageItem.setImageBitmap(ReadingGridViewAdapter.empty_cover);
			}
		}else{
			imageItem.setImageBitmap(ReadingGridViewAdapter.empty_cover);
		}
		
		if (r.getFullPath()!=null){
			String curName=chkbox.getText().toString();
			int idx = curName.lastIndexOf("(");
			if (idx!=-1){
				String suffix=curName.substring(idx, curName.length());
				String fullName = r.getFullPath() + suffix;
				setCheckBoxName(chkbox, fullName, r);
			}else{
				setCheckBoxName(chkbox, r.getFullPath(), r);
			}
		}
	}
	
	@Override
	public void onSuccess(Object ppParam, final Object bmp) {
		//ignore the key
		final ImageView iv = (ImageView) ppParam;
		
		//this item is still in display, update it's image
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run(){
				try{
					iv.setImageBitmap((Bitmap)bmp);	
				}catch(Throwable t){
					Log.e(TAG, "", t);
				}
			}
		});
	}
	@Override 
	public void onFailure(Object request, Object response){
		
	}
}