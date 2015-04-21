package cy.crbook;


import java.io.File;

import snae.tmc.TMHttpClient;
import snae.tmc.TMURLManager;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import cy.cfs.CFSInstance;
import cy.readall.R;

public class CRSettingActivity extends Activity {
	
	private static final String TAG = "CRSettingActivity";
	
	CheckBox enableSNECb = null;
	EditText sneHostET=null;
	EditText snePortET=null;
	Button sneProxyBtn = null;
	
	EditText downloadThreadNumET=null;
	EditText itemsPerPageET=null;
	CheckBox localModeCB = null;
	Spinner spinnerWrite = null;
	Button btnCacheDir = null;
	Button btnClearCache = null;
	Spinner spinnerRead = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final CRApplication myApp = (CRApplication) this.getApplication();
		myApp.loadPreference();
		
		setContentView(R.layout.setting);
		
		/////// begin of TM ////
		
		sneHostET = (EditText)findViewById(R.id.SNEIP);
		sneHostET.setText(myApp.getSneHost());
		
		snePortET = (EditText)findViewById(R.id.SNEPort);
		snePortET.setText(myApp.getSnePort()+"");
		
		sneProxyBtn = (Button)findViewById(R.id.btnUseSNE);
		myApp.setProxyCBText(sneProxyBtn);
		sneProxyBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				myApp.setupSNE(sneHostET.getText().toString(), 
						Integer.parseInt(snePortET.getText().toString()), 
								myApp.getUserid(), sneProxyBtn);
			}});
		
		enableSNECb = (CheckBox)findViewById(R.id.cbEnableSNE);
		if (myApp.getTmurlMgr()==null){
			enableSNECb.setChecked(false);
			sneProxyBtn.setEnabled(false);
		}else{
			enableSNECb.setChecked(true);
			sneProxyBtn.setEnabled(true);
		}
		enableSNECb.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					//enable the sne setting
					sneProxyBtn.setEnabled(true);
				}else{
					//disable the whole sne
					sneProxyBtn.setEnabled(false);
					myApp.setTmurlMgr(null);//
				}
			}
		});
		/////// end of TM ////
		
		downloadThreadNumET = (EditText) findViewById(R.id.downloadThreadNum);
		downloadThreadNumET.setText(myApp.getThreadNum()+"");
		
		itemsPerPageET = (EditText) findViewById(R.id.itemsPerPage);
		itemsPerPageET.setText(myApp.getPageSize()+"");
		
		localModeCB = (CheckBox)findViewById(R.id.localMode);
		localModeCB.setChecked(myApp.getLocalMode());
		
		
		spinnerWrite = (Spinner) findViewById(R.id.spinnerWriteCacheMode);
		ArrayAdapter<CharSequence> writeAdapter = ArrayAdapter.createFromResource(this,
		        R.array.saveMode, android.R.layout.simple_spinner_item);
		writeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerWrite.setAdapter(writeAdapter);
		spinnerWrite.setOnItemSelectedListener(new OnItemSelectedListener(){
			int check=0;
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				check++;
				if (check>1){
					Log.i(TAG, id+"");//the id is in-sync with the saveMode definition, see CRApplication
					myApp.setSaveMode((int) id);
					showButtons();
				}else{
					Log.i(TAG, "called by init set to init value.");
					spinnerWrite.setSelection(myApp.getSaveMode());
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		btnCacheDir = (Button) findViewById(R.id.btnCacheDir);
		btnCacheDir.setText(FileCache.getCacheRoot());
		
		btnClearCache = (Button)findViewById(R.id.btnClearCache);
		btnClearCache.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				File cacheRoot = new File(FileCache.getCacheRoot());
				deleteRecursive(cacheRoot);
			}
		});
		
		spinnerWrite.setSelection(myApp.getSaveMode());
		showButtons();
		
		spinnerRead = (Spinner)findViewById(R.id.spinnerReadFromMode);
		spinnerRead.setOnItemSelectedListener(new OnItemSelectedListener(){
			int check=0;
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				check++;
				if (check>1){
					Log.i(TAG, id+"");//the id is in-sync with the readMode definition, see CRApplication
					myApp.setReadMode((int) id);
				}else{
					Log.i(TAG, "called by init set to init value.");
					spinnerRead.setSelection(myApp.getReadMode());
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		spinnerRead.setSelection(myApp.getReadMode());
		
		ArrayAdapter<CharSequence> readAdapter = ArrayAdapter.createFromResource(this,
		        R.array.readMode, android.R.layout.simple_spinner_item);
		readAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerRead.setAdapter(readAdapter);
		
		Button cfsSettingBtn = (Button) findViewById(R.id.cfsSetting);
		cfsSettingBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setAction(CFSInstance.CFS_ACTION_CONNECT);
				intent.putExtra(CFSInstance.INTENT_EXTRA_CFS_USER_ID, myApp.getUserid());
				startActivity(intent);
			}
		});
	}
	
	private void deleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            deleteRecursive(child);
	    fileOrDirectory.delete();
	}
	
	private void showButtons(){
		CRApplication myApp = (CRApplication) getApplication();
		if (myApp.getSaveMode()==CRApplication.SAVE_TO_FILE){
			btnCacheDir.setVisibility(View.VISIBLE);
			btnClearCache.setVisibility(View.VISIBLE);
		}else{
			btnCacheDir.setVisibility(View.GONE);
			btnClearCache.setVisibility(View.GONE);
			
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		CRApplication myApp = (CRApplication) this.getApplication();
		boolean quit=true;
		if (keyCode == KeyEvent.KEYCODE_BACK){
			String threadNumText = downloadThreadNumET.getText().toString();
			try {
				int threadNum = Integer.parseInt(threadNumText);
				myApp.setThreadNum(threadNum);
				
				int pageSize = Integer.parseInt(itemsPerPageET.getText().toString());
				myApp.setPageSize(pageSize);
				
				boolean localMode= localModeCB.isChecked();
				myApp.setLocalMode(localMode);
				
				Toast t = Toast.makeText(getApplicationContext(), "setting updated.", 
						Toast.LENGTH_LONG);
				myApp.savePreference();
				t.show();
			}catch(Exception e){
				quit=false;
				Toast t = Toast.makeText(getApplicationContext(), "invalid thread num:" + threadNumText, 
						Toast.LENGTH_LONG);
				t.show();
			}
		}
		if (quit){
			super.onKeyDown(keyCode, event);
			return true;
		}
		else
			return false;	
	}
}
