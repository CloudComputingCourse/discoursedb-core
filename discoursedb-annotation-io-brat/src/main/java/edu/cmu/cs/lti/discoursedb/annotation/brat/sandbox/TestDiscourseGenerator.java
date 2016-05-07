package edu.cmu.cs.lti.discoursedb.annotation.brat.sandbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.cmu.cs.lti.discoursedb.configuration.BaseConfiguration;
import edu.cmu.cs.lti.discoursedb.core.model.annotation.AnnotationInstance;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Content;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Contribution;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.service.annotation.AnnotationService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContentService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContributionService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.DiscoursePartService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.DiscourseService;
import edu.cmu.cs.lti.discoursedb.core.type.ContributionTypes;
import edu.cmu.cs.lti.discoursedb.core.type.DiscoursePartTypes;

/**
 * Generates a sample discourse with contributions, content and annotations.
 * 
 * @author Oliver Ferschke
 */
@Component
@SpringBootApplication
@ComponentScan(	basePackages = { "edu.cmu.cs.lti.discoursedb.configuration"}, 
				useDefaultFilters = false, 
				includeFilters = {@ComponentScan.Filter(
						type = FilterType.ASSIGNABLE_TYPE, 
						value = {TestDiscourseGenerator.class, BaseConfiguration.class })})
public class TestDiscourseGenerator implements CommandLineRunner {

	@Autowired private DiscourseService discourseService;
	@Autowired private DiscoursePartService discoursePartService;
	@Autowired private ContributionService contribService;
	@Autowired private ContentService contentService;
	@Autowired private AnnotationService annoService;
	
	/**
	 * Launches the SpringBoot application 
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
        SpringApplication.run(TestDiscourseGenerator.class, args);       
	}
	
	@Override
	@Transactional
	public void run(String... args) throws Exception {
		Discourse discourse = discourseService.createOrGetDiscourse("testdiscourse");
		DiscoursePart threadOne = discoursePartService.createOrGetTypedDiscoursePart(discourse, "ThreadOne", DiscoursePartTypes.THREAD); 
		DiscoursePart threadTwo = discoursePartService.createOrGetTypedDiscoursePart(discourse, "ThreadTwo", DiscoursePartTypes.THREAD); 
		/////
		
		Contribution contrib1 = contribService.createTypedContribution(ContributionTypes.THREAD_STARTER);
		Content content1 = contentService.createContent();
		content1.setText("Contribution one 😎😎😎");
		contrib1.setFirstRevision(content1);
		contrib1.setCurrentRevision(content1);
		discoursePartService.addContributionToDiscoursePart(contrib1, threadOne);

		Contribution contrib2 = contribService.createTypedContribution(ContributionTypes.POST);
		Content content2 = contentService.createContent();
		content2.setText("Contribution two 😅😅😅");
		contrib2.setFirstRevision(content2);
		contrib2.setCurrentRevision(content2);
		discoursePartService.addContributionToDiscoursePart(contrib2, threadOne);

		Contribution contrib3 = contribService.createTypedContribution(ContributionTypes.THREAD_STARTER);
		Content content3 = contentService.createContent();
		content3.setText("Contribution three 😆😆😆");
		contrib3.setFirstRevision(content3);
		contrib3.setCurrentRevision(content3);
		discoursePartService.addContributionToDiscoursePart(contrib3, threadTwo);
		
		/////
		
		AnnotationInstance anno1 = annoService.createTypedAnnotation("DDB_LABEL_1");
//		annoService.addFeature(anno1, annoService.createFeature("1.0"));
		annoService.addAnnotation(contrib1, anno1);
//		
//		AnnotationInstance anno2 = annoService.createTypedAnnotation("DDB_LABEL_2");
//		annoService.addFeature(anno2, annoService.createFeature("two"));
//		annoService.addAnnotation(contrib2, anno2);
//		
//		AnnotationInstance anno3 = annoService.createTypedAnnotation("DDB_LABEL_3");
//		annoService.addFeature(anno3, annoService.createFeature("3"));
//		annoService.addAnnotation(contrib3, anno3);

	}
}
