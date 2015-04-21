package cy.crbook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.cld.util.CompareUtil;
import org.cld.util.StringUtil;

import cy.common.entity.Book;
import cy.common.entity.EntityUtil;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.crbook.picbook.ThumbNailRecordHolder;
import cy.crbook.textbook.TextRowRecordHolder;
import cy.readall.R;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Toast;

public class ReadingGridViewAdapter extends ArrayAdapter<Reading> {
	
	private static final String TAG = "ReadingGridViewAdapter";
	
	private Activity activity;
	private CRApplication myApp;
	private int layoutResourceId;
	
	//cache the empty_cover bmp
	public static Bitmap empty_cover = null;
	
	NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	
	//for selection
	Map<String, Reading> items = new ConcurrentHashMap<String, Reading>();
	Set<Reading> selected = new ConcurrentSkipListSet<Reading>();
	private ReadingBrowseFragment rbFrag;
	private Collection<Reading> readingList;

	
	public ReadingGridViewAdapter(Activity context, int layoutResourceId) {
		super(context, layoutResourceId);
		this.layoutResourceId = layoutResourceId;
		this.activity = context;
		this.myApp = (CRApplication) context.getApplication();
		if (empty_cover == null)
			empty_cover = BitmapFactory.decodeResource(context.getResources(), R.drawable.empty_cover);
	}
	
	public void setRBFrag(ReadingBrowseFragment rbFrag){
		this.rbFrag = rbFrag;
	}
	
	private int totalPages;
	
	public Collection<Reading> getReadingList(){
		return readingList;
	}
	
	//init function
	public void setReadingList(Collection<Reading> data){
		this.clear();
		this.addAll(data);
		this.readingList = data;
		if (rbFrag!=null){
			rbFrag.setTotalPage(totalPages);
		}
	}
	
	public Set<Reading> getSelectedReadings(){
		return selected;
	}
	
	class RemoteFetch extends AsyncTask<Integer, Void, Collection<Reading>> {
		int fragId;
		String searchTxt;
		String[] searchCats;
		
		RemoteFetch(int fragId, String searchTxt, String[] searchCats){
			this.fragId = fragId;
			this.searchTxt = searchTxt;
			this.searchCats=searchCats;
		}
		
        @Override
        protected Collection<Reading> doInBackground(Integer... params) {
        	if (null!=searchTxt && !"".equals(searchTxt)){
        		return syncSearchByName(fragId, searchTxt, params[0]);
        	}else{
        		return syncSearchByCats(fragId, searchCats, params[0]);
        	}
        }

        @Override
        protected void onPostExecute(Collection<Reading> result) {
        	if (result!=null)
        		setReadingList(result);
        	
    		if (myApp.isMyReadingMode() && 
    				(myApp.getUserid()==null||"".equals(myApp.getUserid()))){
        		activity.runOnUiThread(new Runnable(){
					@Override
					public void run(){
						Toast t = Toast.makeText(activity, "Please signin.", Toast.LENGTH_LONG);
						t.show();
					}
				});
    		}
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
	
	private void asyncSearch(int fragId, String searchTxt, String[] searchCats, int range){
		(new RemoteFetch(fragId, searchTxt, searchCats)).execute(range);
	}
	
	public void asyncSearchByName(int fragId, String name, int range){
		asyncSearch(fragId, name, null, range);
	}
	public void asyncSearchByCat(int fragId, String[] catIds, int range){
		asyncSearch(fragId, null, catIds, range);
	}
	
	class LimitOffset{
		int limit;
		int offset;
		LimitOffset(int offset, int limit){
			this.offset=offset;
			this.limit=limit;
		}
	}
	private LimitOffset getLimitOffset(int pageNum){
		if (pageNum<0){
			return new LimitOffset(-1,0);
		}else{
			return new LimitOffset(pageNum*myApp.getPageSize(), myApp.getPageSize());
		}
	}
	
	private void setTotalPage(long totalItems){
		int leftItems = (int) (totalItems % myApp.getPageSize());
		if (leftItems == 0){
			totalPages = (int) (totalItems/myApp.getPageSize());
		}else{
			totalPages = (int) (totalItems/myApp.getPageSize())+1;
		}
	}
	
	//pageNum starting from 0, -1 means all
	private Collection<Reading> syncSearchByName(int fragId, String searchTxt, int pageNum){
		LimitOffset los = getLimitOffset(pageNum);
		if (myApp.isMyReadingMode()){
			List<Reading> rl=new ArrayList<Reading>();
			long totalItemsCount=0;
			if (myApp.getLocalMode()){
				rl = myApp.getLocalPersistManager().getMyReadingsLike(searchTxt, los.offset, los.limit);
				totalItemsCount = myApp.getLocalPersistManager().getMyReadingsCountLike(searchTxt);
				setTotalPage(totalItemsCount);
			}else{
				//check user id
				if (myApp.getUserid()==null || "".equals(myApp.getUserid())){
					return null; //tell user not login yet.
				}else{
					List<Volume> cats = myApp.getRemotePersistManager().getVolumesLike(searchTxt, 
							myApp.getUserid(), rbFrag.getType(), los.offset, los.limit);
					List<Book> books = myApp.getRemotePersistManager().getBooksByName(searchTxt, 
							myApp.getUserid(), rbFrag.getType(), los.offset, los.limit);
					rl.addAll(cats);
					rl.addAll(books);
					if (pageNum==0){
						long t1 = myApp.getRemotePersistManager().getVCLike(searchTxt, 
								myApp.getUserid(), rbFrag.getType());
						long t2 = myApp.getRemotePersistManager().getBCByName(searchTxt, 
								myApp.getUserid(), rbFrag.getType());
						totalItemsCount = t1+t2;
						setTotalPage(totalItemsCount);
					}
				}
			}
			return rl;
		}else{
			long totalItemsCount=0;
			if (!myApp.getLocalMode()){
				List<Volume> cats = myApp.getRemotePersistManager().getVolumesLike(searchTxt, "", 
						rbFrag.getType(), los.offset, los.limit);
				List<Book> books = myApp.getRemotePersistManager().getBooksByName(searchTxt, "", 
						rbFrag.getType(), los.offset, los.limit);
				List<Reading> rl = new ArrayList<Reading>();
				rl.addAll(cats);
				rl.addAll(books);
				if (pageNum==0){
					long t1 = myApp.getRemotePersistManager().getVCLike(searchTxt, "",
							rbFrag.getType());
					long t2 = myApp.getRemotePersistManager().getBCByName(searchTxt, "",
							rbFrag.getType());
					totalItemsCount = t1+t2;
					setTotalPage(totalItemsCount);
				}
				return rl;
			}else{
				List<Reading> ll = new ArrayList<Reading>();
				List<Volume> lcats = myApp.getLocalPersistManager().getVolumesLike(searchTxt, los.offset, los.limit);
				List<Book> lbooks = myApp.getLocalPersistManager().getBooksByName(searchTxt, los.offset, los.limit);
				
				ll.addAll(lcats);
				ll.addAll(lbooks);	
				
				if (pageNum==0){
					long t1 = myApp.getLocalPersistManager().getVCLike(searchTxt);
					long t2 = myApp.getLocalPersistManager().getBCByName(searchTxt);
					totalItemsCount = t1+t2;	
					setTotalPage(totalItemsCount);
				}
				return ll;
			}
		}
	}
	
	private Collection<Reading> syncSearchByCats(int fragId, String[] searchCats, int pageNum){
		//if there is a specific cat to list/refresh, there is no difference between my reading and all reading
		
		LimitOffset los = getLimitOffset(pageNum);
		List<Reading> rl = new ArrayList<Reading>();
		long totalItemsCount=0;
		if (myApp.isMyReadingMode() && (Arrays.deepEquals(searchCats, rbFrag.getRootCatId()))){
			//no cat specified for my readings, we list all my readings
			return syncSearchByName(fragId, "", pageNum);
		}else{
			//expand searchCats to String
			String txtSearchCats;
			if (searchCats == null){
				searchCats = rbFrag.getRootCatId();
			}
			txtSearchCats = StringUtil.toStringList(searchCats);
			
			if (!myApp.getLocalMode()){
				List<Volume> cats = myApp.getRemotePersistManager().getVolumesByPCat(txtSearchCats, los.offset, los.limit);
				List<Book> books = myApp.getRemotePersistManager().getBooksByCat(txtSearchCats, los.offset, los.limit);
				rl = new ArrayList<Reading>();
				if (cats!=null)
					rl.addAll(cats);
				if (books!=null)
					rl.addAll(books);
				
				if (pageNum==0){
					long t1 = myApp.getRemotePersistManager().getVCByPCat(txtSearchCats);
					long t2 = myApp.getRemotePersistManager().getBCByCat(txtSearchCats);
					totalItemsCount = t1+t2;
					setTotalPage(totalItemsCount);
				}
				return rl;
			}else{
				List<Volume> lcats = myApp.getLocalPersistManager().getVolumesByPCat(searchCats, los.offset, los.limit);
				List<Book> lbooks = myApp.getLocalPersistManager().getBooksByCat(searchCats, los.offset, los.limit);
				rl.addAll(lcats);
				rl.addAll(lbooks);
				
				if (pageNum==0){
					long t1 = myApp.getLocalPersistManager().getVCByPCat(searchCats);
					long t2 = myApp.getLocalPersistManager().getBCByCat(searchCats);
					totalItemsCount = t1+t2;	
					setTotalPage(totalItemsCount);
				}
				return rl;
			}
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		RecordHolder holder = null;
		Reading r = getItem(position);
		
		if (row == null || position==0 || (((RecordHolder)row.getTag()).getPosition()==0)) {
			//special treatment for 0, no cache, and no reuse of position 0
			LayoutInflater inflater = ((Activity) activity).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);

			CheckBox chkbox = (CheckBox) row.findViewById(R.id.item_chkbx);
			if (rbFrag.getFragId()==ReadingBrowserActivity.LHH_MH_SEG){
				holder = new ThumbNailRecordHolder(items, chkbox, position, row, r, activity);
			}else if (rbFrag.getFragId()==ReadingBrowserActivity.XS_SEG){
				holder = new TextRowRecordHolder(items,chkbox,position,row,r);
			}
			
			holder.chkbox.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					CheckBox cb = (CheckBox)v;
					if (cb.isChecked()){
						Log.i(TAG, "items:" + items);
						selected.add(items.get(cb.getText()));
						Log.i(TAG, "selected:" + selected);
					}else{
						Log.i(TAG, "items:" + items);
						selected.remove(items.get(cb.getText()));
						Log.i(TAG, "selected:" + selected);
					}				
				}
			});
			//make it to load content for every new row view.
			holder.r = null;
		} else {
			holder = (RecordHolder) row.getTag();
		}
		
		
		if (CompareUtil.ObjectDiffers(r, holder.r)){
			if (selected.contains(r)){
				if (!holder.chkbox.isChecked()){
					holder.chkbox.setChecked(true);
				}
			}else{
				if (holder.chkbox.isChecked()){
					holder.chkbox.setChecked(false);
				}
			}
			holder.loadContent(r, position);
			row.setTag(holder);
		}
		Log.i(TAG, "get view:" + holder.toString());
		return row;
	}
}