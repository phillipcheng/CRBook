package cy.crbook.textbook;

import java.util.Map;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import cy.common.entity.Book;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.crbook.RecordHolder;
import cy.readall.R;

public class TextRowRecordHolder extends RecordHolder {

	private static final String TAG = "TextRowRecordHolder";
	TextView authorTxt;
	TextView totalItemsTxt;
	
	public TextRowRecordHolder(Map<String, Reading> items, CheckBox chkbox, 
			int position, View row, Reading r){
		super(items, chkbox, position, r);
		
		authorTxt = (TextView) row.findViewById(R.id.readingAuthor);
		totalItemsTxt = (TextView) row.findViewById(R.id.readingTotalItems);
	}
	
	public void loadContent(Reading r, int position){
		super.loadContent(r, position);
		
		String name = r.getName();
		String author = r.getAuthor();
		int totalItems=0;
		if (r instanceof Volume){
			totalItems = ((Volume)r).getBookNum();
		}else if (r instanceof Book){
			totalItems = ((Book)r).getTotalPage();
		}
		authorTxt.setText(author);
		totalItemsTxt.setText(totalItems+"");
		setCheckBoxName(chkbox, name, r);		
	}
}