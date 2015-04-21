package cy.crbook.picbook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import org.cld.util.PatternResult;
import org.json.JSONArray;

import cy.cfs.CallbackOp;
import cy.common.entity.Book;
import cy.common.entity.EntityUtil;
import cy.common.entity.Page;
import cy.common.entity.Stroke;
import cy.common.persist.LocalPersistManager;
import cy.crbook.CRApplication;
import cy.crbook.util.CRBookUtils;
import cy.crbook.util.ImageUtil;



import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;

public class PageEditView extends PageCurlView implements CallbackOp{
	
	private static final String TAG = "PageEditView";
	private static final int ERASE_COLOR = 0x000000;
    private static final int ERASE_WIDTH = 30;
    public static final int VIRTUAL_WIDTH = 768;
    
    private boolean mIsDirty = false; // Updated since load?
	private int currentColor = 0x56A5EC;
	private int currentWidth = 13;
	
	private boolean eraseMode = false;	
	private int runMode = -1;
	private int sub1Mode = -1;
	
    private int localWidth = 0;
    private int localHeight = 0;

    final Random mRandom = new Random();
    
    private Page realPage;
    private SparseArray<List<Integer>> currentStrokes = new SparseArray<List<Integer>>();
    private List<Stroke> answerStrokes;
    private List<Stroke> questionStrokes;
    
    private int pageNum;
    private int nextPageNum;
    private int prevPageNum;
    private Book book = null;
    
    private Bitmap bgBMP = null;
    private BitmapShader fillBMPshader = null;
    private Bitmap nextBgBMP = null;
    private Bitmap prevBgBMP = null;
	
	private PageEditActivity pea;
	private LocalPersistManager localPersistManager;
	private CRApplication myApp;
	
	public void setPEA(PageEditActivity pageEditActivity) {
		pea = pageEditActivity;	
		localPersistManager = pea.getLocalPersistManager();
	}

    public PageEditView(Context context, AttributeSet attrs){
    	super(context, attrs);
    	myApp = (CRApplication) context.getApplicationContext();
    }
    
    public Page getPage(){
    	return realPage;
    }
    
    public int getPageNum(){
    	return pageNum;
    }
    
    public Book getBook(){
    	return book;
    }
    
    @Override
    public void turnPage(int direction){
    	setupPage(pageNum, book, direction);
    }
	
    @SuppressLint("NewApi")
	private void mySetBackGround(final Drawable bmp){
    	if (pea!=null){
    		//check added for ui editor
	    	pea.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					int sdk = android.os.Build.VERSION.SDK_INT;
			    	if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			    	    setBackgroundDrawable(bmp);
			    	} else {
			    	    setBackground(bmp);
			    	}				
				}
	    	});
    	}
    }
    
    private LinkedHashMap<Integer, Bitmap> bitmapCache = new LinkedHashMap<Integer, Bitmap>();
	int cacheMaxSize=3;
	private Bitmap getBitmapFromCache(int pn){
		if (bitmapCache.containsKey(pn)){
			return bitmapCache.get(pn);
		}else{
			return null;
		}
	}
	private void putBitmapToCache(int pn, Bitmap bmp){
		bitmapCache.put(pn, bmp);
		if (bitmapCache.size()>cacheMaxSize){
			int firstpn = bitmapCache.keySet().iterator().next();
			bitmapCache.remove(firstpn);
		}
	}
	
    //direction: pre-fetch directions, next, prev, both or none
	public void setupPage(int pn, Book b, int direction){
    	this.pageNum = pn;
		this.book = b;
		this.nextPageNum=EntityUtil.getNextPage(book, pageNum);			
		this.prevPageNum=EntityUtil.getPreviousPage(book, pageNum);
		

		
		//set current bgBMP
		if (direction == PageCurlView.NEXT_PAGE){			
			//turn next page to here, so current becomes previous, and next become current
			prevPageNum = pageNum;
			prevBgBMP = bgBMP;
			super.setUpPrev(prevBgBMP);
			//for current
			pageNum = nextPageNum;
			bgBMP = nextBgBMP;
			super.setUpCurrent(bgBMP);
			//for next
			nextPageNum=EntityUtil.getNextPage(book, pageNum);
		}else if (direction == PageCurlView.PREV_PAGE){			
			//turn previous page to here, so current becomes next, and previous become current
			nextPageNum = pageNum;
			nextBgBMP = bgBMP;
			super.setUpCurrent(bgBMP);
			//for current
			pageNum = prevPageNum;
			bgBMP = prevBgBMP;
			super.setUpNext(nextBgBMP);
			//for prev
			prevPageNum=EntityUtil.getPreviousPage(book, pageNum);
		}else{
			//jump direct here, so need to load current first
			loadPage(this.pageNum, localWidth, localHeight);
			this.pageNum = pn;
			this.nextPageNum=EntityUtil.getNextPage(book, pageNum);			
			this.prevPageNum=EntityUtil.getPreviousPage(book, pageNum);	
		}
		
		pea.setBookPageNum(pageNum);
		
		//get or create realpage object
		realPage = localPersistManager.getPage(b.getId(), pageNum);
		if (realPage==null){
			realPage = new Page();
			realPage.setBookid(b.getId());
			realPage.setPageNum(pageNum);
			if (b.getPageBgUrlPattern()!=null){				
				realPage.setBackgroundUri(PatternResult.guessUrl(b.getPageBgUrlPattern(), pageNum-1));
			}else{
				Log.e(TAG, "page does not have an backgroud url");
			}
		}
		
		this.answerStrokes=realPage.getAnswerStrokes();
		this.questionStrokes = realPage.getQuestionStrokes();
		
		if (localWidth!=0 && localHeight!=0){
			updateBackground(direction,localWidth, localHeight);
			(new AsyncTask<Void, Void, Void>(){
				@Override
				protected Void doInBackground(Void... params) {
					loadPage(prevPageNum, localWidth, localHeight);
					loadPage(nextPageNum, localWidth, localHeight);				
					return null;
				}				
			}).execute();
		}
		postInvalidate();
	}
    
	private void setCurrentBg(){
		Log.w(TAG, "get image: width:" + localWidth + ", height:" + localHeight);
    	if (bgBMP!=null){	
    		putBitmapToCache(pageNum, bgBMP);
			Drawable backgroundDrawable = new BitmapDrawable(getResources(), bgBMP);
			mySetBackGround(backgroundDrawable);
		    fillBMPshader = new BitmapShader(bgBMP, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
		}else{
			fillBMPshader = null;
			mySetBackGround(null);
		}
	}
	
	//called back in another thread, the main page may be changed so next, 
	//prev may not be the real next and prev for the current main page
	@Override
	public void onSuccess(Object ppParam, Object downloadedItem) {
		BgImgParam param = (BgImgParam)ppParam;
		Bitmap bmp = (Bitmap)downloadedItem;		
		if (param.bookId.equals(book.getId())){
			int pn = param.pageNum;
			if (pn == pageNum){
				bgBMP = bmp;
				super.setUpCurrent(bmp);
				setCurrentBg();
				postInvalidate();
				putBitmapToCache(pn, bmp);
			}else if (pn == this.nextPageNum){
				nextBgBMP=bmp;
				super.setUpNext(bmp);
				putBitmapToCache(pn, bmp);
			}else if (pn == this.prevPageNum){
				prevBgBMP=bmp;
				super.setUpPrev(bmp);
				putBitmapToCache(pn, bmp);
			}
		}
	}
	@Override 
	public void onFailure(Object request, Object response){
		
	}
	
	/**
	 * @param pn: page num to preload
	 * @param dippKey
	 */
	private void loadPage(int pn, int width, int height){
		Bitmap bmp = null;
		bmp = getBitmapFromCache(pn); 
		if (bmp!=null){
			onSuccess(new BgImgParam(pn, book.getId()), bmp);
			return;
		}
				
		if (width!=0 && height!=0){
			String pageUrl = localPersistManager.getPageBgUrl(book, pn);
			ImageUtil.getImageFromUrl(pageUrl, book, pn, this, new BgImgParam(pn, book.getId()), 
					myApp, width, height, true);
		}
	}
	
	private void updateBackground(int direction, int width, int height){
		//
		bgBMP = this.getBitmapFromCache(pageNum);
		if (bgBMP==null){
			//
			String pageUrl = null;
			if (realPage!=null){
				pageUrl = CRBookUtils.getPageBgUrlDirectly(book, realPage, pageNum);
			}else{
				if (localPersistManager!=null){
					//check added for ui editor
					pageUrl = localPersistManager.getPageBgUrl(book, pageNum);
				}
			}
			if (book!=null){
				ImageUtil.getImageFromUrl(pageUrl, book, pageNum, this, new BgImgParam(pageNum, book.getId()), 
						myApp, width, height, true);
			}
		}else{
			setCurrentBg();
		}
		
		super.setUpCurrent(bgBMP);	
	}
	
	boolean cancelLoadBook=false;
	public void cancelLoadBook(){
		this.cancelLoadBook=true;
	}
	
	private void loadBook(Book b, int width, int height){
		/*ConnectivityManager connManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) {					
			for (int i=1; i<=b.getTotalPage(); i++){
				if (!cancelLoadBook){
					String url =localPersistManager.getPageBgUrl(b, i);
					if (url!=null){
						String fileKey = FileCache.generateKey(false, b, i);
						File f = new File(fileKey);
						if (!f.exists()){
							DownloadImageJobManager.singleton.submitDownloadImageJob(
									this, new BgImgParam(i, book.getId()), getContext(), 
									b.getId(), fileKey, width, height, url, 
									CRApplication.getTemplate(b.getId()).getReferer(), false);
						}
					}
				}
			}		
		}*/
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int newlocalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int newlocalHeight = MeasureSpec.getSize(heightMeasureSpec);
        int orient = getContext().getResources().getConfiguration().orientation;
    	
    	//for portrait orientation, we need to make sure the width < height
    	if (orient == Configuration.ORIENTATION_PORTRAIT){    		
			if (newlocalWidth > newlocalHeight){
				int t = newlocalWidth;
				newlocalWidth = newlocalHeight;
				newlocalHeight = t;
			}
    	}        
    	if ((localWidth != newlocalWidth && newlocalWidth!=0 || 
    			localHeight != newlocalHeight && newlocalHeight!=0)){
    		localWidth = newlocalWidth;
    		localHeight = newlocalHeight;
    		updateBackground(PageCurlView.BOTH_PAGE, localWidth, localHeight);
    		if (book != null && localWidth!=0 && localHeight!=0){
    			(new AsyncTask<Void, Void, Void>(){
					@Override
					protected Void doInBackground(Void... params) {
						loadBook(book, localWidth, localHeight);
						return null;
					}
    			}).execute();
    		}
    	}
    }
    
    public int getCurrentColor(){
    	return currentColor;
    }    
    public void setCurrentColor(int color){
    	this.currentColor = color;
    }
    
    public int getCurrentWidth(){
    	return currentWidth;
    }    
    public void setCurrentWidth(int width){
    	this.currentWidth = width;
    }
    
    public boolean isDirty(){
    	return mIsDirty;
    }    
    public void setIsDirty(boolean isDirty){
    	mIsDirty = isDirty;
    }
    
    public boolean isEraseMode(){
    	return eraseMode;
    }    
    public void setEraseMode(boolean eraseMode){
    	this.eraseMode = eraseMode;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);    	
    	paintState(canvas, runMode);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

		if (sub1Mode==PageEditActivity.MODE_BROWSE){
			return super.onTouchEvent(ev);
		}    	
    	
    	if (sub1Mode == PageEditActivity.MODE_DRAW){  	
	    	int action = ev.getAction();        
	        
	        switch (action & MotionEvent.ACTION_MASK) {	        	
	            case MotionEvent.ACTION_DOWN: 
	            case MotionEvent.ACTION_POINTER_DOWN:{
	            	final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
	                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	                final int pointerId = ev.getPointerId(pointerIndex);
	                startStroke(pointerId, pointerIndex, ev);
	                Log.w(TAG, "action down. id:" + pointerId + ", idx:" + pointerIndex);
	                break;
	            }
	            case MotionEvent.ACTION_UP: 
	            case MotionEvent.ACTION_POINTER_UP: {
	            	final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
	                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	    	        final int pointerId = ev.getPointerId(pointerIndex);
	                finishStroke(pointerId, pointerIndex, ev, runMode);
	                mIsDirty = true;
	                Log.w(TAG, "action up. id:" + pointerId + ", idx:" + pointerIndex);
	                break;
	            }
	            case MotionEvent.ACTION_MOVE: {
	                updateStrokes(ev);
	                break;
	            }
	            case MotionEvent.ACTION_CANCEL: {
	                Log.d(TAG, "Cancelled a gesture");
	                currentStrokes.clear();
	                break;
	            }
	        }
	        invalidate();
	        return true;
    	}else{
    		return false;
    	}
    }

    void updateStrokes(MotionEvent ev) {
        final int historySize = ev.getHistorySize();
        final int pointerCount = ev.getPointerCount();
        for (int p = 0; p < pointerCount; p++) {
            int id = ev.getPointerId(p);
            List<Integer> currentPoints = currentStrokes.get(id);
            for (int h = 0; h < historySize; h++) {
                currentPoints.add(localToVirt(ev.getHistoricalX(p, h)));
                currentPoints.add(localToVirt(ev.getHistoricalY(p, h)));
            }
        }

        for (int p = 0; p < pointerCount; p++) {
            int id = ev.getPointerId(p);
            List<Integer> currentPoints = currentStrokes.get(id);
            currentPoints.add(localToVirt(ev.getX(p)));
            currentPoints.add(localToVirt(ev.getY(p)));
        }
    }

	private String padToSixChars(String s){
		while(s.length() < 6) s = "0" + s;
		return s;
	}
	
    public Stroke newStroke(int color, int width, List<Integer> points){
		Stroke stroke = new Stroke();
		
		stroke.setId(mRandom.nextInt());
		stroke.setColor("#" + padToSixChars(Integer.toHexString(color)));
		stroke.setWidth(width);
		int[] ipoints = new int[points.size()];
		for(int i=0; i<points.size(); i++){
			ipoints[i]=points.get(i).intValue();
		}
		stroke.setPoints(ipoints);
		return stroke;
	}
    
    private void startStroke(int pointerId, int pointerIndex, MotionEvent ev) {
    	List<Integer> stroke = currentStrokes.get(pointerId);
        if (stroke==null) {
            currentStrokes.put(pointerId, new ArrayList<Integer>());
        } else {
            currentStrokes.get(pointerId).clear();
        }
        currentStrokes.get(pointerId).add(localToVirt(ev.getX(pointerIndex)));
        currentStrokes.get(pointerId).add(localToVirt(ev.getY(pointerIndex)));
    }

    private void finishStroke(int pointerId, int pointerIndex, MotionEvent ev, int mode) {
        List<Integer> stroke = currentStrokes.get(pointerId);
        if (stroke == null) {
            return;
        }
        if (stroke.size() <= 4) {
            // fun effect :)
            //stroke.add(0);
            //stroke.add(0);

            int x = localToVirt(ev.getX(pointerIndex));
            int y = localToVirt(ev.getY(pointerIndex));

            stroke.add(x - 1);
            stroke.add(y - 1);

            stroke.add(x + 1);
            stroke.add(y + 1);
        } else {
            stroke.add(localToVirt(ev.getX(pointerIndex)));
            stroke.add(localToVirt(ev.getY(pointerIndex)));
        }
        addStroke(stroke, mode);
        
        stroke.clear();
    }

    private void addStroke(List<Integer> stroke, int mode) {
        int color;
        int width;
        
        if (eraseMode) {
            color = ERASE_COLOR;
            width = ERASE_WIDTH;
        } else {
            color = currentColor;
            width = localToVirt(currentWidth);
        }
        
        Stroke obj = newStroke(color, width, stroke);
        if (mode == PageEditActivity.TEACHER_MODE){
        	questionStrokes.add(obj);
        }else if (mode == PageEditActivity.STUDENT_MODE){
        	answerStrokes.add(obj);
        }
    }

	protected void paintState(final Canvas canvas, int mode){
		
		final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setDither(true);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		List<Stroke> strokes=null;
		if (mode == PageEditActivity.STUDENT_MODE){
			strokes = answerStrokes;
		}else if (mode == PageEditActivity.TEACHER_MODE){
			strokes = questionStrokes;
		}else{
			Log.e(TAG, "run mode not supported:" + mode);
		}
		// paint prop state
		if (strokes != null){
	        for (Stroke o : strokes) {
	            int color = Integer.parseInt(o.getColor().substring(1), 16);
	            if (color == ERASE_COLOR){
	            	mPaint.setShader(fillBMPshader);
	            }else{
	            	mPaint.setShader(null);
	            }
	            mPaint.setColor(0xFF000000 | color);
	        	mPaint.setStrokeWidth(virtToLocal(o.getWidth()));
	            int[] points = o.getPoints();
	            paintStroke(canvas, mPaint, points);
	        }
		}
        
		paintCurrentStrokes(canvas);
	}

    protected void paintCurrentStrokes(Canvas canvas) {
        Paint paint = new Paint();
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        // Draw stroke-in-progress
        if (eraseMode) {
            paint.setColor(0x08000000 | ERASE_COLOR);//transparent
            paint.setStrokeWidth(ERASE_WIDTH);
        } else {
            paint.setColor(0xFF000000 | currentColor);
            paint.setStrokeWidth(virtToLocal(localToVirt(currentWidth)));
        }
        
        for(int i = 0; i < currentStrokes.size(); i++) {
        	List<Integer> points = currentStrokes.valueAt(i);
        	int[] ipoints = new int[points.size()];
        	for (int j=0; j<ipoints.length; j++){
        		ipoints[j]=points.get(j).intValue();
        	}
        	paintStroke(canvas, paint, ipoints);
        }
    }

	protected void paintStroke(Canvas canvas, Paint paint, int[] points){
		if(points.length >= 4){
			int x1, y1, x2, y2;
			x1 = virtToLocal(points[0]);
			y1 = virtToLocal(points[1]);
			for(int i = 2; i < points.length; i += 2) {
				x2 = virtToLocal(points[i]);
				y2 = virtToLocal(points[i+1]);
				canvas.drawLine(x1,y1,x2,y2, paint);
				x1 = x2;
				y1 = y2;
			}
		}
	}

	protected void paintStroke(Canvas canvas, Paint paint, JSONArray points){
		 
		if(points.length() >= 4){
			int x1, y1, x2, y2;
			x1 = virtToLocal(points.optInt(0));
			y1 = virtToLocal(points.optInt(1));
			for(int i = 2; i < points.length(); i += 2) {
				x2 = virtToLocal(points.optInt(i));
				y2 = virtToLocal(points.optInt(i+1));
				canvas.drawLine(x1,y1,x2,y2, paint);
				x1 = x2;
				y1 = y2;
			}
		}
	}
	
	private int virtToLocal(float val){
		float ratio = (float)localWidth/(float)VIRTUAL_WIDTH;
		return (int)(val * ratio);
	}

	private int virtToLocal(int val){
		return virtToLocal((float)val);
	}

	private int localToVirt(float val){
		float ratio = (float)VIRTUAL_WIDTH/(float)localWidth;
		return (int)(val * ratio);
	}

	private int localToVirt(int val){
		return localToVirt((float)val);
	}
	
	public int getSub1Mode() {
		return sub1Mode;
	}
	public void setSub1Mode(int sub1Mode) {
		this.sub1Mode = sub1Mode;
	}
	
	public int getRunMode() {
		return runMode;
	}
	public void setRunMode(int runMode) {
		this.runMode = runMode;
	}
}