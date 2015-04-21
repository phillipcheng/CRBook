package cy.crbook.persist;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.cld.util.PatternResult;
import org.cld.util.jdbc.SqlUtil;

import cy.common.entity.BPackage;
import cy.common.entity.Book;
import cy.common.entity.Reading;
import cy.common.entity.Volume;
import cy.common.entity.Page;
import cy.common.persist.BatchPersistManager;
import cy.common.persist.LocalPersistManager;
import cy.common.persist.RemotePersistManager;

public class SQLitePersistManager implements LocalPersistManager, BatchPersistManager{
	
	SQLiteOpenHelper dbhelper = null;
	
	public SQLitePersistManager(SQLiteOpenHelper dbhelper){
		this.dbhelper = dbhelper;
	}
	
	private final String TAG = "PersistManager";
	
	/**
	 * @param range: 
	 *     -1: ""
	 * 		0: limit BATCH_SIZE
	 *      n: limit n*BATCH_SIZE, (n+1)*BATCH_SIZE
	 * @return
	 */
	private String getLimitRange(int offset, int limit){
		//<offset>,<limit>
		if (offset <0){
			return "";
		}else if (offset == 0){
			return limit+"";
		}else{
			return offset+","+limit;
		}
	}
	
	//using in SQL direct construction 
	private String getLimitFullString(int offset, int limit){
		//limit <offset>,<limit>
		if (offset <0){
			return "";
		}else if (offset == 0){
			return "limit " + limit + "";
		}else{
			return "limit " + offset + "," + limit;
		}
	}

	
	//pagenum is from 1 to totalPage
	public String getPageBgUrl(Book b, int pageNum){
		if (b!=null){
			PatternResult pattern = b.getPageBgUrlPattern();
			if (pattern!=null){
				return PatternResult.guessUrl(pattern, pageNum-1);
			}
			
			//read page from db
			Page p = getPage( b.getId(), pageNum);
			if (p!=null){
				String url="";
				if (b.getbUrl()!=null){ 
					url = b.getbUrl() + p.getBackgroundUri();
				}
				if (b.getsUrl()!=null){
					url = url + b.getsUrl();
				}
				return url;
			}
		}
		
		return null;
	}
	
	public void batchInsertVols(String[][] vols){
		synchronized (dbhelper){
			Log.i(TAG, "number of vols to be inserted or updated:" + vols.length);
		
			SQLiteDatabase db = null;
			int suc=0;
			try{
				db = dbhelper.getWritableDatabase();
				String sql = "insert or replace into " + DBHelper.TABLE_VOL + 
						" (id, name, utime, pcat, author, data, booknum) values (?, ?, ?, ?, ?, ?, ?)";
				SQLiteStatement statement = db.compileStatement(sql);
		        
				db.beginTransaction();
				for (int i=0; i<vols.length; i++){
					String[] vol = vols[i];
					statement.clearBindings();
					statement.bindString(1, vol[0]);
					statement.bindString(2, vol[1]);
					statement.bindString(3, vol[2]);
					statement.bindString(4, vol[3]);
					statement.bindString(5, vol[4]);
					if (vol[5] == null)
						statement.bindNull(6);
					else
						statement.bindString(6, vol[5]);
					statement.bindLong(7, Integer.parseInt(vol[6]));
	                long ret = statement.executeInsert();
	                if (ret != -1){
	                	suc++;
	                }
				}
				db.setTransactionSuccessful();
				Log.i(TAG, "number of vols inserted or updated:" + suc);
			}catch(Exception e){
				Log.e(TAG, "", e);
			}finally{
				db.endTransaction();
				if (db != null){
					db.close();
				}
			}
		}
	}
	
	@Override
	public void batchInsertPages(String[][] pages){
		synchronized (dbhelper){
			Log.i(TAG, "number of pages to be inserted or updated:" + pages.length);
			SQLiteDatabase db = null;
			int suc=0;
			try{
				db = dbhelper.getWritableDatabase();
				String sql = "insert or replace into " + DBHelper.TABLE_PAGE + 
						" (id, pagenum, data) values (?, ?, ?)";
				SQLiteStatement statement = db.compileStatement(sql);
		        
				db.beginTransaction();
				for (int i=0; i<pages.length; i++){
					String[] page = pages[i];
					statement.clearBindings();
					statement.bindString(1, page[0]);
					statement.bindLong(2, Integer.parseInt(page[1]));
					statement.bindString(3, page[2]);
	                long ret = statement.executeInsert();
	                if (ret != -1){
	                	suc++;
	                }
				}
				db.setTransactionSuccessful();
				Log.i(TAG, "number of pages inserted or updated:" + suc);
			}catch(Exception e){
				Log.e(TAG, "", e);
			}finally{
				db.endTransaction();
				if (db != null){
					db.close();
				}
			}
		}
	}
	
	@Override
	public void batchInsertBooks(String[][] books){
		synchronized (dbhelper){
			SQLiteDatabase db = null;
			Log.i(TAG, "number of books to be inserted or updated:" + books.length);
			int suc=0;
			try{
				db = dbhelper.getWritableDatabase();
				String sql = "insert or replace into " + DBHelper.TABLE_BOOK + 
						" (id, name, totalpage, lastpage, utime, data, cat) values (?, ?, ?, ?, ?, ?, ?)";
				SQLiteStatement statement = db.compileStatement(sql);     
				
				db.beginTransaction();
				for (int i=0; i<books.length; i++){
					String[] book = books[i];
					statement.clearBindings();
					statement.bindString(1, book[0]);
					statement.bindString(2, book[1]);
					statement.bindLong(3, Integer.parseInt(book[2]));
					statement.bindLong(4, Integer.parseInt(book[3]));
					statement.bindString(5, book[4]);
					statement.bindString(6, book[5]);
					statement.bindString(7, book[6]);
					long ret = statement.executeInsert();
	                if (ret != -1){
	                	suc++;
	                }
				}
				db.setTransactionSuccessful();
				Log.i(TAG, "number of books inserted or updated:" + suc);
			}catch(Exception e){
				Log.e(TAG, "", e);			
			}finally{
				db.endTransaction();
				if (db != null){
					db.close();
				}
			}
		}
	}
	
	@Override
	public void createBookAndPageUrls(Book b, List<String> pageUris){
		synchronized (dbhelper){
			String[] aPageUris = new String[pageUris.size()];
			pageUris.toArray(aPageUris);
			Arrays.sort(aPageUris);
			List<Page> pages = new ArrayList<Page>();
			for (int i=0; i<aPageUris.length; i++){
				String strP = aPageUris[i];
				Page p = new Page();
				p.setBookid(b.getId());
				p.setBackgroundUri(strP);
				p.setPageNum(i+1);//page number ranging from 1 to totalPage
				p.setUtime(RemotePersistManager.SDF_SERVER_DTZ.format(new Date()));
				
				pages.add(p);
				Log.w(TAG, "page to create:" + p);
			}
			b.setTotalPage(aPageUris.length);
			b.setUtime(new Date());
			Log.w(TAG, "book to create:" + b);
			
	    	
	    	insertOrUpdateBook(b);
	    	
			for (int i=0; i<pages.size(); i++){
				Page p = pages.get(i);
				insertOrUpdatePage(p);
			}	
		}
	}	
	
	//return the all cats up to the root cat this r belongs to
	private List<Reading> getRecursiveCatUp(Reading r){
		List<Reading> ls = new ArrayList<Reading>();
		String id = r.getId();
		while (id!=null && !r.getCat().startsWith(Volume.ROOT_VOLUME_PREFIX)){
			ls.add(r);
			//not root
			Volume v = getVolumeById( r.getCat());
			if (v!=null){
				id = v.getId();
				r = v;
			}else{
				Log.e(TAG, "Volume:" + r.getCat() + " does not exist.");
				break;
			}
		}
		if (id!=null){
			//r's parent is root, r's id is id
			ls.add(r);
			//
			ls.add(Volume.getRootVolume(r.getCat()));
		}
		return ls;
	}
	
	//return the all cats down to leaf cat (containing only books), not including v itself
	//return in depth first order
	private List<Volume> getRecursiveCatDown(Volume v){
		List<Volume> lv = new ArrayList<Volume>();
		lv = getVolumesByPCat(new String[]{v.getId()}, -1, 0);
		if (lv.size()==0){
			return lv;
		}else{
			List<Volume> retlv = new ArrayList<Volume>();
			for (int i=0; i<lv.size(); i++){
				Volume va = lv.get(i);
				retlv.addAll(getRecursiveCatDown( va));
			}
			retlv.addAll(lv);
			return retlv;
		}
	}
	
	//
	public Book getFirstBook(Volume v){
		List<Book> lb = new ArrayList<Book>();
		lb = getBooksByCat(new String[]{v.getId()}, 0, 1);
		if (lb.size()>=1){
			return lb.get(0);
		}
		List<Volume> lv = getVolumesByPCat(new String[]{v.getId()}, 0, 1);
		for (Volume av : lv){
			Book b = getFirstBook( av);
			if (b!=null){
				return b;
			}
		}
		return null;
	}
	//////////////////////
	//Category
	///////////////////////
	/**
	 * @param dbhelper
	 * @param r
	 * @return the cat-path from the root, including reading's name or id
	 */
	public synchronized String getCatIdPath(String id){		
		synchronized (dbhelper){
			Reading r = getBookById(id);
			if (r==null){
				r=getVolumeById(id);
			}
			List<Reading> ls = getRecursiveCatUp( r);
			String catPath="";
			for (int i=ls.size()-1; i>=0; i--){
				if (i==ls.size()-1){
					catPath= ls.get(i).getId();
				}else{
					catPath = catPath + File.separatorChar +ls.get(i).getId();
				}
			}
			return catPath;
		}
	}
	
	public synchronized String getReadingDisplayName(Reading r){
		synchronized (dbhelper){
			List<Reading> ls = getRecursiveCatUp( r);
			String catPath="";
			for (int i=ls.size()-1; i>=0; i--){
				if (i==ls.size()-1){
					catPath= ls.get(i).getName();
				}else{
					catPath = catPath + File.separatorChar +ls.get(i).getName();
				}
			}
			return catPath;
		}
	}
	public long insertOrUpdateReading(Reading r, SQLiteDatabase db){
		if (r instanceof Volume){
			return insertOrUpdateVolume((Volume)r, db);
		}else if (r instanceof Book){
			return insertOrUpdateBook((Book)r, db);
		}else{
			Log.e(TAG, "no such type to insert or update.");
			return -1;
		}
	}
	public Reading getReadingById(Reading r, SQLiteDatabase db){
		if (r instanceof Volume){
			return getVolumeById(r.getId(), db);
		}else if (r instanceof Book){
			return getBookById(r.getId(), db);
		}else{
			Log.e(TAG, "no such type to insert or update.");
			return null;
		}
	}
	
	public long insertOrUpdateIfNew(Reading reading){
		synchronized (dbhelper){
	    	SQLiteDatabase db = null;
	    	try {
	    		db = dbhelper.getWritableDatabase();	    	
		    	Reading oldReading = getReadingById(reading, db);
		    	if (oldReading!=null){
		    		Date oldTS = oldReading.getUtime();
			    	if (oldTS!=null){
			    		//new is from server
			    		Date newTS = reading.getUtime();
			    		if (newTS!=null){
					    	if (newTS.after(oldTS)){
					    		return insertOrUpdateReading(reading, db);
					    	}else{
					    		return 0;
					    	}		    		
			    		}else{
			    			//if the new vol comes without a timestamp and i already have one, i will not accept it.
			    			return 0;
			    		}
			    	}else{
			    		//if oldVolume has no time, update it
			    		return insertOrUpdateReading(reading, db);
			    	}
		    	}else{
		    		//if no oldVolume, add new
		    		return insertOrUpdateReading(reading, db);
		    	}
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    		return 0;
	    	}finally{
	    		if (db!=null){
	    			db.close();
	    		}
	    	}
		}
	}
	

	private long insertOrUpdateVolume(Volume vol, SQLiteDatabase db){
		vol.dataToJSON();
		ContentValues vals = new ContentValues();
		vals.put(DBHelper.COL_ID, vol.getId());
		vals.put(DBHelper.COL_TYPE, vol.getType());
		vals.put(DBHelper.COL_NAME, vol.getName());
		String strCur = RemotePersistManager.SDF_SERVER_DTZ.format(new Date());
		vals.put(DBHelper.COL_UTIME, strCur);
		vals.put(DBHelper.COL_DATA, vol.getData());
		vals.put(DBHelper.COL_PARENTCAT, vol.getParentCat());
		vals.put(DBHelper.COL_AUTHOR, vol.getAuthor());
		vals.put(DBHelper.COL_BOOKNUM, vol.getBookNum());	
		return db.insertWithOnConflict(DBHelper.TABLE_VOL, null, vals, 
				SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	public long insertOrUpdateVolume(Volume vol){
		synchronized (dbhelper){
			SQLiteDatabase db = null;
			try{
		    	db = dbhelper.getWritableDatabase();
		    	return insertOrUpdateVolume(vol, db);
			}
			catch(Exception e){
				Log.e(TAG, e.toString(), e);
				return -1;
			}finally{
				if (db!=null)
					db.close();
			}
		}
	}

    public long getVCByPCat(String[] pcatValue){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db = dbhelper.getReadableDatabase();
	    	try {
	    		String inSql = SqlUtil.generateInParameters(pcatValue);
	    		return DatabaseUtils.queryNumEntries(db, DBHelper.TABLE_VOL,
	    				DBHelper.COL_PARENTCAT + " in " + inSql , 
	    				pcatValue);
	    	}finally{
	    		db.close();
	    	}    	
    	}
    }
    
    public long getVCByName(String name){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db = dbhelper.getReadableDatabase();
	    	try {
	    		String where= DBHelper.COL_NAME + " like '%" + name + "%'"; 	    		
	    		return DatabaseUtils.queryNumEntries(db, DBHelper.TABLE_VOL,
	    				where, null);
	    	}finally{
	    		db.close();
	    	}    	
    	}
    }
    
    public long getVCByAuthor(String author){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db = dbhelper.getReadableDatabase();
	    	try {
	    		String where= DBHelper.COL_AUTHOR + " like '%" + author + "%'"; 
	    		return DatabaseUtils.queryNumEntries(db, DBHelper.TABLE_VOL,
	    				where, null);
	    	}finally{
	    		db.close();
	    	}    	
    	}
    }
    
    public long getVCLike(String param){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db = dbhelper.getReadableDatabase();
	    	try {
	    		String where= DBHelper.COL_AUTHOR + " like '%" + param + "%' or " + 
	    				DBHelper.COL_NAME + " like '%" + param + "%'"; 
	    		return DatabaseUtils.queryNumEntries(db, DBHelper.TABLE_VOL,
	    				where, null);
	    	}finally{
	    		db.close();
	    	}    	
    	}
    }
    
    private String[] volColumns = new String[] {
		DBHelper.COL_ID, DBHelper.COL_TYPE, DBHelper.COL_NAME, DBHelper.COL_UTIME, 
		DBHelper.COL_DATA, DBHelper.COL_PARENTCAT, DBHelper.COL_AUTHOR, DBHelper.COL_BOOKNUM};
    private Volume getVolume(Cursor cursor){
    	return new Volume(
				cursor.getString(0), cursor.getInt(1), cursor.getString(2), 
				cursor.getString(3), cursor.getString(4), cursor.getString(5), 
				cursor.getString(6), cursor.getInt(7));
    }
    
    //order by name asc
    private List<Volume> getVolumesBySQL(SQLiteDatabase db, String where, String[] whereValues, int offset, int limit){
    	ArrayList<Volume> cats = new ArrayList<Volume>();
		String orderBy=DBHelper.COL_NAME;
		Cursor cursor = db.query(DBHelper.TABLE_VOL, volColumns,
					where, whereValues, null, null, orderBy, getLimitRange(offset, limit));
		if(cursor.moveToFirst()){
			do {
				Volume c = getVolume(cursor);
				Log.i(TAG, "got cat: " + c);
				cats.add(c);
			} while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()){
			cursor.close();
		}
		Log.d(TAG, "total volumes got: " + cats.size());
		return cats;
    }
    private List<Volume> getVolumesByAuthor(String author, SQLiteDatabase db, int offset, int limit){    	
    	String where= DBHelper.COL_AUTHOR + " like '%" + author + "%'"; 
		return getVolumesBySQL(db, where, null, offset, limit);
    }
    
    private List<Volume> getVolumesByName(String name, SQLiteDatabase db, int offset, int limit){    	
    	String where= DBHelper.COL_NAME + " like '%" + name + "%'"; 
		return getVolumesBySQL(db, where, null, offset, limit);  	
    }
    
    public List<Volume> getVolumesLike(String param, SQLiteDatabase db, int offset, int limit){
    	String where= DBHelper.COL_NAME + " like '%" + param + "%' or " + 
				DBHelper.COL_AUTHOR + " like '%" + param + "%'"; 
		return getVolumesBySQL(db, where, null, offset, limit);  	  
    }
    
    public List<Volume> getVolumesByPCat(String[] pcatValue, SQLiteDatabase db, int offset, int limit){    	
    	String inSql = SqlUtil.generateInParameters(pcatValue);
    	String where= DBHelper.COL_PARENTCAT + " in " + inSql; 
		String[] whereValues = pcatValue;
		return getVolumesBySQL(db, where, whereValues, offset, limit);  		
    }
    
    public List<Volume> getVolumesByName(String name, int offset, int limit){
    	synchronized (dbhelper) {
    		SQLiteDatabase db = null;
    		try {
	    		db = dbhelper.getReadableDatabase();
		    	return getVolumesByName( name, db, offset, limit);
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    		return new ArrayList<Volume>();
	    	}finally{
	    		if (db!= null)
	    			db.close();
	    	}
    	}
    }
    
    public List<Volume> getVolumesByAuthor(String author, int offset, int limit){
    	synchronized (dbhelper) {
    		SQLiteDatabase db = null;
    		try {
	    		db = dbhelper.getReadableDatabase();
		    	return getVolumesByAuthor(author, db, offset, limit);
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    		return new ArrayList<Volume>();
	    	}finally{
	    		if (db!= null)
	    			db.close();
	    	}
    	}
    }
    
    public List<Volume> getVolumesLike(String param, int offset, int limit){
    	synchronized (dbhelper) {
    		SQLiteDatabase db = null;
    		try {
	    		db = dbhelper.getReadableDatabase();
	    		return getVolumesLike(param, db, offset, limit);
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    		return new ArrayList<Volume>();
	    	}finally{
	    		if (db!= null)
	    			db.close();
	    	}
    	}
    }
    
    //get category with parentcat is null or ""
    public List<Volume> getVolumesByPCat(String[] pcatValue, int offset, int limit){
    	synchronized (dbhelper) {
    		SQLiteDatabase db = null;
    		try {
	    		db = dbhelper.getReadableDatabase();
		    	return getVolumesByPCat( pcatValue, db, offset, limit);
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    		return new ArrayList<Volume>();
	    	}finally{
	    		if (db!= null)
	    			db.close();
	    	}
    	}
	}
    

    
    private Volume getVolumeById(String id, SQLiteDatabase db){
    	Volume c = null;
    	String where= DBHelper.COL_ID + "=?"; 
		String[] whereValues = new String[1];
		whereValues[0]= id;
    	Cursor cursor = db.query(DBHelper.TABLE_VOL, volColumns, 
								 where, whereValues, null, null, null);
		if(cursor.moveToFirst()){
			do {
				c = getVolume(cursor);
				Log.d(TAG, "got category: " + c);
			} while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()){
			cursor.close();
		}
		return c;
    }
    
    public Volume getVolumeById(String id){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db=null;
	    	try {
	    		db = dbhelper.getReadableDatabase();
		    	return getVolumeById(id, db);
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    		return null;
	    	}finally{
	    	
	    		if (db!=null)
	    			db.close();
	    	}
    	}
    }
    
    //delete volumes whose pcat is pcat
    private int deleteVolumesByPCat(String pcat){
	  	SQLiteDatabase db = dbhelper.getWritableDatabase();
	  	try {
	    	String where= DBHelper.COL_PARENTCAT + "=?"; 
			String[] whereValues = new String[]{pcat};
	    	int rows = db.delete(DBHelper.TABLE_VOL, where, whereValues);
			return rows;
	  	}finally{
	  		db.close();
	  	}
	}
    
    //delete volumes itself
    private int deleteVolumeById(String id){
	  	SQLiteDatabase db = dbhelper.getWritableDatabase();
	  	try {
	    	String where= DBHelper.COL_ID + "=?"; 
			String[] whereValues = new String[]{id};
	    	int rows = db.delete(DBHelper.TABLE_VOL, where, whereValues);
			return rows;
	  	}finally{
	  		db.close();
	  	}
	}
    
    
    public int deleteRecursiveVolumeById(String id){
    	synchronized (dbhelper) {
    		Volume v = getVolumeById( id);
	    	
		  	List<Volume> lv = getRecursiveCatDown( v);
		  	lv.add(v);
		  	for (Volume va : lv){
	  			//1st cat, it's child can be books, and for book, we need to delete book and pages
	  			//remove all books with this cat
		  		List<Book> blist = getBooksByCat(new String[]{va.getId()}, -1, 0);
		  		for (Book b : blist){
		  			int pages = deletePagesOfBook( b.getId());
		  			Log.i(TAG, pages + " pages deleted. for book:" + b.getId() + "," + b.getName());
		  			deleteBook( b.getId());
		  			Log.i(TAG, "book:" + b.getId() + ", name:" + b.getName());
		  		}
	  		
		  		//now we remove volumes.
		  		int vs = deleteVolumesByPCat( va.getId());
		  		Log.i(TAG, vs + " volumes deleted. whose parent volume is:" + va.getId());
		  		
		  		//delete myself
			  	deleteVolumeById( va.getId());
		  		Log.i(TAG, "volume deleted with id:" + va.getId());
		  	}
		  	
		  	return 0;
    	}
	}
    
    ////////////////////////
    //Set Volume Book number
    ////////////////////////    
    public void setBookNum(){
    	SQLiteDatabase db = dbhelper.getWritableDatabase();
    	try{
    		//setBCForLeafVolume( db);    	
    		for (String rootId:Volume.ROOT_VOLUMES.keySet()){
    			setBCForNoneLeafVolume( rootId, db);
    		}
    	}finally{
    		if (db!=null)
    			db.close();
    	}
    }
    
    public void setBCForLeafVolume(){
    	synchronized (dbhelper) {
    		SQLiteDatabase db=null;
	    	try {
	    		db = dbhelper.getWritableDatabase();
	    		//set the total book number as the count of all books belongs to this v
	    		String where1 = "update " + DBHelper.TABLE_VOL + " set " + 
	    				DBHelper.COL_BOOKNUM + "=(select count(*) from " +
	    				DBHelper.TABLE_BOOK + 
	    				" where " + DBHelper.COL_CAT + "=" + DBHelper.TABLE_VOL+ "."+DBHelper.COL_ID + ")";
	    		db.execSQL(where1, new Object[]{});
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    	}finally{
	    		if (db!=null){
	    			db.close();
	    		}
	    	}  
    	}
    }
    
    private void setBCForNoneLeafVolume(String volumeId, SQLiteDatabase db){
    	synchronized (dbhelper) {
	    	try {
	    		String where= "exists (select * from volumes v2 where v2.pcat=volumes.id) and volumes.pcat=?";
	    		String[] whereValues = new String[1];
	    		whereValues[0]= volumeId;
	    		long vc = DatabaseUtils.queryNumEntries(db, DBHelper.TABLE_VOL,
	    				where, whereValues);
	    		if (vc!=0){
		    		Cursor cursor = db.query(DBHelper.TABLE_VOL, volColumns, 
		    					where, whereValues, null, null, null, null);
		    		if(cursor.moveToFirst()){
		    			do {
		    				Volume c = getVolume(cursor);
		    				setBCForNoneLeafVolume( c.getId(), db);
		    			} while(cursor.moveToNext());
		    		}
		    		if(cursor != null && !cursor.isClosed()){
		    			cursor.close();
		    		}
	    		}
	    		
    			//set the total book number as sum of all the v's total book num
	    		String where1 = "update " + DBHelper.TABLE_VOL + " set " + 
	    				DBHelper.COL_BOOKNUM + "=(select sum(" + DBHelper.COL_BOOKNUM + ") from " +
	    				DBHelper.TABLE_VOL + " where " + DBHelper.COL_PARENTCAT + "=?) where " +
	    				DBHelper.COL_ID + "=?";
	    		String[] where1Values= new String[]{volumeId, volumeId};
	    		db.execSQL(where1, where1Values);
	    		Log.i(TAG, "update for:" + volumeId);
    		
	    	}finally{
	    	}    	
    	}
    }
    
	/////////////////////
	//Book
	///////////////////////
    public long insertOrUpdateBook(Book book){
    	synchronized (dbhelper) {
    		return insertOrUpdateBook( book, null);
    	}
    }
	//-1 for failure
	public long insertOrUpdateBook(Book book, SQLiteDatabase outdb){
		synchronized (dbhelper) {
			SQLiteDatabase db;
			if (outdb==null)
				db= dbhelper.getWritableDatabase();
			else
				db = outdb;
	    	book.dataToJSON();
			ContentValues vals = new ContentValues();
			vals.put(DBHelper.COL_ID, book.getId());
			vals.put(DBHelper.COL_TYPE, book.getType());
			vals.put(DBHelper.COL_NAME, book.getName());
			vals.put(DBHelper.COL_TOTALPAGE, book.getTotalPage());
			vals.put(DBHelper.COL_LASTPAGE, book.getLastPage());
			String strCur = RemotePersistManager.SDF_SERVER_DTZ.format(new Date());
			vals.put(DBHelper.COL_UTIME, strCur);
			vals.put(DBHelper.COL_DATA, book.getData());
			vals.put(DBHelper.COL_CAT, book.getCat());
			vals.put(DBHelper.COL_READ, book.getRead());
			vals.put(DBHelper.COL_CACHED, book.getCached());
			vals.put(DBHelper.COL_INDEXPAGE, book.getIndexedPages());
			vals.put(DBHelper.COL_AUTHOR, book.getAuthor());
			vals.put(DBHelper.COL_STATUS, book.getState());
			
			
			try{
				return db.insertWithOnConflict(DBHelper.TABLE_BOOK, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
			}
			catch(Exception e){
				Log.e(TAG, e.toString(), e);
				return -1;
			}finally{
				if (outdb==null)
					db.close();
				else
					;//nothing
			}
		}
	}
    
    public long getBCByCat(String[] catId){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db = dbhelper.getReadableDatabase();
	    	try {
	    		long ret=0;
	    		String inSql = SqlUtil.generateInParameters(catId);
	    		ret = DatabaseUtils.queryNumEntries(db, DBHelper.TABLE_BOOK,
	    				DBHelper.COL_CAT + " in " + inSql , 
	    				catId);
	    		return ret;
	    	}finally{
	    		db.close();
	    	}    	
    	}
    }
    
    public long getBCByName(String name){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db = dbhelper.getReadableDatabase();
	    	try {
	    		String where= DBHelper.COL_NAME + " like '%" + name + "%'"; 
	    		return DatabaseUtils.queryNumEntries(db, DBHelper.TABLE_BOOK,
	    				where, null);
	    	}finally{
	    		db.close();
	    	}    	
    	}
    }
    
    public String[] bookColumns = new String[] {
		DBHelper.COL_ID, DBHelper.COL_TYPE, DBHelper.COL_NAME, DBHelper.COL_TOTALPAGE, 
		DBHelper.COL_LASTPAGE, DBHelper.COL_UTIME, DBHelper.COL_DATA, DBHelper.COL_CAT, 
		DBHelper.COL_READ, DBHelper.COL_CACHED, DBHelper.COL_INDEXPAGE, DBHelper.COL_AUTHOR, DBHelper.COL_STATUS};
    
    private Book getBook(Cursor cursor){
    	return new Book(
				cursor.getString(0), cursor.getInt(1), cursor.getString(2), cursor.getInt(3), 
				cursor.getInt(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), 
				cursor.getInt(8), cursor.getInt(9), cursor.getInt(10), cursor.getString(11), cursor.getInt(12));
    }
    private List<Book> getBooksBySQL(String where, String[] whereValues, SQLiteDatabase db, int offset, int limit){
    	ArrayList<Book> books = new ArrayList<Book>();
		String orderBy=DBHelper.COL_NAME;
		Cursor cursor = db.query(DBHelper.TABLE_BOOK, bookColumns, 
					where, whereValues, null, null, orderBy, getLimitRange(offset, limit));
		if(cursor.moveToFirst()){
			do {
				Book b = getBook(cursor);
				books.add(b);
			} while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()){
			cursor.close();
		}
		return books;
    }
    private List<Book> getBooksByName(String name, SQLiteDatabase db, int offset, int limit){    	
    	String where= DBHelper.COL_NAME + " like '%" + name + "%'"; 
    	return getBooksBySQL(where, null, db, offset, limit);
    }
    
    private List<Book> getBooksByCat(String[] catId, SQLiteDatabase db, int offset, int limit){
    	String inSql = SqlUtil.generateInParameters(catId);
    	String where=DBHelper.COL_CAT + " in " + inSql;
    	return getBooksBySQL(where, catId, db, offset, limit);
    }
    
    public List<Book> getBooksByName(String name, int offset, int limit){
    	synchronized (dbhelper) {
    		SQLiteDatabase db = null;	    	
	    	try {
	    		db = dbhelper.getReadableDatabase();
				return getBooksByName( name, db, offset, limit);
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    		return new ArrayList<Book>();
	    	}finally{
	    		if (db!=null)
	    			db.close();
	    	}
    	}
	}
    
    public List<Book> getBooksByCat(String[] catId, int offset, int limit){
    	synchronized (dbhelper) {
    		SQLiteDatabase db = null;	    	
	    	try {
	    		db = dbhelper.getReadableDatabase();
				return getBooksByCat( catId, db, offset, limit);
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
	    		return new ArrayList<Book>();
	    	}finally{
	    		if (db!=null)
	    			db.close();
	    	}
    	}
	}
    
    private Book getBookById(String id, SQLiteDatabase db){
    	Book b = null;
    	String where= DBHelper.COL_ID + "=?"; 
		String[] whereValues = new String[1];
		whereValues[0]= id;
    	Cursor cursor = db.query(DBHelper.TABLE_BOOK, bookColumns, 
								 where, whereValues, null, null, null);
		if(cursor.moveToFirst()){
			do {
				b = getBook(cursor);
				Log.d(TAG, "got book: " + b);
			} while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()){
			cursor.close();
		}
		return b;
    }
    
    public Book getBookById(String id){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db = null;
	    	try {
	    		db = dbhelper.getReadableDatabase();
		    	return getBookById(id, db);
	    	}finally{
	    		if (db!=null)
	    			db.close();
	    	}
    	}
    }
    
    public int deleteBook(String id){
    	synchronized (dbhelper) {
		  	SQLiteDatabase db = dbhelper.getWritableDatabase();
		  	try {
		    	String where= DBHelper.COL_ID + "=? "; 
				String[] whereValues = new String[1];
				whereValues[0]= id;
		    	int rows = db.delete(DBHelper.TABLE_BOOK, where, whereValues);
				return rows;
		  	}finally{
		  		db.close();
		  	}
    	}
	  }
    
    public void createBookAndPages(Book b, List<Page> pages){
    	synchronized (dbhelper) {
    		SQLiteDatabase db = dbhelper.getWritableDatabase();
		  	try {
				b.setUtime(new Date());
				Log.i(TAG, "book to create:" + b);
				
				insertOrUpdateBook(b, db);
		    	
				for (int i=0; i<pages.size(); i++){
					Page p = pages.get(i);
					insertOrUpdatePage(p, db);
				}	
		  	}finally{
		  		db.close();
		  	}
    	}
	}
	
    
    ////////////////////////////////////////////////////
    /// For pages
    ////////////////////////////
    public long insertOrUpdatePage(Page page){
    	synchronized (dbhelper) {
    		return insertOrUpdatePage(page, null);
    	}
    }
    //-1 for failure
  	public long insertOrUpdatePage(Page page, SQLiteDatabase outdb){
  		synchronized (dbhelper) {
	  		SQLiteDatabase db;
			if (outdb==null)
				db= dbhelper.getWritableDatabase();
			else
				db = outdb;
	      	page.dataToJSON();
	  		ContentValues vals = new ContentValues();
	  		vals.put(DBHelper.COL_ID, page.getBookid());
	  		vals.put(DBHelper.COL_PAGENUM, page.getPagenum());
	  		vals.put(DBHelper.COL_DATA, page.getData());
	  		String strCur = RemotePersistManager.SDF_SERVER_DTZ.format(new Date());
	  		vals.put(DBHelper.COL_UTIME, strCur);
	  		
	  		try{
	  			return db.insertWithOnConflict(DBHelper.TABLE_PAGE, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
	  		}catch(Exception e){
	  			Log.e(TAG, e.toString(), e);
	  			return -1;
	  		}finally{
	  			if (outdb==null)
					db.close();
				else
					;//nothing
	  		}
  		}
  	}
      
  	public Page getPage(String bookid, int pageNum){
	  synchronized (dbhelper) {
		SQLiteDatabase db = null;
	  	try {
	  		db = dbhelper.getReadableDatabase();
	    	Page p = null;
	    	String where= DBHelper.COL_ID + "=? and "+
					DBHelper.COL_PAGENUM + "=" + pageNum; 
			String[] whereValues = new String[1];
			whereValues[0]= bookid;
	    	Cursor cursor = db.query(DBHelper.TABLE_PAGE, 
					new String[] {DBHelper.COL_ID, DBHelper.COL_PAGENUM, 
	    							DBHelper.COL_DATA, DBHelper.COL_UTIME}, 
									 where, whereValues, null, null, null);
			if(cursor.moveToFirst()){
				do {
					p = new Page(
  							cursor.getString(0), cursor.getInt(1), 
  							cursor.getString(2), cursor.getString(3), true);;
					Log.d(TAG, "got page: " + p);
				} while(cursor.moveToNext());
			}
			if(cursor != null && !cursor.isClosed()){
				cursor.close();
			}
			return p;
	  	}catch(Exception e){
	  		Log.e(TAG, "exception in get page.", e);
	  		return null;
	  	}finally{
	  		if (db!=null)
	  			db.close();
	  	}
	  }
  	}
  
  	public List<Page> getPagesOfBook(String bookId){
  		synchronized (dbhelper) {
	  		SQLiteDatabase db = dbhelper.getReadableDatabase();
		  	try {
		    	List<Page> pages = new ArrayList<Page>();
		    	String where= DBHelper.COL_ID + "=? "; 
				String[] whereValues = new String[1];
				whereValues[0]= bookId;
		    	Cursor cursor = db.query(DBHelper.TABLE_PAGE, 
						new String[] {DBHelper.COL_ID, DBHelper.COL_PAGENUM, DBHelper.COL_DATA, DBHelper.COL_UTIME}, 
										 where, whereValues, null, null, null);
				if(cursor.moveToFirst()){
					do {
						Page p = new Page(
	  							cursor.getString(0), cursor.getInt(1), 
	  							cursor.getString(2), cursor.getString(3), true);;
						pages.add(p);
						Log.d(TAG, "got page: " + p);
					} while(cursor.moveToNext());
				}
				if(cursor != null && !cursor.isClosed()){
					cursor.close();
				}
				return pages;
		  	}finally{
		  		db.close();
		  	}
  		}
	  }
  	
  	public int deletePagesOfBook(String bookId){
  		synchronized (dbhelper) {
	  		SQLiteDatabase db = dbhelper.getWritableDatabase();
		  	try {
		    	String where= DBHelper.COL_ID + "=? "; 
				String[] whereValues = new String[1];
				whereValues[0]= bookId;
		    	int rows = db.delete(DBHelper.TABLE_PAGE, where, whereValues);
				return rows;
		  	}finally{
		  		db.close();
		  	}
  		}
	 }	
  	
  	//////////////////////
  	////package
  	//////////////////////
  	public long deletePackage(BPackage pkg){  	
  		synchronized (dbhelper) {
		  	SQLiteDatabase db = dbhelper.getWritableDatabase();
		  	try {
		    	String where= DBHelper.COL_NAME + "=? "; 
				String[] whereValues = new String[1];
				whereValues[0]= pkg.getName();
		    	int rows = db.delete(DBHelper.TABLE_PKG, where, whereValues);
				return rows;
		  	}finally{
		  		db.close();
		  	}
    	}
  	}
  	//-1 for failure
  	public long addPackage(BPackage pkg){  		
  		synchronized (dbhelper) {
	  		SQLiteDatabase db =dbhelper.getWritableDatabase();
			ContentValues vals = new ContentValues();
	  		vals.put(DBHelper.COL_NAME, pkg.getName());
	  		vals.put(DBHelper.COL_SIZE, pkg.getSize());
	  		vals.put(DBHelper.COL_PTIME, pkg.getPtime());
	  		String strCur = RemotePersistManager.SDF_SERVER_DTZ.format(new Date());
	  		vals.put(DBHelper.COL_ITIME, strCur);
	  		
	  		try{
	  			return db.insertWithOnConflict(DBHelper.TABLE_PKG, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
	  		}catch(Exception e){
	  			Log.e(TAG, e.toString(), e);
	  			return -1;
	  		}finally{
				db.close();
	  		}
  		}
  	}
  	
  	//order by name
  	public HashMap<String, BPackage> getAllPackages(String orderByCol){
    	synchronized (dbhelper) {
	    	SQLiteDatabase db = dbhelper.getReadableDatabase();
	    	try {
				HashMap<String, BPackage> pkgs = new HashMap<String, BPackage>();
				String orderBy = orderByCol;
				Cursor cursor = db.query(DBHelper.TABLE_PKG, 
						new String[] {
							DBHelper.COL_NAME, DBHelper.COL_SIZE, 
							DBHelper.COL_PTIME, DBHelper.COL_ITIME}, 
							null, null, null, null, orderBy);
				if(cursor.moveToFirst()){
					do {
						BPackage pkg = new BPackage(
								cursor.getString(0), cursor.getInt(1),  
								cursor.getString(2), cursor.getString(3));
						Log.i(TAG, "got package: " + pkg);
						pkgs.put(pkg.getName(), pkg);
					} while(cursor.moveToNext());
				}
				if(cursor != null && !cursor.isClosed()){
					cursor.close();
				}
				Log.d(TAG, "total packages loaded: " + pkgs.size());
				return pkgs;
	    	}finally{
	    		db.close();
	    	}
    	}
	}
  	
  	//////////////
  	//My Readings
  	//////////////
	private String makePlaceholders(int len) {
	    if (len < 1) {
	        // It will lead to an invalid query anyway ..
	        throw new RuntimeException("No placeholders");
	    } else {
	        StringBuilder sb = new StringBuilder(len * 2 - 1);
	        sb.append("?");
	        for (int i = 1; i < len; i++) {
	            sb.append(",?");
	        }
	        return sb.toString();
	    }
	}

	@Override
	public int addMyReadings(List<String> ids) {
		synchronized (dbhelper) {
	    	SQLiteDatabase db = dbhelper.getWritableDatabase();
	    	try {
	    		for (String id: ids){
		    		ContentValues vals = new ContentValues();
					vals.put(DBHelper.COL_ID, id);				
					db.insertWithOnConflict(DBHelper.TABLE_MY_READING, 
							null, vals, SQLiteDatabase.CONFLICT_REPLACE);
	    		}
	    		return ids.size();
	    	}catch(Exception e){
	    		Log.e(TAG, "", e);
			}finally{
	    		db.close();
	    	}
		}
		return 0;
	}
	
	@Override
	public List<String> getMyReadingsIn(List<String> ids) {
		synchronized (dbhelper) {
			SQLiteDatabase db=null;
			try{
				List<String> retIdList =new ArrayList<String>();
		    	db = dbhelper.getReadableDatabase();
				String where= DBHelper.COL_ID + " in (" + makePlaceholders(ids.size())+ ")"; 
				String[] whereValues = new String[ids.size()];
				whereValues = ids.toArray(whereValues);
				Cursor cursor = db.query(DBHelper.TABLE_MY_READING, null, 
							where, whereValues, null, null, null, null);
				if(cursor.moveToFirst()){
					do {
						String id = cursor.getString(1);
						retIdList.add(id);
					} while(cursor.moveToNext());
				}
				
				if(cursor != null && !cursor.isClosed()){
					cursor.close();
				}
				return retIdList;
			}finally{
				if (db!=null){
					db.close();
				}
			}
		}	
	}
	
	private int getMyVolumesCountLike(String param, SQLiteDatabase db) {
		String sql = null;
		String[] whereValues = new String[]{};
		if ("".equals(param) || param==null){
			sql= "select count(*) from volumes, myreadings where volumes.id = myreadings.rid";
		}else{
			sql= "select count(*) from volumes, myreadings where volumes.id = myreadings.rid "
					+ "and (volumes.name like ? or volumes.author like ?)";
			whereValues = new String[]{"%"+param+"%", "%"+param+"%"};
		}
		Cursor cursor = db.rawQuery(sql, whereValues);
		int count=0;
		if (cursor!=null){
			if(cursor.getCount() > 0){
		      cursor.moveToFirst();    
		      count = cursor.getInt(0);
		    }
		    cursor.close();
		}
		return count;
	}
	
	private List<Volume> getMyVolumesLike(String param, SQLiteDatabase db, int offset, int limit) {
		String sql= "select " + DBHelper.allVolumeDBFields + " from volumes, myreadings " +
				"where volumes.id = myreadings.rid and (volumes.name like ? or volumes.author like ?) order by name " +
				getLimitFullString(offset, limit);
		String[] whereValues = new String[]{"%"+param+"%", "%"+param+"%"};
		Cursor cursor = db.rawQuery(sql, whereValues);
		List<Volume> vl = new ArrayList<Volume>();
		if(cursor.moveToFirst()){
			do {
				Volume v = getVolume(cursor);
				Log.i(TAG, "got cat: " + v);
				vl.add(v);
			} while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()){
			cursor.close();
		}
		return vl;
	}
	
	private int getMyBooksCountLike(String param, SQLiteDatabase db){
		String sql=null;
		String[] whereValues = new String[]{};
		if (param==null ||"".equals(param)){
			sql= "select count(*) from books, myreadings where books.id = myreadings.rid";
		}else{
			sql= "select count(*) from books, myreadings where books.id = myreadings.rid and books.name like ?";
			whereValues = new String[]{"%" + param + "%"};
		}
		Cursor cursor = db.rawQuery(sql, whereValues);
		int count=0;
		if (cursor!=null){
			if(cursor.getCount() > 0){
		      cursor.moveToFirst();    
		      count = cursor.getInt(0);
		    }
		    cursor.close();
		}
		return count;
	}
	
	private List<Book> getMyBooksLike(String param, SQLiteDatabase db, int offset, int limit){
		String sql = null;
		String[] whereValues = new String[]{};
		if (param==null ||"".equals(param)){
			sql= 
			"select " + DBHelper.allBookDBFields +
			"where books.id = myreadings.rid order by name " + getLimitFullString(offset, limit);
		}else{
			sql= 
			"select " + DBHelper.allBookDBFields + 
			"where books.id = myreadings.rid and books.name like ? order by name " + getLimitFullString(offset, limit);
			whereValues = new String[]{"%" + param + "%"};
		}
		
		Cursor cursor = db.rawQuery(sql, whereValues);
		List<Book> bl = new ArrayList<Book>();
		if(cursor.moveToFirst()){
			do {
				Book b = getBook(cursor);
				Log.i(TAG, "got book: " + b);
				bl.add(b);
			} while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()){
			cursor.close();
		}
		return bl;
    }
	
	@Override
	public List<Reading> getMyReadingsLike(String param, int offset, int limit){
		synchronized (dbhelper) {
			SQLiteDatabase db=null;
			try{
				db = dbhelper.getReadableDatabase();
				List<Reading> rl = new ArrayList<Reading>();
				rl.addAll(getMyVolumesLike(param, db, offset, limit));
				rl.addAll(getMyBooksLike(param, db, offset, limit));
				return rl;				
			}finally{
				if (db!=null){
					db.close();
				}
			}
		}	
	}
	
	public int getMyReadingsCountLike(String param){
		synchronized (dbhelper) {
			SQLiteDatabase db=null;
			try{
				db = dbhelper.getReadableDatabase();
				int count=0;
				count+=getMyVolumesCountLike(param, db);
				count+=getMyBooksCountLike(param, db);
				return count;
			}finally{
				if (db!=null){
					db.close();
				}
			}
		}	
	}
	
	@Override
	public int deleteMyReadings(List<String> ids){
		synchronized (dbhelper) {
		  	SQLiteDatabase db = dbhelper.getWritableDatabase();
		  	try {
		  		String where= DBHelper.COL_ID + " in (" + makePlaceholders(ids.size())+ ")"; 
				String[] whereValues = new String[ids.size()];
				whereValues = ids.toArray(whereValues);
		    	int rows = db.delete(DBHelper.TABLE_MY_READING, where, whereValues);
				return rows;
		  	}catch(Exception e){
		  		Log.e(TAG, "", e);
		  		return -1;
			}finally{
		  		db.close();
		  	}
		}
	}

}
