package cy.crbook;

import java.util.Map;
import android.widget.CheckBox;

import cy.common.entity.Reading;

public abstract class RecordHolder {
	private static final String TAG = "RecordHolder";

	//view for thumb-nail
	protected CheckBox chkbox; //the chkbox view
	int position;
	protected Reading r;
	
	//passed in
	Map<String, Reading> items;
	
	public RecordHolder(Map<String, Reading> items, CheckBox chkbox, int position, Reading r){
		this.items = items;
		this.chkbox = chkbox;
		this.position = position;
		this.r = r;
	}
	
	public String toString(){
		String ret = "isChecked:" + chkbox.isChecked();
		ret += ", position:" + position;
		ret += ", reading:" + r.getName();
		return ret;
	}
	
	public int getPosition(){
		return position;
	}
	
	public Reading getReading(){
		return r;
	}
	
	protected void setCheckBoxName(CheckBox cb, String name, Reading r){
		//maintain the items map, name to item
		synchronized(items){
			items.remove(cb.getText());
			items.put(name, r);
			cb.setText(name);
		}
		
	}
	
	public void loadContent(Reading r, int position){
		this.r = r;
		this.position = position;
	}
}