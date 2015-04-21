package cy.crbook.picbook;


import org.cld.util.PatternResult;

import cy.common.entity.Book;
import cy.common.entity.Page;
import cy.common.persist.LocalPersistManager;
import cy.crbook.CRBookIntents;
import cy.crbook.FileCache;
import cy.crbook.persist.PersistManagerFactory;
import cy.crbook.util.DownloadImageJobManager;
import cy.readall.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;

public class PageEditActivity extends Activity {

	public static final String TAG = "whiteboard";
	public static final boolean DBG = true;
	public static final String EXTRA_APP_ARGUMENT = "android.intent.extra.APPLICATION_ARGUMENT";
    private static final int REQUEST_CODE_PICK_COLOR = 1;
    private static final int REQUEST_CODE_PICK_LINE_WIDTH = 2;
    
	public static final int SET_COLOR = 0;
	public static final int SET_LINE_WIDTH = 1;
	public static final int START_ERASER = 2;
	public static final int STOP_ERASER = 3;
	public static final int CLEAR = 4;
	public static final int EXIT = 5;
	public static final int SAVE_BOARD = 11;
	private static final int CHANGE_RUN_MODE = 14;//select teacher/student
	private static final int MARK_READ = 15;
	private static final int SETTING=16;

	private PageEditView pageView = null;
	private TextView bookPageNumEdit = null;
	private SeekBar pageProgress = null;
    
    LocalPersistManager localPersistManager;
    
    public LocalPersistManager getLocalPersistManager(){
    	return localPersistManager;
    }
    
    public static int NUMBER_RUN_MODE=2;
    
    public static final int TEACHER_MODE=0;
    public static final int STUDENT_MODE=1;

    public static final int MODE_BROWSE=0;
    public static final int MODE_DRAW=1;//stroking
    public static final int MODE_EDIT=2;//doing selection, renaming, moving
    public static final int MODE_SCRIPT=3;//scripting
    public static final int MODE_CHECK=4;
    
    private int runMode = STUDENT_MODE;
    private int sub1Mode = MODE_BROWSE;
    
    
    public void setBookPageNum(int num){
    	bookPageNumEdit.setText(num+"", TextView.BufferType.EDITABLE);
    	pageProgress.setProgress(num-1);//progress bar starts from 0 to totalpage-1
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {    	
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.page);
		localPersistManager = PersistManagerFactory.getLocalPersistManager(this);
		
		pageView = (PageEditView)findViewById(R.id.drawingPanel1);
		pageView.setRunMode(runMode);
		pageView.setPEA(this);
		bookPageNumEdit = (TextView)findViewById(R.id.bookPageNum);
		pageProgress = (SeekBar)findViewById(R.id.pageProgress);
		pageProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (pageView.getBook()!=null)
					pageView.setupPage(seekBar.getProgress()+1, pageView.getBook(),  PageCurlView.BOTH_PAGE);
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				bookPageNumEdit.setText(progress+1+"", TextView.BufferType.EDITABLE);
			}
		});
		
		Intent intent = getIntent();
		Book book = new Book();
		int pageNum = 1;
		if (intent.getAction() == CRBookIntents.ACTION_OPEN_BOOK){
			String bookJson = intent.getExtras().getString(CRBookIntents.EXTRA_SAVED_BOOK);
			book.fromTopJSONString(bookJson);
			initPage(pageNum, book);
			if (book.getPageBgUrlPattern().getPatternType() == PatternResult.pt_list){
				pageProgress.setSecondaryProgress(book.getIndexedPages());
			}else{
				pageProgress.setSecondaryProgress(book.getTotalPage());
			}
		}else{
			Log.e(TAG, "unsupported action:" + intent.getAction());
		}
	}
    
    //1. high light the radio button corresponding to the sub1mode
    //2. hide/show corresponding radio button groups
    private void highLightSub1ModeRB(){
    	int viewId = getSub1ModeRadioButtonId();
    	RadioButton rb = (RadioButton)findViewById(viewId);
    	rb.setChecked(true);
    	if (this.runMode == TEACHER_MODE){
    		rb = (RadioButton)findViewById(R.id.checkMode);
    		rb.setVisibility(View.GONE);
    		rb = (RadioButton)findViewById(R.id.scriptMode);
    		rb.setVisibility(View.VISIBLE);
    	}else if (this.runMode == STUDENT_MODE){
    		rb = (RadioButton)findViewById(R.id.scriptMode);
    		rb.setVisibility(View.GONE);
    		rb = (RadioButton)findViewById(R.id.checkMode);
    		rb.setVisibility(View.VISIBLE);    		
    	}
    }
    private int getSub1ModeRadioButtonId(){
    	switch(this.sub1Mode){
    	case MODE_BROWSE:
    		return R.id.browseMode;
    	case MODE_DRAW:
    		return R.id.drawMode;
    	case MODE_EDIT:
    		return R.id.selectMode;
    	case MODE_SCRIPT:
    		return R.id.scriptMode;
    	case MODE_CHECK:
    		return R.id.checkMode;
    	default:
    		return -1;
    	}
    }
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
    	int preSub1Mode = sub1Mode;
        boolean checked = ((RadioButton) view).isChecked();
        
        // Check which radio button was clicked
        int id =view.getId();
        if (id== R.id.browseMode){
            if (checked)
                sub1Mode = MODE_BROWSE;
        }else if (id== R.id.drawMode){
            if (checked)
                sub1Mode = MODE_DRAW;
        }else if (id == R.id.selectMode){
            if (checked)
                sub1Mode = MODE_EDIT;
        }else if (id == R.id.scriptMode){
            if (checked)
                sub1Mode = MODE_SCRIPT;
        }else if (id == R.id.checkMode){
            if (checked)
                sub1Mode = MODE_CHECK;
        }
        
        if (preSub1Mode != sub1Mode){
        	if (pageView.isDirty()){
				promptSaveDialog();
			}
			pageView.setSub1Mode(sub1Mode);
			pageView.postInvalidate();
        }
        Log.w(TAG, "current selected mode:" + sub1Mode);
    }


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK){
		    if (pageView.isDirty() &&
		    		(sub1Mode == MODE_DRAW || sub1Mode == MODE_EDIT || sub1Mode == MODE_SCRIPT)) {
		        new AlertDialog.Builder(PageEditActivity.this)
		            .setTitle("Save changes?")
		            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                        promptSaveDialog();
	                        setResult(RESULT_OK);
	                        finish();
	                    }
	                })
	                .setNegativeButton("No", new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                        setResult(RESULT_CANCELED);
	                        finish();
	                    }
	                }).create().show();
		        
		        return true;
		    }
		    
	    	
	    	pageView.cancelLoadBook();
		    DownloadImageJobManager.singleton.cancelAll();
		    
		    //update book download status
		    Book b = pageView.getBook();
		    if (b.getCached()==0){
			    int downloadedPages = FileCache.getCachedPages(b);
				if (Math.abs(downloadedPages-b.getTotalPage())<3){
					b.setCached(1);
				}
		    }
		    b.setLastPage(pageView.getPageNum());
		    localPersistManager.insertOrUpdateBook(b);
		}
	    
	    return super.onKeyDown(keyCode, event);
	}
	
	public void initPage(int pageNum, Book book){
		pageProgress.setMax(book.getTotalPage()-1);
		pageView.setSub1Mode(sub1Mode);
		pageView.setupPage(pageNum, book, PageCurlView.BOTH_PAGE);
		highLightSub1ModeRB();
		pageView.postInvalidate();
	}
    
    public void savePage(Context context){
		localPersistManager.insertOrUpdatePage(pageView.getPage());
	}

	public void setRunmodeMenuItem(MenuItem item){
		if (runMode == TEACHER_MODE){
			item.setTitle(getResources().getText(R.string.teacher_mode));
			item.setIcon(getResources().getDrawable(R.drawable.icon_teacher));
		}else if (runMode == STUDENT_MODE){
			item.setTitle(getResources().getText(R.string.student_mode));
			item.setIcon(getResources().getDrawable(R.drawable.icon_student));
		}else{
			item.setTitle(getResources().getText(R.string.no_such_mode));
		}
	}
	
	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu){
	    MenuItem item;
        menu.clear();
        item = menu.add(0, CLEAR, 0, "Clear All");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        if (pageView.isEraseMode()) {
            menu.add(0, STOP_ERASER, 0, "Stop Erasing");
        } else {
        	menu.add(0, MARK_READ, 0, "Mark Read");
            //
            item = menu.add(0, CHANGE_RUN_MODE, 0 , "XXX");
            setRunmodeMenuItem(item);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            item = menu.add(0, SET_COLOR, 0, "Color");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            item = menu.add(0, SET_LINE_WIDTH, 0, "Width");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            item = menu.add(0, START_ERASER, 0, "Eraser");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }       
        menu.add(0, SAVE_BOARD, 0, "Save Page");
        menu.add(0, SETTING, 0, "Setting");
        menu.add(0, EXIT, 0, "Exit");
    
        return true;
    }

	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){		
		case MARK_READ:
			pageView.getBook().incRead();
			long ret = localPersistManager.insertOrUpdateBook(pageView.getBook());
			Log.i(TAG, "return:" + ret + ", for update book:" + pageView.getBook());
			return true;
		case SET_COLOR:
			pickColor();
			return true;
		case SET_LINE_WIDTH:
			pickLineWidth();
			return true;
		case START_ERASER:
			pageView.setEraseMode(true);
			return true;
		case STOP_ERASER:
			pageView.setEraseMode(false);
			return true;
		case CLEAR:
			if (runMode == TEACHER_MODE){
				pageView.getPage().clearQuestionStrokes();
			}else if (runMode == STUDENT_MODE){
				pageView.getPage().clearAnswerStrokes();
			}
			pageView.postInvalidate();
			Log.w(TAG, "postInvalidate called");
			return true;
		case SAVE_BOARD:
			promptSaveDialog();
			return true;
		case EXIT:
			Process.killProcess(Process.myPid());
			return true;
		case CHANGE_RUN_MODE:			
			if (pageView.isDirty()){
				promptSaveDialog();
			}			
			runMode = (runMode + 1) % NUMBER_RUN_MODE;
			setRunmodeMenuItem(item);
			pageView.setRunMode(runMode);
			highLightSub1ModeRB();
			pageView.postInvalidate();
			return true;
		case SETTING:
			Intent intent = new Intent();	
			intent.setAction(CRBookIntents.ACTION_SETTING);
			startActivity(intent);
			return true;
		}
		return false;
	}

	private void pickColor(){
		Intent i = new Intent();
		i.setAction(CRBookIntents.ACTION_PICK_COLOR);
		i.putExtra(CRBookIntents.EXTRA_COLOR, 0xFF000000 | pageView.getCurrentColor() );
		startActivityForResult(i, REQUEST_CODE_PICK_COLOR);
	}

	private void pickLineWidth(){
		Intent i = new Intent();
		i.setAction(CRBookIntents.ACTION_PICK_LINE_WIDTH);
		i.putExtra(CRBookIntents.EXTRA_LINE_WIDTH, pageView.getCurrentWidth() );
		startActivityForResult(i, REQUEST_CODE_PICK_LINE_WIDTH);
	}

	private void promptSaveDialog() {
		String bookId = pageView.getPage().getBookid();
		if (Page.NAME_NONAME.equals(bookId)){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.advertise_dialog_title);  
			alert.setMessage("Name this Whiteboard");
			final EditText input = new EditText(this);  
			alert.setView(input);
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
					public void onClick(DialogInterface dialog, int whichButton){
						savePage(PageEditActivity.this);
						pageView.setIsDirty(false);
						Toast.makeText(PageEditActivity.this, "Saved",
						        Toast.LENGTH_SHORT).show();
					}
				});  
			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
					public void onClick(DialogInterface dialog, int whichButton) {}  
				});  
			alert.show();  
		}else{//update
			savePage(PageEditActivity.this);
			pageView.setIsDirty(false);
			Toast.makeText(PageEditActivity.this, "Saved", Toast.LENGTH_SHORT).show();
		}
		return;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_CODE_PICK_COLOR:
			if(resultCode == RESULT_OK){
				int currentColor = data.getIntExtra(
					CRBookIntents.EXTRA_COLOR, pageView.getCurrentColor());
				// Get rid of alpha information
				currentColor &= 0x00FFFFFF;
				pageView.setCurrentColor(currentColor);
			}
			break;
		case REQUEST_CODE_PICK_LINE_WIDTH:
			if(resultCode == RESULT_OK){
				int currentWidth = data.getIntExtra(
					CRBookIntents.EXTRA_LINE_WIDTH, pageView.getCurrentWidth());
				pageView.setCurrentWidth(currentWidth);
			}
			break;
		}
	}
}