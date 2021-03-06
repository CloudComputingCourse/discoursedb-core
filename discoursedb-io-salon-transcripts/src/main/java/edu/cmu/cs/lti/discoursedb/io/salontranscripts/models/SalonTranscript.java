package edu.cmu.cs.lti.discoursedb.io.salontranscripts.models;
/*******************************************************************************
 * Copyright (C)  2015 - 2017  Carnegie Mellon University
 * Author: Oliver Ferschke, Chris Bogart
 *
 * This file is part of DiscourseDB.
 *
 * DiscourseDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * DiscourseDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscourseDB.  If not, see <http://www.gnu.org/licenses/> 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street, 
 * Fifth Floor, Boston, MA 02110-1301  USA
 *******************************************************************************/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SalonTranscript {
	public class SpeakerAndWords {
		public String speaker = "";
		public String words = "";
		public String info = "";
	}
	private static final Logger logger = LogManager.getLogger(SalonTranscript.class);
	
	private ArrayList<SpeakerAndWords> interviewContributions = new ArrayList();
	private ArrayList<SpeakerAndWords> classroomContributions = new ArrayList();
	private String moderator;
	private Date classDate;
	
	static SimpleDateFormat sdfmt= new SimpleDateFormat("dd MMM");
	public SalonTranscript(File f) throws ParseException, IOException {
		
		classDate = sdfmt.parse( f.getName() );
		int section = 0;  // 0=class, 1=interview
		FileReader fileReader = 
                new FileReader(f.getAbsoluteFile());

        BufferedReader bufferedReader = 
                new BufferedReader(fileReader);
        String line;
        while((line = bufferedReader.readLine()) != null) {
        	if (line.startsWith("Moderator:")) {
        		moderator = line.substring(line.indexOf(':')+1).trim();
        	} else if (line.startsWith("INTERVIEW")) {
        		section = 1;
        	} else if (line.startsWith("NOTE")) {
        		// Ignore for now
        	} else
		    if (section==0) {
		    	if (line.contains(":")) {
			    	SpeakerAndWords sw = new SpeakerAndWords();
			    	line = line.replace("•","").trim();
			    	int firstcolon = line.indexOf(':');
		        	sw.speaker =  line.substring(0, firstcolon).trim();
			    	if (sw.speaker.equals(moderator)) {
			    		sw.info = "moderation";
			    	}
			    	sw.words = line.substring(firstcolon+1).trim();
			    	if (sw.words.length() > 0) {
			    		classroomContributions.add(sw);
			    	}
		    	} else {
		    		logger.info("Skipping line: " + line);
		    	}
		    } else {
		    	SpeakerAndWords sw = new SpeakerAndWords();
		    	if (line.contains("•")) {
		    		sw.speaker = "interviewer";
		    		sw.info = "interviewer";
		    	} else {
		    		sw.speaker = moderator;
		    		sw.info = "interviewee";
		    	}
		    	
		    	sw.words = line.replace("•","").trim();
		    	if (sw.words.length() > 0) {
		    		interviewContributions.add(sw);
		    	}
		    	
		    }
        }
        bufferedReader.close();   
	}

	public void adjustYear(int year) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(this.classDate);
		cal.set(Calendar.YEAR, year);
		this.classDate = cal.getTime();
	}
	public ArrayList<SpeakerAndWords> getInterviewContributions() {
		return interviewContributions;
	}
	public boolean hasInterview() {
		return this.interviewContributions != null && this.interviewContributions.size() > 0;
	}
	public void setInterviewContributions(ArrayList<SpeakerAndWords> interviewContributions) {
		this.interviewContributions = interviewContributions;
	}
	public ArrayList<SpeakerAndWords> getClassroomContributions() {
		return classroomContributions;
	}
	public void setClassroomContributions(ArrayList<SpeakerAndWords> classroomContributions) {
		this.classroomContributions = classroomContributions;
	}
	public String getModerator() {
		return moderator;
	}
	public void setModerator(String moderator) {
		this.moderator = moderator;
	}
	public Date getClassDate() {
		return classDate;
	}
	public void setClassDate(Date classDate) {
		this.classDate = classDate;
	}
}