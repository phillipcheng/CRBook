package cy.crbook.util;

import java.util.List;

import com.dropbox.client2.DropboxAPI.Entry;

public interface ImportPostProcess {
	/**
	 * 
	 */
	public void importPostProcess(List<Entry> entries);
}
