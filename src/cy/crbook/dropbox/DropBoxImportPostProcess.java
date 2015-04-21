package cy.crbook.dropbox;

import java.util.List;

import com.dropbox.client2.DropboxAPI.Entry;

public interface DropBoxImportPostProcess {
	/**
	 * 
	 */
	public void importPostProcess(List<Entry> entries);
}
