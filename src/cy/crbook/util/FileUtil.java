package cy.crbook.util;

import java.io.File;
import java.io.IOException;

import android.content.res.AssetManager;
import android.util.Log;

public class FileUtil {

	private static final String TAG = "FileUtil";

	//
	public static void walk(String path){
		File root = new File(path);
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath() );
                Log.d(TAG, "Dir:" + f.getAbsoluteFile() );
            }
            else {
                Log.d(TAG, "File:" + f.getAbsoluteFile() );
            }
        }
	}
	
	public static void walk(AssetManager am, String path) {
	    String assets[] = null;
	    
        try {
			assets = am.list(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (assets.length ==  0) {
            Log.w(TAG, "xx:" + path);
        } else {
            for (int i = 0; i < assets.length; ++i) {
                walk(am, path + "/" + assets[i]);
            }
        }
    
	}

}
