package cy.crbook.util;

import org.cld.util.PatternResult;

import cy.common.entity.Book;
import cy.common.entity.Page;
import cy.common.entity.Reading;

public class CRBookUtils {
	
	public static String getPageBgUrlDirectly(Book b, Page p, int pageNum){
		PatternResult pattern = b.getPageBgUrlPattern();
		if (pattern!=null){
			return PatternResult.guessUrl(pattern, pageNum-1);
		}else{
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
	
	public static String getBookCachePath(Reading r){
		//r.rootId/r.Id
		return r.getId();
	}
}
