package cy.crbook;

import java.util.Collection;
import java.util.List;
import java.util.Stack;
import cy.common.entity.Book;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.common.persist.LocalPersistManager;
import cy.crbook.util.DownloadImageJobManager;
import cy.readall.R;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

class StackItem{
	StackItem(Volume vol, int pos, Collection<Reading> readingList, int currentPage){
		this.vol = vol;
		this.position = pos;
		this.readingList = readingList;
		this.currentPage = currentPage;
	}
	Volume vol; //vol can be null means rootCats
	int position; //position of the grid view
	Collection<Reading> readingList;
	int currentPage; //the page number of search result
}

public class ReadingBrowseFragment extends Fragment implements OnItemClickListener {
	private static final String TAG = "ReadingBrowserFragment";
    
	private int fragId;
	private int type=-1;
	private AbsListView absListView;
	private ReadingGridViewAdapter readingGridAdapter;
	//rootCatId can be an array of catId
	private String[] rootCatsIds;
	Stack<StackItem> catStack = new Stack<StackItem>();
	//when curVol is null, meaning current vol is rootCats, can be an array of rootId
	private Volume curVol;
	
	private static final String BUNDLE_KEY_CURRENT_VOLUME="CurrentVolume";
	private static final String BUNDLE_KEY_FRAG_ID="FragmentId";
	private static final String BUNDLE_KEY_ROOT_VOLUME_IDS="RootVolumeIds";
    
	EditText et;
	//
	EditText curSRPNTxt;//current search result page number
	int curSRPN=0;
	//
	TextView tvTotalPage;
	int totalPage=1;
	CRApplication myApp;
	
	public static final ReadingBrowseFragment newInstance(
			int fragId, String[] rootCatsIds, CRApplication myApp)
	{
		ReadingBrowseFragment fragment = new ReadingBrowseFragment(myApp);
	    Bundle bundle = new Bundle();
	    bundle.putInt(BUNDLE_KEY_FRAG_ID, fragId);
	    bundle.putStringArray(BUNDLE_KEY_ROOT_VOLUME_IDS, rootCatsIds);
	    fragment.setArguments(bundle);
	    return fragment;
	}
	
	public ReadingBrowseFragment(CRApplication myApp){
		this.myApp = myApp;
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		if (savedInstanceState==null){
			Log.i(TAG, "no saved instance found, using argument.");
			fragId = getArguments().getInt(BUNDLE_KEY_FRAG_ID);
			type = ReadingBrowserActivity.getTypeFromFragId(fragId);
			rootCatsIds = getArguments().getStringArray(BUNDLE_KEY_ROOT_VOLUME_IDS);
		}else{
			Log.i(TAG, "using saved instance.");
			fragId = savedInstanceState.getInt(BUNDLE_KEY_FRAG_ID);
			type = ReadingBrowserActivity.getTypeFromFragId(fragId);
			rootCatsIds = savedInstanceState.getStringArray(BUNDLE_KEY_ROOT_VOLUME_IDS);
			String volJSon = savedInstanceState.getString(BUNDLE_KEY_CURRENT_VOLUME);
			if (volJSon!=null){
				curVol = new Volume();
				curVol.fromTopJSONString(volJSon);
			}
		}
		
        View rootView = null;
        if (fragId==ReadingBrowserActivity.LHH_MH_SEG){
        	rootView = inflater.inflate(R.layout.book_grid, container, false);
        }else if (fragId == ReadingBrowserActivity.XS_SEG){
        	rootView = inflater.inflate(R.layout.reading_list, container, false);
        }else{
        	Log.e(TAG, "unsupported fragId:" + fragId);
        }
        
        et = (EditText)rootView.findViewById(R.id.searchText);
		curSRPNTxt = (EditText)rootView.findViewById(R.id.pageNum);
		curSRPNTxt.setText(curSRPN+"", TextView.BufferType.EDITABLE);
		Button searchButton = (Button)rootView.findViewById(R.id.searchButton);
		searchButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				int pn=0;
				try{
					pn = Integer.parseInt(curSRPNTxt.getText().toString());
					absListView.smoothScrollToPosition(0);
				}catch(Exception e){
					Log.e(TAG, "", e);
				}
				curSRPN=pn;
				refreshByName(et.getText().toString(), -1);
			}
		});
		Button prevButton = (Button)rootView.findViewById(R.id.prevButton);
		prevButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				curSRPN--;
				if (curSRPN>=-1){
					refreshByName(et.getText().toString(), -1);
					curSRPNTxt.setText(curSRPN+"", TextView.BufferType.EDITABLE);
					absListView.smoothScrollToPosition(0);
				}else{
					curSRPN++;
				}
			}
		});
		Button nextButton = (Button)rootView.findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				curSRPN++;
				refreshByName(et.getText().toString(), -1);
				curSRPNTxt.setText(curSRPN+"", TextView.BufferType.EDITABLE);
				absListView.smoothScrollToPosition(0);
			}
		});
		
		tvTotalPage = (TextView)rootView.findViewById(R.id.totalPageNum);
		tvTotalPage.setText(totalPage+"", TextView.BufferType.NORMAL);
		
		if (fragId==ReadingBrowserActivity.LHH_MH_SEG){
			absListView = (GridView) rootView.findViewById(R.id.gridView1);
			readingGridAdapter = new ReadingGridViewAdapter(this.getActivity(), R.layout.book_thumbnail);
        }else if (fragId == ReadingBrowserActivity.XS_SEG){
        	absListView = (ListView) rootView.findViewById(R.id.listView1);
        	readingGridAdapter = new ReadingGridViewAdapter(this.getActivity(), R.layout.reading_row);
        }else{
        	Log.e(TAG, "unsupported fragId:" + fragId);
        }
		
		readingGridAdapter.setRBFrag(this);
		absListView.setAdapter(readingGridAdapter);
		absListView.setOnItemClickListener(this); 

        return rootView;
    }
	
	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putInt(BUNDLE_KEY_FRAG_ID, fragId);
		bundle.putStringArray(BUNDLE_KEY_ROOT_VOLUME_IDS, rootCatsIds);
		if (curVol!=null){
			bundle.putString(BUNDLE_KEY_CURRENT_VOLUME, curVol.toTopJSONString());
		}
	}
	
	/**
	 * the page number is read from the curPageText
	 * @param name: search name
	 * @param pos: scroll to which position
	 */
	private void refreshByName(String name, int pos){
		if (!"".equals(name)){
			readingGridAdapter.asyncSearchByName(fragId, name, curSRPN);
		}else{
			if (curVol==null){
				readingGridAdapter.asyncSearchByCat(fragId, this.rootCatsIds, curSRPN);
			}else{
				readingGridAdapter.asyncSearchByCat(fragId, new String[]{curVol.getId()}, curSRPN);
			}
		}
		if (pos>=0)
			absListView.smoothScrollToPosition(pos);
	}

	private void refreshByCat(String[] catIds, int pos){
		readingGridAdapter.asyncSearchByCat(fragId, catIds, curSRPN);
		if (pos>=0)
			absListView.smoothScrollToPosition(pos);
	}
	
	private void refresh(Collection<Reading> readingList, int pos){
		readingGridAdapter.clear();
		readingGridAdapter.setReadingList(readingList);
		if (pos>=0)
			absListView.smoothScrollToPosition(pos);
	}

	private void refreshByCat(String cat){
		readingGridAdapter.asyncSearchByCat(fragId, new String[]{cat}, curSRPN);
	}
	
	private void refresh(int pos){
		if ("".equals(et.getText().toString())){
			if (curVol!=null){
				refreshByCat(new String[]{curVol.getId()}, pos);
			}else{
				refreshByCat(this.rootCatsIds, pos);
			}
		}else{
			refreshByName(et.getText().toString(), pos);
		}
	}
	
	@Override
	public void onStart(){
		super.onStart();
		if (curVol==null)
			curVol=Volume.ROOT_VOLUMES.get(rootCatsIds);	
		refresh(-1);
	}
	
	@Override
    public void onItemClick(AdapterView parent, View v, int position, long id){
		//cancel existing download tasks
    	DownloadImageJobManager.singleton.cancelAll();
    	
		Intent intent = new Intent();
		//remember current volume name and position
		StackItem si = new StackItem(curVol, position, readingGridAdapter.getReadingList(), this.curSRPN);
		this.catStack.push(si);
		Reading r = (Reading)readingGridAdapter.getItem(position);
		if (r instanceof Book){
			Book b = (Book)readingGridAdapter.getItem(position);
			if (b.getLastPage()==0){
				b.setLastPage(1);
			}
			if (b.getType() == Reading.TYPE_PIC){
				intent.putExtra(CRBookIntents.EXTRA_SAVED_BOOK, b.toTopJSONString());
				intent.setAction(CRBookIntents.ACTION_OPEN_BOOK);				
			}else if (b.getType()==Reading.TYPE_NOVEL){
				intent.putExtra(CRBookIntents.EXTRA_SAVED_XPATH, myApp.getTemplate(b.getId()).getContentXPath());
				intent.putExtra(CRBookIntents.EXTRA_SAVED_BOOK, b.toTopJSONString());
				intent.setAction(CRBookIntents.ACTION_OPEN_HTMLPAGE);				
			}else{
				Log.e(TAG, "unsupported book type:" + b.getType());
			}
			startActivity(intent);
		}else if (r instanceof Volume){
			Volume c = (Volume)readingGridAdapter.getItem(position);
			curVol = c;
			setCurrentPage(0);
			refreshByCat(curVol.getId());
		}
	}

	int pressTime=0;
	static int Total_Press_Allowed=2;
	
	public void backPressed() {
		Log.i(TAG, "back pressed:" + curVol + ", pressTime:" + pressTime);
		if (curVol==null || catStack.isEmpty()){
			//curVol=Volume.getRootVolume(rootCatId);
			refreshByCat(rootCatsIds, 0);
			//already root volume
			if (pressTime<Total_Press_Allowed){
				pressTime++;
				String msg = "press " + (Total_Press_Allowed-pressTime) + "  more, app will quit";
				if (getActivity()!=null){
					Toast success = Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG);
	                success.show();
				}else{
					pressTime--;
				}
			}else{
				CRApplication myApp = (CRApplication) this.getActivity().getApplication();
				myApp.quit();
			}
		}else{
			//cancel existing download tasks
        	DownloadImageJobManager.singleton.cancelAll();
        	//
	        StackItem si = catStack.pop();
	        curVol = si.vol;
		    setCurrentPage(si.currentPage);
	        refresh(si.readingList, si.position);
		    pressTime=0;
		}	
	}
	

	public int getFragId(){
		return fragId;
	}
	public int getType() {
		return type;
	}

	public void setRootCatId(String[] rootCatsIds){
		this.rootCatsIds = rootCatsIds;
	}
	public String[] getRootCatId(){
		return rootCatsIds;
	}
	public ReadingGridViewAdapter getReadingGridAdapter(){
		return readingGridAdapter;
	}
	public void setCurrentPage(int page){
		curSRPN = page;
		curSRPNTxt.setText(""+page);
	}
	public void setTotalPage(int tp){
		this.totalPage = tp;
		tvTotalPage.setText(tp+"");
	}
}
