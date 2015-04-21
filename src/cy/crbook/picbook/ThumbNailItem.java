package cy.crbook.picbook;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class ThumbNailItem{
	//thumb-nail view
	public ImageView mainImage;
	public boolean readImageVisible;
	public boolean downloadImageVisible;
	
	
	public int pos;
	public String name;
	public boolean isVolume;
	
	public ThumbNailItem(){		
	}
	
	ThumbNailItem(int pos, String name, ImageView mainImage, boolean readImageVisible, 
			boolean downloadImageVisible, boolean isVolume){
		this.pos = pos;
		this.name = name;
		this.mainImage = mainImage;
		this.readImageVisible = readImageVisible;
		this.downloadImageVisible = downloadImageVisible;
		this.isVolume = isVolume;
	}
}
	