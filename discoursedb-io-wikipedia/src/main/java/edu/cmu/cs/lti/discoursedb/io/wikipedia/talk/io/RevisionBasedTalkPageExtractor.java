package edu.cmu.cs.lti.discoursedb.io.wikipedia.talk.io;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiInitializationException;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiPageNotFoundException;
import de.tudarmstadt.ukp.wikipedia.revisionmachine.api.Revision;
import de.tudarmstadt.ukp.wikipedia.revisionmachine.api.RevisionApi;
import edu.cmu.cs.lti.discoursedb.io.wikipedia.talk.model.TalkPage;
import edu.cmu.cs.lti.discoursedb.io.wikipedia.talk.util.RevisionUtils;

/**
 * Retrieves and segments Talk page discussions for a given list of articles.
 * Archived discussions are retrieved from the revision history of the main Talk page using 
 * the creation times of the archive pages as jump marks into the revision history.
 * i.e. the algorithm segments the current page and then jumps to the revisions before each archiving event.
 * Due to partial archiving, this could result in duplicate discussion threads. Duplicates are therefore
 * filtered based on the discussion title.
 *  
 * 
 * @author Oliver Ferschke
 *
 */
public class RevisionBasedTalkPageExtractor {

	private static final Logger logger = LogManager.getLogger(RevisionBasedTalkPageExtractor.class);
	
	private Wikipedia wiki;
	private RevisionApi revApi;
	private List<Page> sourcePages;
	private List<TalkPage> talkPages;
	private boolean includeArchives;
	
	public RevisionBasedTalkPageExtractor(Wikipedia wiki, String pageTitle, boolean includeArchives) throws WikiInitializationException, WikiApiException{
		this.wiki =wiki;
		this.includeArchives=includeArchives;
		revApi = new RevisionApi(wiki.getDatabaseConfiguration());
		
		sourcePages = new ArrayList<>();
		talkPages = new ArrayList<>();
		try {
			sourcePages.add(wiki.getPage(pageTitle));
		} catch (WikiPageNotFoundException e) {
			logger.warn("Could not find article " + pageTitle);
		}
		loadTalkPages();
		
	}

	public RevisionBasedTalkPageExtractor(Wikipedia wiki, Collection<String> pageTitles, boolean includeArchives) throws WikiInitializationException, WikiApiException{
		this.wiki =wiki;
		this.includeArchives=includeArchives;
		revApi = new RevisionApi(wiki.getDatabaseConfiguration());

		sourcePages = new ArrayList<>();
		talkPages=new ArrayList<>();
		logger.trace("Loading "+pageTitles.size()+" pages....");
		for(String title:pageTitles){
			try{
				sourcePages.add(wiki.getPage(title));
			}catch(WikiPageNotFoundException e){
				logger.warn("Could not find article "+title);
			}
		}
		logger.trace("done.");
		loadTalkPages();
		
	}
		
	private void loadTalkPages() throws WikiApiException{
		for(Page p:sourcePages){
			try{
				Set<Revision> curTPrevs = getTalkPageRevisions(p);	
				for(Revision curRev:curTPrevs){
					try{
						talkPages.add(new TalkPage(revApi, curRev, false));					
					}catch(Exception e){
						logger.warn("Could not load TalkPage for revision "+curRev.getRevisionID());
					}
				}				
			}catch(Exception e){
				logger.warn("Could not load Talk pages for article "+p.getTitle().getPlainTitle());				
			}
		}
	}
	
			
	/**
	 * If <code>includeArchives</code> is true, then do:<br/>
	 * For the given Talk page, get the the revisions of the main talk page before each time it was archived. <br/>
 	 * (The list includes the newest revision as well.)
	 * This approach assumes that the pages was archived in full and that the revision history remains part of the original page.<br/>
	 * This assumption is not always true!<br/>
	 * <br/>
	 * If <code>includeArchives</code> is false, only retrieve the newest rev of the current Talk page.<br/>
	 * <br/>
	 * TODO partial archiving could be addressed by detecting bot commit comments<br/>
	 * TODO detect archiving strategies that move the revision history to the archive page and deal with this case
	 *  
 	 * 
	 * @param p article page
	 * @return list of pre-archive revisions
	 * 
	 */
	private Set<Revision> getTalkPageRevisions(Page p) throws WikiApiException{
		Set<Revision> preArchiveRevisions = new HashSet<>();
		Set<Integer> preArchiveRevIds = new HashSet<>();

		logger.info("Identifying pre-archive snapshots of the Talk page for article "+p.getTitle().getPlainTitle());
		Page mainDiscussionPage = wiki.getDiscussionPage(p);
		int mainDiscussionPageId = mainDiscussionPage.getPageId();
		Iterable<Page> discArchives = wiki.getDiscussionArchives(p);
		
		//add main discussion page
		Revision mainDiscPageLatestRev = revApi.getRevision(mainDiscussionPageId,revApi.getLastDateOfAppearance(mainDiscussionPageId));
		preArchiveRevisions.add(mainDiscPageLatestRev);
		preArchiveRevIds.add(mainDiscPageLatestRev.getRevisionID());

		//in case we don't want archives or if we haven't found any, return just the rev of the current talk page
		if(!includeArchives||discArchives==null){
			return preArchiveRevisions;
		}

		//process archives
		int archNum = 0;
		for(Page arch:discArchives){
			Timestamp ts = revApi.getFirstDateOfAppearance(arch.getPageId());			
			Revision rev=null; 
			try{
				//TODO check if rev history was moved to archive - in that case, pass on archive page rather than main page
				rev = RevisionUtils.getRevisionBeforeTimestamp(mainDiscussionPage.getPageId(), ts, revApi);				
			}catch(Exception e){
				logger.warn("Could not retrieve pre-archive revision for "+mainDiscussionPage.getTitle().getPlainTitle());
			}
			if(!preArchiveRevIds.contains(rev.getRevisionID())){
				if(rev!=null){
					preArchiveRevIds.add(rev.getRevisionID());					
					preArchiveRevisions.add(rev);
				}
			}
			archNum++;											
		}
		logger.info("Identified "+preArchiveRevisions.size()+" Talk page snapshots for "+archNum+" discussion archives");		
		return preArchiveRevisions;
	}

	public List<Page> getSourcePages() {
		return sourcePages;
	}

	public List<TalkPage> getTalkPages() {
		return talkPages;
	}
	
	public Wikipedia getWiki(){
		return wiki;
	}

	
	
}
