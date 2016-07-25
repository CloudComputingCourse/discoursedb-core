package edu.cmu.cs.lti.discoursedb.api.browsing.resource;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.hateoas.ResourceSupport;
import com.fasterxml.jackson.annotation.JsonFormat;

import edu.cmu.cs.lti.discoursedb.api.browsing.controller.BrowsingRestController;
import edu.cmu.cs.lti.discoursedb.core.model.annotation.AnnotationInstance;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePartRelation;
import edu.cmu.cs.lti.discoursedb.core.model.user.DiscoursePartInteraction;
import edu.cmu.cs.lti.discoursedb.core.repository.user.DiscoursePartInteractionRepository;

public class BrowsingDiscoursePartResource extends ResourceSupport {
	
	private String name;
	private String type;
	private long subDiscoursePartCount;
	private long contributionCount;
	private List<String> userInteractions;
	private Date startTime;
	private Date endTime;
	private String discourseName;
	private long discourseId;
	private Map<Long,String> containingDiscourseParts;
	private DiscoursePart dp;
	private List<BrowsingAnnotationResource> annotations;
	
	private static final Logger logger = LogManager.getLogger(BrowsingDiscoursePartResource.class);	

	public BrowsingDiscoursePartResource(DiscoursePart dp) {
		this.dp = dp;
		this.setName(dp.getName());
		this.setType(dp.getType());
		this.setStartTime(dp.getStartTime());
		this.setEndTime(dp.getEndTime());
		
		if (dp.getAnnotations() != null) {
			List<BrowsingAnnotationResource> annos = new LinkedList<BrowsingAnnotationResource>();
			for (AnnotationInstance ai: dp.getAnnotations().getAnnotations()) {
				annos.add(new BrowsingAnnotationResource(ai));
			}
			this.setAnnotations(annos);
		}
		
		this.setContributionCount(dp.getDiscoursePartContributions().stream().collect(Collectors.summingLong(f -> 1L)));
		this.setSubDiscoursePartCount(dp.getSourceOfDiscoursePartRelations().stream().collect(Collectors.summingLong(f -> 1L)));
		try {
			// Quick and dirty -- if we're in two discourses, just pick the first one
			Discourse discourse = dp.getDiscourseToDiscourseParts().iterator().next().getDiscourse();
			
			this.setDiscourseId(discourse.getId());
			this.setDiscourseName(discourse.getName());
		} catch (Exception e) {
			// do nothing
		}

		if (this.getSubDiscoursePartCount() > 0) {
			this.add(BrowsingRestController.makeLink("/browsing/subDiscourseParts/" + dp.getId() + "/", 
					 dp.getName() ));			
		}
/*		for (DiscoursePartRelation dp1 : dp.getSourceOfDiscoursePartRelations()) {
			this.add(BrowsingRestController.makeLink("/browsing/subDiscourseParts/" + dp1.getTarget().getId() + "/", 
					dp1.getType() + ": " + dp1.getTarget().getName() ));			
		} */
		
		if (this.getContributionCount() > 0) {
			this.add(BrowsingRestController.makeLink("/browsing/dpContributions/" + dp.getId() + "/", "contributions"));
		}
		
   	    containingDiscourseParts = new HashMap<Long,String>();
   	    for (DiscoursePartRelation dpr : dp.getTargetOfDiscoursePartRelations()) {
   	    	containingDiscourseParts.put(dpr.getSource().getId(), dpr.getSource().getName());
   	    }
	}
	
	public void fillInUserInteractions(DiscoursePartInteractionRepository dpr) {
		for (DiscoursePartInteraction dpi : dpr.findAllByDiscoursePart(dp)) {
			String interaction = dpi.getUser().getUsername() + ": " + dpi.getType();
			if (dpi.getAnnotations() != null) {
				List<BrowsingAnnotationResource> anno = dpi.getAnnotations().getAnnotations().stream().
						map(BrowsingAnnotationResource::new).
						collect(Collectors.toList());
				if (annotations == null) { 
					annotations = new LinkedList<BrowsingAnnotationResource>();
				}
				annotations.addAll(anno);
			}
			if (userInteractions == null) {
				userInteractions = new LinkedList<String>();
			}
			userInteractions.add(interaction);
		}
	}
	
	public void filterAnnotations(String annotType) {
		if (!annotType.equals("*")) {
			setAnnotations(this.getAnnotations().stream().filter( bai -> bai.getType().equals(annotType) ).collect(Collectors.toList()));
		}
	}

	/*
	 * Underscore so that Jackson doesn't pick it up -- this is for internal use only
	 */
	public Long _getDpId() {
		return dp.getId();
	}

	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public Map<Long,String> _getContainingDiscourseParts() {
		return containingDiscourseParts;
	}
	public List<String> getContainingDiscourseParts() {
		return containingDiscourseParts.values().stream().collect(Collectors.toList());
	}


	public Date getStartTime() {
		return startTime;
	}


	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}


	public Date getEndTime() {
		return endTime;
	}


	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}


	public List<BrowsingAnnotationResource> getAnnotations() {
		return annotations;
	}


	public void setAnnotations(List<BrowsingAnnotationResource> annotations) {
		this.annotations = annotations;
	}

	public long getSubDiscoursePartCount() {
		return subDiscoursePartCount;
	}

	public void setSubDiscoursePartCount(long subDiscoursePartCount) {
		this.subDiscoursePartCount = subDiscoursePartCount;
	}

	public long getContributionCount() {
		return contributionCount;
	}

	public void setContributionCount(long contributionCount) {
		this.contributionCount = contributionCount;
	}

	public long getDiscourseId() {
		return discourseId;
	}

	public void setDiscourseId(long discourseId) {
		this.discourseId = discourseId;
	}

	public String getDiscourseName() {
		return discourseName;
	}

	public void setDiscourseName(String discourseName) {
		this.discourseName = discourseName;
	}

	public List<String> getUserInteractions() {
		return userInteractions;
	}

	public void setUserInteractions(List<String> userInteractions) {
		this.userInteractions = userInteractions;
	}


	
	

}
