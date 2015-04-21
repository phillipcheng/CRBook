package cy.crbook.persist;

import android.database.sqlite.*;
import android.util.Log;
import android.content.Context;

public class DBHelper extends SQLiteOpenHelper {

	//DB_Version=2 since APP_Version 2.4.1/29 
	//DB_Version=3 since APP_Version 2.4.8/36 
	//DB_Version=4 since APP_Version 2.4.9/37
	//DB_Version=5 since APP_Version 2.5.0/38
	//DB_Version=6 since APP_Version 2.5.4/42
	private static final int DATABASE_VERSION = 6;
	
	public static final String DATABASE_NAME = "CRBooks";
	
	public static final String TABLE_PAGE = "boards";
	public static final String TABLE_BOOK = "books";
	public static final String TABLE_VOL = "volumes";
	public static final String TABLE_PKG = "packages";
	public static final String TABLE_MY_READING="myreadings";
	
	//common
	public static final String COL_ID="id";
	public static final String COL_NAME="name";
	public static final String COL_TYPE="type";
	public static final String COL_UTIME = "utime"; //updated time
	public static final String COL_DATA = "data";
	public static final String COL_SIZE = "size";
	public static final String COL_AUTHOR="author";	
	
	//book
	public static final String COL_CAT = "cat";
	public static final String COL_TOTALPAGE="totalpage";
	public static final String COL_LASTPAGE="lastpage";
	public static final String COL_READ="read";
	public static final String COL_CACHED="cached";
	public static final String COL_INDEXPAGE="indexpage";
	public static final String COL_STATUS="status";
	
	//page
	public static final String COL_PAGENUM="pagenum";
	
	//volume
	public static final String COL_PARENTCAT="pcat";
	public static final String COL_BOOKNUM="booknum";
	
	//package
	public static final String COL_PTIME = "ptime"; //package time
	public static final String COL_ITIME = "itime"; //install time
	
	public static final String PAGE_TABLE_CREATE =
	"CREATE TABLE boards (id TEXT, pagenum INTEGER, data TEXT, utime TEXT, primary key(id, pagenum));";
	
	public final static String allBookDBFields =" id, type, name, totalpage, lastpage, utime, data, cat, read, cache, indexpage, author, status ";
	public static final String BOOK_TABLE_CREATE =
	"CREATE TABLE books (id TEXT, type integer, name TEXT, totalpage INTEGER, lastpage INTEGER, utime TEXT, data TEXT, cat TEXT, read INTEGER, cached INTEGER, indexpage INTEGER, author TEXT, status integer, primary key(id));";
	
	public final static String allVolumeDBFields = " id, type, name, utime, data, pcat, author, booknum ";
	public static final String VOL_TABLE_CREATE =
	"CREATE TABLE volumes (id TEXT, type integer, name TEXT, utime TEXT, data TEXT, pcat TEXT, author TEXT, booknum INTEGER, primary key(id));";
	
	public static final String PKG_TABLE_CREATE =
	"CREATE TABLE packages (name TEXT, size INTEGER, ptime TEXT, itime TEXT, primary key(name));";

	public static final String MYREADING_TABLE_CREATE=
	"CREATE TABLE myreadings (rid TEXT, primary key(rid));";
	
	//index
	public static final String BOOK_NAME_INDEX_CREATE=
			"create index if not exists book_name_idx on books (name);";
	public static final String BOOK_CAT_INDEX_CREATE=
			"create index if not exists book_cat_idx on books (cat);";
	public static final String BOOK_AUTHOR_INDEX_CREATE=
			"create index if not exists book_author_idx on books (author);";
	public static final String BOOK_TYPE_INDEX_CREATE=
			"create index if not exists book_type_idx on books (type);";
	public static final String BOOK_STATUS_INDEX_CREATE=
			"create index if not exists book_status_idx on books (status);";
	
	public static final String VOL_NAME_INDEX_CREATE=
			"create index if not exists vol_name_idx on volumes (name);";
	public static final String VOL_PCAT_INDEX_CREATE=
			"create index if not exists vol_pcat_idx on volumes (pcat);";
	public static final String VOL_AUTHOR_INDEX_CREATE=
			"create index if not exists vol_pcat_idx on volumes (author);";
	public static final String VOL_TYPE_INDEX_CREATE=
			"create index if not exists vol_type_idx on volumes (type);";
	
	//db migration sql
	public static final String BooksAddColumnIndexPageSQL= "alter table books add column indexpage integer default 0;";
	

	private static final String TAG = "DBHelper";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.w(TAG, "db create.");
		db.execSQL(PAGE_TABLE_CREATE);
		db.execSQL(BOOK_TABLE_CREATE);
		db.execSQL(VOL_TABLE_CREATE);
		db.execSQL(PKG_TABLE_CREATE);
		db.execSQL(MYREADING_TABLE_CREATE);
		
		//
		db.execSQL(BOOK_NAME_INDEX_CREATE);
		db.execSQL(BOOK_CAT_INDEX_CREATE);
		db.execSQL(BOOK_AUTHOR_INDEX_CREATE);
		db.execSQL(BOOK_TYPE_INDEX_CREATE);
		db.execSQL(BOOK_STATUS_INDEX_CREATE);
		
		db.execSQL(VOL_NAME_INDEX_CREATE);		
		db.execSQL(VOL_PCAT_INDEX_CREATE);		
		db.execSQL(VOL_AUTHOR_INDEX_CREATE);	
		db.execSQL(VOL_TYPE_INDEX_CREATE);	
	}

	//to from+1
	private void upgradeConsecutive(SQLiteDatabase db ,int from){
		if (from==2){
			//to v3
			db.execSQL(BOOK_CAT_INDEX_CREATE);
			db.execSQL(VOL_PCAT_INDEX_CREATE);	
			return;
		}
		if (from==3){
			//to v4
			db.execSQL(VOL_AUTHOR_INDEX_CREATE);
			db.execSQL(MYREADING_TABLE_CREATE);
		}
		if (from==4){
			//to v5
			db.execSQL(BooksAddColumnIndexPageSQL);
		}
	}
	@Override
	public void onUpgrade(SQLiteDatabase db ,int from ,int to){
		for (int i=from; i<to; i++){
			upgradeConsecutive(db, i);
		}
	}
}

