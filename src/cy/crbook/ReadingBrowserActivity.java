package cy.crbook;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cy.common.entity.Book;
import cy.common.entity.Page;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.common.xml.XmlWorker;
import cy.crbook.persist.SQLitePersistManager;
import cy.crbook.picbook.BookDownload;
import cy.crbook.util.MyReadingPostProcess;
import cy.filedialog.FileDialog;
import cy.readall.R;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;


public class ReadingBrowserActivity extends FragmentActivity implements 
		ActionBar.TabListener, MyReadingPostProcess {

	public static final int SEG_COUNT=2;
	
	public static final int LHH_MH_SEG=0;
	public static final int XS_SEG=1;
	
	public static String[] SEG_NAMES;
	SQLitePersistManager localPersistManager = null;
	
	private int activeFragId=0;
	public int getFragId() {
		return activeFragId;
	}
	public void setFragId(int fragId){
		this.activeFragId = fragId;
	}
	
	public static int getTypeFromFragId(int fragId) {
		if (fragId==LHH_MH_SEG){
			return Volume.TYPE_PIC;
		}else if (fragId == XS_SEG){
			return Volume.TYPE_NOVEL;
		}else{
			return -1;
		}
	}

	/**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter}
     * derivative, which will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will display the three primary sections of the app, one at a
     * time.
     */
    ViewPager mViewPager;
    CRApplication myApp;

    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "on create of readingbrowseractivity is called.");
        super.onCreate(savedInstanceState);
        
        //-- start for testing wake up unlocked
        KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        mKeyGuardManager.newKeyguardLock("activity_classname").disableKeyguard();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //--- end
        myApp = (CRApplication) this.getApplication();
        localPersistManager = myApp.getLocalPersistManager();
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(
        		getSupportFragmentManager(), (CRApplication) getApplication());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        // Specify that we will be displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);
                activeFragId=position;
            }
        });

        SEG_NAMES =new String[]{
        		getResources().getString(R.string.LLH_MH_Name), 
        		getResources().getString(R.string.XS_Name),
        		getResources().getString(R.string.Self_Name)};
        
        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    	
        Log.i(TAG, "on create of readingbrowseractivity is finished.");
    	
    }

    
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
        this.setFragId(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState called.");
	}
	
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

    	ReadingBrowseFragment[] fragments = new ReadingBrowseFragment[SEG_COUNT];
    	
        public AppSectionsPagerAdapter(FragmentManager fm, CRApplication myApp) {
            super(fm);
            fragments[LHH_MH_SEG]=ReadingBrowseFragment.newInstance(LHH_MH_SEG, new String[]{
            		Volume.ROOT_VOLUME_LHH, Volume.ROOT_VOLUME_MH, Volume.ROOT_VOLUME_BANNED}, myApp);
            fragments[XS_SEG]=ReadingBrowseFragment.newInstance(XS_SEG, new String[]{
            		Volume.ROOT_VOLUME_XS}, myApp);
        }

        @Override
        public Fragment getItem(int i) {
           return fragments[i];
        }

        @Override
        public int getCount() {
            return SEG_COUNT;
        }
        
        public String getPageTitle(int i){
        	return SEG_NAMES[i];
        }
    }
    
    //prevent restart
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    public static final int IMPORT_BOOK_CONTENT = 0;
	public static final int EXIT = 1;
	public static final int ABOUT = 2;
	public static final int DEL_READING_LOCAL_INDEX = 5;
	public static final int MYREADING_MODE = 9;
	public static final int MARK_MYBOOK=10;
	public static final int UNMARK_MYBOOK=11;
	public static final int LOGIN=12;
	public static final int BATCH_SAVE=13;
    
	private static final int REQUEST_CODE_SELECT_DIR_CREATE_BOOK = 1;
	
	private static final String TAG = "ReadingBrowserActivity";
	
	private String lastPath="/";
	
	/**
     * Handles resolution callbacks.
     */
    
    @Override
	public boolean onPreparePanel(int featureId, View view, Menu menu){
    	CRApplication myApp = (CRApplication) this.getApplication();
    	Log.i(TAG,"on prepare panel called.");
    	
	    menu.clear();
	    MenuItem item =null;
	    
	    String loginMenuName = getResources().getString(R.string.login);
	    if (myApp.getLoginMenu()!=null){
	    	loginMenuName = (String) myApp.getLoginMenu().getTitle();
	    }
	    item = menu.add(0, LOGIN, 0, loginMenuName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        myApp.setLoginMenu(item);
        
        item = menu.add(0, MYREADING_MODE, 0, getResources().getString(R.string.Self_Name));
        item.setCheckable(true);
        item.setChecked(myApp.isMyReadingMode());
        
        item = menu.add(0, MARK_MYBOOK, 0, getResources().getString(R.string.MarkMyBook));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        item = menu.add(0, UNMARK_MYBOOK, 0, getResources().getString(R.string.UnMarkMyBook));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    	menu.add(0, DEL_READING_LOCAL_INDEX, 0, "delete");
    	menu.add(0, IMPORT_BOOK_CONTENT, 0, "import content");
    	menu.add(0, BATCH_SAVE, 0, "Batch Save");
    	String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;	    	
	    	menu.add(0, ABOUT,0, versionName + ":" + versionCode);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "", e);
		}    	
        menu.add(0, EXIT, 0, "Exit");
    
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		CRApplication myApp = (CRApplication) this.getApplication();
		switch (item.getItemId()){
			case LOGIN:
			{
				Intent intent = new Intent();	
				intent.setAction(CRBookIntents.ACTION_LOGIN);
				startActivity(intent);
				return true;
			}
			case IMPORT_BOOK_CONTENT:
			{
				importBookContent();
				return true;
			}
			case DEL_READING_LOCAL_INDEX:
			{
				deleteLocalReading();
				return true;
			}
			case EXIT:
			{
				myApp.quit();
				return true;
			}
			case ABOUT:
			{
				Intent intent = new Intent();	
				intent.setAction(CRBookIntents.ACTION_SETTING);
				startActivity(intent);
				return true;
			}
			case MYREADING_MODE:
			{
				boolean nextMode = !myApp.isMyReadingMode();
				if (nextMode && !myApp.getLocalMode() && 
						(myApp.getUserid()==null||"".equals(myApp.getUserid()))){
					//switch to my reading mode, currently not login and in remote mode, warning
					Toast t = Toast.makeText(this, "Please login...", Toast.LENGTH_LONG);
					t.show();
				}else{
					myApp.setMyReadingMode(!(myApp.isMyReadingMode()));
					item.setChecked(myApp.isMyReadingMode());
				}
				return true;
			}
			case MARK_MYBOOK:
			{
				ReadingBrowseFragment rbFrag = (ReadingBrowseFragment) mAppSectionsPagerAdapter.getItem(this.activeFragId);
				Set<Reading> rl = rbFrag.getReadingGridAdapter().getSelectedReadings();
				List<String> idList = new ArrayList<String>();
				for (Reading r: rl){
					idList.add(r.getId());
				}
				if (myApp.getLocalMode()){
					int ret = localPersistManager.addMyReadings(idList);
					Toast t = Toast.makeText(getBaseContext(), ret + " marked as favorate locally.", Toast.LENGTH_LONG);
					t.show();
				}else{
					if (myApp.getUserid()!=null && !"".equals(myApp.getUserid())){
						(new AsyncMyReadingProcess(myApp.getUserid(), idList, 
								AsyncMyReadingProcess.ADD_OP, this, myApp)).execute();
					}else{
						Toast t = Toast.makeText(this, "Login First..", Toast.LENGTH_LONG);
						t.show();
					}
				}
				return true;
			}
			case UNMARK_MYBOOK:
			{
				ReadingBrowseFragment rbFrag = (ReadingBrowseFragment) mAppSectionsPagerAdapter.getItem(this.activeFragId);
				Set<Reading> rl = rbFrag.getReadingGridAdapter().getSelectedReadings();
				List<String> idList = new ArrayList<String>();
				for (Reading r: rl){
					idList.add(r.getId());
				}
				if (myApp.getLocalMode()){
					int ret = localPersistManager.deleteMyReadings(idList);
					Toast t = Toast.makeText(getBaseContext(), ret + " deleted as favorate locally.", Toast.LENGTH_LONG);
					t.show();
				}else{
					if (myApp.getUserid()!=null && !"".equals(myApp.getUserid())){
						(new AsyncMyReadingProcess(myApp.getUserid(), idList, 
								AsyncMyReadingProcess.DEL_OP, this, myApp)).execute();
					}else{
						Toast t = Toast.makeText(this, "Login First..", Toast.LENGTH_LONG);
						t.show();
					}
				}
				return true;
			}
			case BATCH_SAVE:
			{
				ReadingBrowseFragment rbFrag = (ReadingBrowseFragment) mAppSectionsPagerAdapter.getItem(this.activeFragId);
				Set<Reading> rl = rbFrag.getReadingGridAdapter().getSelectedReadings();
				BookDownload bdl = new BookDownload((CRApplication) this.getApplication());
				bdl.downloadAllASync(rl);
			}
		}
		return false;
	}
	
	@Override
	public void myReadingPostProcess(int rows) {
		Toast t = Toast.makeText(getBaseContext(), rows + " affected.", Toast.LENGTH_LONG);
		t.show();	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_CODE_SELECT_DIR_CREATE_BOOK:
			if(resultCode == RESULT_OK){
				String dir = data.getStringExtra(FileDialog.RESULT_PATH);
				lastPath = dir;
				Log.d(TAG, "selected:" + dir);
				createBookFromExternalStorage(dir);
			}
			break;
		}
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK){
			mAppSectionsPagerAdapter.fragments[activeFragId].backPressed();
		}
		return false;
	}
	
	/*********************************
     *Action Helper methods
     ******************************/
	
	private Set<Reading> getSelectedReading(){
		return mAppSectionsPagerAdapter.fragments[this.activeFragId].getReadingGridAdapter().getSelectedReadings();
	}
	
	//select folder containing downloaded pages
	private void importBookContent(){
		Intent i = new Intent();
		i.setAction(CRBookIntents.ACTION_SELECT_DIR);
		i.putExtra(FileDialog.START_PATH, lastPath);
		startActivityForResult(i, REQUEST_CODE_SELECT_DIR_CREATE_BOOK);
	}
	
	private void deleteLocalReading(){
		//find selected book and delete
		Set<Reading> rlist = getSelectedReading();
		for (Reading r: rlist){
			if (r instanceof Book){
				localPersistManager.deleteBook(r.getId());
				int pages = localPersistManager.deletePagesOfBook(r.getId());
				Log.w(TAG, pages + " pages deleted. for book:" + r.getId() + ",name:" + r.getName());
	  			Log.w(TAG, "book:" + r.getId() + ", name:" + r.getName());
			}else if (r instanceof Volume){
				localPersistManager.deleteRecursiveVolumeById(r.getId());
			}
		}
	}
	// e.g. dir = /sdcard/media/crbook/iam4
	protected void createBookFromExternalStorage(String dir){		
		File d = new File(dir);
		if (d.isDirectory()){
			File[] files = d.listFiles();
			Book b = new Book();
			b.setCat(Volume.ROOT_VOLUME_SELF);
			b.setId(d.getName());
			b.setName(d.getName());
			List<String> pageUris = new ArrayList<String>();
			for (int i=0; i<files.length; i++){
				File f = files[i];
				if (f.isDirectory()){
					
				}else{
					if (f.getName().contains(CRConstants.FILE_COVER)){
						b.setCoverUri(f.getAbsolutePath());
						Log.d(TAG, "cover file:" + b.getCoverUri());
					}else{
						//add to pages
						pageUris.add(f.getAbsolutePath());
					}
				}
			}
			localPersistManager.createBookAndPageUrls(b, pageUris);					    	
		}else{
			Log.e(TAG, "dir:" + dir + " is not a book directory.");
		}
	}

	// e.g. dir = /sdcard/media/crbook/iam4
	protected void exportLocalReadingToExternalStorage(String dir){		
		File d = new File(dir);
		if (d.isDirectory()){
			Set<Reading> rlist = getSelectedReading();
			for (Reading r: rlist){
				if (r instanceof Book){
					List<Page> pagelist = localPersistManager.getPagesOfBook(r.getId());
					Book b = (Book) r;
					File f = new File(d, r.getId() + "." + XmlWorker.BOOK_SUFFIX_1);
					try{
						XmlWorker.writeBookXml(b, pagelist, f);
					}catch(Exception e){
						Log.e(TAG, "error when export:", e);
					}finally{
						
					}
				}else if (r instanceof Volume){
					Volume c = (Volume) r;
					File f = new File(d, r.getId() + "." + XmlWorker.VOL_SUFFIX_1);
					try{
						XmlWorker.writeVolumeXml(c, f);
					}catch(Exception e){
						Log.e(TAG, "error when export:", e);
					}finally{
						
					}
				}else {
					Log.e(TAG, "not supported type of reading:" + r.getClass().getName());
				}
			}
		}else{
			Log.e(TAG, "dir:" + dir + " is not a directory.");
		}
	}
}
