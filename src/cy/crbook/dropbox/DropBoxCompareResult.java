package cy.crbook.dropbox;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.DropboxAPI.Entry;

import cy.common.entity.BPackage;


public class DropBoxCompareResult{
	public static final String DropBoxEntry_sdformat="EEE, dd MMM yyyy kk:mm:ss ZZZZZ";
	public static final SimpleDateFormat db_sdf = new SimpleDateFormat(DropBoxEntry_sdformat, Locale.US);
	
	public static final int UPDATED=1; //reference one is updated
	public static final int NEW=2; //reference one is new
	public static final int DELETED=3; //reference one is deleted
	public static final int EQUAL=4; //reference = users 
	
	//updated or new, obj stores the reference's
	//deleted or equal, obj stores the users'
	private int flag;
	private Object obj;
	
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public Object getObj() {
		return obj;
	}
	public void setObj(Object obj) {
		this.obj = obj;
	}
	
	public static String getEntryName(Entry e){
		String p = e.path;
		return p.substring(p.lastIndexOf(File.separatorChar)+1);
	}
	
	public static Date getEntryUpdateTime(Entry e){
		String st = e.modified;
		return RESTUtility.parseDate(st);
	}
	
	@Override
	public boolean equals(Object obj){
		if (!(obj instanceof DropBoxCompareResult)){
			return false;
		}
		DropBoxCompareResult cr = (DropBoxCompareResult)obj;
		if (cr.flag != this.flag){
			return false;
		}
		if (cr.flag == UPDATED || cr.flag == NEW){
			return ((Entry)(this.obj)).equals(cr.obj);
		}
		if (cr.flag == DELETED || cr.flag == EQUAL){
			return ((BPackage)(this.obj)).equals(cr.obj);
		}
		return false;			
	}
	
	public String toString(){
		if (flag == UPDATED || flag == NEW){
			Entry ent= ((Entry)(obj));
			return ent.path;
		}
		if (flag == DELETED || flag == EQUAL){
			return ((BPackage)(obj)).toString();
		}
		return "";
	}
	
	public String getName(){
		if (flag == UPDATED || flag == NEW){
			return getEntryName((Entry)(this.obj));
		}
		if (flag == DELETED || flag == EQUAL){
			return ((BPackage)(this.obj)).getName();
		}
		return null;
	}
	
	public static List<Entry> getEntryList(Set<DropBoxCompareResult> crlist){
		List<Entry> el = new ArrayList<Entry>();
		for (DropBoxCompareResult cr: crlist){
			if (cr.flag == DropBoxCompareResult.NEW || cr.flag == DropBoxCompareResult.UPDATED){
				el.add((Entry)cr.getObj());
			}
		}
		return el;
	}
	
	public static List<BPackage> getBPackageList(Set<DropBoxCompareResult> crlist){
		List<BPackage> el = new ArrayList<BPackage>();
		for (DropBoxCompareResult cr: crlist){
			if (cr.flag == DropBoxCompareResult.DELETED || cr.flag == DropBoxCompareResult.EQUAL){
				el.add((BPackage)cr.getObj());
			}
		}
		return el;		
	}
	
	public static BPackage convert(Entry e){
		String name = getEntryName(e);
		String ptime="";;
		try {
			ptime = BPackage.sdf.format(db_sdf.parse(e.modified));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		};
		long size = e.bytes;		
		String itime = BPackage.sdf.format(new Date());
		return new BPackage(name, size, ptime, itime);
		
	}
}