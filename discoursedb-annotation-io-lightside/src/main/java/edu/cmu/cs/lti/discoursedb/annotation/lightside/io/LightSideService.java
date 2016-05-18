package edu.cmu.cs.lti.discoursedb.annotation.lightside.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

import edu.cmu.cs.lti.discoursedb.annotation.lightside.model.RawDataInstance;
import edu.cmu.cs.lti.discoursedb.core.model.annotation.AnnotationInstance;
import edu.cmu.cs.lti.discoursedb.core.model.annotation.Feature;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Contribution;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.service.annotation.AnnotationService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContentService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContributionService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.DiscoursePartService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.DiscourseService;
import edu.cmu.cs.lti.discoursedb.core.type.DiscoursePartTypes;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;

@Log4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
@Transactional(propagation= Propagation.REQUIRED, readOnly=false)
public class LightSideService {
	
	private final @NonNull DiscourseService discourseService;
	private final @NonNull ContributionService contribService;
	private final @NonNull ContentService contentService;
	private final @NonNull AnnotationService annoService;
	private final @NonNull DiscoursePartService dpService;

	/*
	 * Values used in LightSide output
	 * Can be adapted to treat missing values differently or to encode booleans with 0 and 1
	 */
	private static final String LABEL_ASSIGNED_VAL = "1";
	private static final String LABEL_MISSING_VAL = "0";
	private static final String VALUE_MISSING_VAL = "?";
	private static final String TEXT_COL = "text";
	private static final String ID_COL = "id";
		
	@Transactional(readOnly=true)
	public void exportAnnotations(String discourseName, DiscoursePartTypes dptype, File outputFolder){
		Discourse discourse = discourseService.findOne(discourseName).orElseThrow(
				() -> new EntityNotFoundException("Discourse with name " + discourseName + " does not exist."));
		exportAnnotations(discourse, dptype, outputFolder);	
	}
	
	@Transactional(readOnly=true)
	public void exportAnnotations(Discourse discourse, DiscoursePartTypes dptype, File outputFolder){
		log.info("Processing discourse "+discourse.getName()+". Extracting DiscourseParts of type "+dptype.name());
		exportAnnotations(dpService.findAllByDiscourseAndType(discourse, dptype), outputFolder);	
	}
	
		
	@Transactional(readOnly=true)
	public void exportAnnotations(Iterable<DiscoursePart> discourseParts, File outputFolder){
		List<RawDataInstance> data = StreamSupport.stream(discourseParts.spliterator(), false).flatMap(dp -> extractAnnotations(dp).stream()).collect(Collectors.toList());
		
		try{
			FileUtils.writeStringToFile(new File(outputFolder, "lightside.csv"), generateLightSideOutput(data));			
		}catch(IOException e){
			log.error("Error writing LightSide file to disk",e);
		}	
	}
	
	@Transactional(readOnly=true)
	public List<RawDataInstance> extractAnnotations(DiscoursePart dp){
		log.info("Processing DiscoursePart "+dp.getName());
		return extractAnnotations(contribService.findAllByDiscoursePart(dp));
	}
	
	
	@Transactional(readOnly=true)
	public List<RawDataInstance> extractAnnotations(Iterable<Contribution> contribs){
		List<RawDataInstance> outputList = new ArrayList<>();

		//one instance per contribution for entity label annotations			
		for(Contribution contrib: contribs){
			RawDataInstance newContribData = new RawDataInstance();
			newContribData.setText(contrib.getCurrentRevision().getText());
			newContribData.setSpanAnnotation(false);
			newContribData.setAnnotations(convertAnnotationInstances(annoService.findAnnotations(contrib)));
			outputList.add(newContribData);											
		}				
		//NOTE: we currently don't process span annotations		
		return outputList;
	}
	
	@Transactional(readOnly=true)
	private Map<String, String> convertAnnotationInstance(AnnotationInstance annotation){
		Set<AnnotationInstance> annos = new HashSet<>();
		annos.add(annotation);
		return convertAnnotationInstances(annos);
	}
	

	@Transactional(readOnly=true)
	private Map<String, String> convertAnnotationInstances(Set<AnnotationInstance> annotations){
		Map<String, String> pairs = new HashMap<>();
		if(annotations==null){
			return pairs;
		}
		//convert each annotation into a FeatureValuePair
		for(AnnotationInstance anno:annotations){
			if(anno.getFeatures()!=null&&!anno.getFeatures().isEmpty()){
				Assert.isTrue(anno.getFeatures().size()==1, "Annotations with more than one features are not supported.");
				pairs.put(anno.getType().toLowerCase(), anno.getFeatures().iterator().next().getValue());					
			}else{
				//if there is no feature, we treat the annotation as a binary label (set to true)
				pairs.put(anno.getType().toLowerCase(), LABEL_ASSIGNED_VAL);					
			}
		}
		return pairs;
	}
	
	private String generateLightSideOutput(List<RawDataInstance> data) throws JsonProcessingException{
		StringBuilder output = new StringBuilder();
		CsvMapper mapper = new CsvMapper();
		
		//generate list of binary label types
		Set<String> binaryLabelTypes = data.stream().parallel().flatMap(instance -> instance.getAnnotations().entrySet().stream())
				.filter(m -> m.getValue().toLowerCase().equals(LABEL_ASSIGNED_VAL)).map(m->m.getKey().toLowerCase()).collect(Collectors.toSet());		
		
		//generate header
		Set<String> types = data.stream().parallel().flatMap(instance -> instance.getAnnotations().entrySet().stream())
				.map(m -> m.getKey().toLowerCase()).collect(Collectors.toSet());
		
		Assert.isTrue(!types.contains(TEXT_COL), "No feature with the name \""+TEXT_COL+"\" is allowed.");
		
		List<String> header = new ArrayList<>(types.size()+1);
		header.add(TEXT_COL);
		header.addAll(types);		
		output.append(mapper.writeValueAsString(header));
		
		//generate data vectors
		for(RawDataInstance instance:data){
			List<String> featVector = new ArrayList<>(header.size());
			featVector.add(instance.getText());
			Map<String,String> curInstAnnos = instance.getAnnotations();			
			for(String type:types){
				//Label assigned to current instance 
				if(curInstAnnos.containsKey(type)){
					featVector.add(curInstAnnos.get(type));
				}
				//Label not assigned to current instance - handle missing value 
				else{
					if(binaryLabelTypes.contains(type)){
						//missing binary label interpreted as "false"
						featVector.add(LABEL_MISSING_VAL); 																			
					}else{
						//missing value on interpreted as "null"
						featVector.add(VALUE_MISSING_VAL); 													
					}
				}					
			}
			Assert.isTrue(featVector.size()==header.size(), "Error writing feature vector. Wrong size.");
			output.append(mapper.writeValueAsString(featVector));
	    }		
		return output.toString();
	}	
	
	
	public void exportDataForAnnotation(String outputFilePath, DiscoursePart dp){
		exportDataForAnnotation(outputFilePath, contribService.findAllByDiscoursePart(dp));
	}
	
	public void exportDataForAnnotation(String outputFilePath, Iterable<Contribution> contributions){		
		Assert.hasText(outputFilePath, "Path to the output file cannot be empty.");				
		File outputFile = new File(outputFilePath);
		Assert.isTrue(outputFile.isFile(), outputFilePath+" is not a file.");
		
		StringBuilder output = new StringBuilder();
		CsvMapper mapper = new CsvMapper();
		try{
			output.append(mapper.writeValueAsString(new String[]{TEXT_COL,ID_COL}));			
			for(Contribution contrib:contributions){
				output.append(mapper.writeValueAsString(new String[]{contrib.getCurrentRevision().getText(), String.valueOf(contrib.getId())}));				
			}
			FileUtils.writeStringToFile(outputFile, output.toString());						
		}catch(IOException e){
			log.error("Error writing exported data to csv");					
		}
	}

	public void importAnnotatedData(String inputFile){
		CsvMapper mapper = new CsvMapper();
		mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
		File csvFile = new File(inputFile);
		try{
			MappingIterator<String[]> it = mapper.readerFor(String[].class).readValues(csvFile);			
			
			//process header
			String[] header = it.next();
			Map<String, Integer> headerId = new HashMap<>();
			for(int i=0;i<header.length;i++){
				headerId.put(header[i], i);
			}

			//process data
			while (it.hasNext()) {
			  String[] row = it.next();
			  Contribution curContrib = null;
			  List<AnnotationInstance> curAnnos = new ArrayList<>();
			  for(int i=0;i<row.length;i++){
				  String field = row[i];				  
				  if(i==headerId.get(TEXT_COL)){
					  //we don't need the text column
				  }else if(i==headerId.get(ID_COL)){
					  curContrib=contribService.findOne(Long.parseLong(field)).orElseThrow(()->new EntityNotFoundException("Cannot find annotated entity in database."));					  
				  }else{
					  //we don't need to create an annotation if it's a binary label set to false
					  if(!field.equalsIgnoreCase(LABEL_MISSING_VAL)){
						  String label = header[i];
						  if(label.endsWith("_predicted")){
							  label=label.split("_predicted")[0];
						  }
						  AnnotationInstance newAnno =annoService.createTypedAnnotation(label);
						  annoService.saveAnnotationInstance(newAnno);
						  curAnnos.add(newAnno);
						  //if we have any other value than true or false, store this value as a feature
						  if(!field.equalsIgnoreCase(LABEL_ASSIGNED_VAL)){							  
							  Feature newFeat = annoService.createFeature(field);
							  annoService.saveFeature(newFeat);
							  annoService.addFeature(newAnno, newFeat);
						  }						  
					  }
				  }
			  }
			  //add annotations to the contribution it belongs to 
			  for(AnnotationInstance anno:curAnnos){
				  annoService.addAnnotation(curContrib, anno);
			  }
			}					
		}catch(IOException e){
			log.error("Error reading and parsing data from csv");					
		}
		
		
	}

}
