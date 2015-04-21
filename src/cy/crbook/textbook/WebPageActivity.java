package cy.crbook.textbook;


import org.cld.util.PatternResult;

import cy.common.entity.Book;
import cy.crbook.AsyncGetHtmlPage;
import cy.crbook.CRApplication;
import cy.crbook.CRBookIntents;
import cy.crbook.util.StringResultPostProcess;
import cy.readall.R;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WebPageActivity extends Activity implements StringResultPostProcess{

	public static final String TAG = "WebPageActivity";
    private int curPage=1;
    private Book book = new Book();
    private String contentXPath;
	
    WebView pageView;
    Button preButton;
    Button nextButton;
    EditText currentPageEditText;
    TextView totalPagesTextView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {    	
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.htmlpage);
		
		pageView = (WebView)findViewById(R.id.webview);
		preButton = (Button)findViewById(R.id.prev);
		nextButton = (Button)findViewById(R.id.next);
		currentPageEditText = (EditText)findViewById(R.id.currentPage);
		totalPagesTextView = (TextView)findViewById(R.id.totalPages);
		
		preButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				curPage--;
				if (curPage>=1){
					currentPageEditText.setText(curPage+"", TextView.BufferType.EDITABLE);
					openPage();
				}else{
					curPage++;
				}
			}
		});
		
		nextButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				curPage++;
				if (curPage<=book.getTotalPage()){
					currentPageEditText.setText(curPage+"", TextView.BufferType.EDITABLE);
					openPage();
				}else{
					curPage--;
				}
			}
		});
		
		currentPageEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
		    	try{
		    		curPage = Integer.parseInt(currentPageEditText.getText().toString());
		    		currentPageEditText.setText(curPage+"", TextView.BufferType.EDITABLE);
					openPage();
		    	}catch(NumberFormatException e){
		    		Toast t = Toast.makeText(WebPageActivity.this, "please enter a page number", Toast.LENGTH_LONG);
		    		t.show();
		    	}
		    }
		 });
		
		Intent intent = getIntent();
		if (intent.getAction() == CRBookIntents.ACTION_OPEN_HTMLPAGE){
			String bookJson = intent.getExtras().getString(CRBookIntents.EXTRA_SAVED_BOOK);
			contentXPath = intent.getExtras().getString(CRBookIntents.EXTRA_SAVED_XPATH);
			book.fromTopJSONString(bookJson);
			curPage = 1;
			totalPagesTextView.setText(" of " + book.getTotalPage());
			openPage();
		}else{
			Log.e(TAG, "unsupported action:" + intent.getAction());
		}
	}
	
    private void openPage(){
    	currentPageEditText.setText(curPage+"", TextView.BufferType.EDITABLE);
    	AsyncGetHtmlPage asyncGetHtmlPage = new AsyncGetHtmlPage((CRApplication)this.getApplication(), this);
		if (book.getPageBgUrlPattern()!=null){				
			String url = PatternResult.guessUrl(book.getPageBgUrlPattern(), curPage-1);
			asyncGetHtmlPage.execute(new String[]{url});
		}else{
			Log.e(TAG, "page does not have an backgroud url");
		}
    }
    
	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu){
	    MenuItem item;
        menu.clear();
    
        return true;
    }

	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){		
		}
		return false;
	}

	@Override
	public void postProcess(String result) {
		WebSettings settings = pageView.getSettings();
		settings.setDefaultTextEncodingName("utf-8");
		pageView.loadDataWithBaseURL(null, result, "text/html", "utf-8", null);
		
		
	}
}