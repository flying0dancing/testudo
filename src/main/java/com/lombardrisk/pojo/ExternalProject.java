package com.lombardrisk.pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;



public class ExternalProject {
	private static final Logger logger = LoggerFactory.getLogger(ExternalProject.class);
	
	private String project;
	private String srcFile;
	private String destDir;
	private String uncompress;
	private String notes;
	
	@Override
	public String toString()
	{
		try{
			return new GsonBuilder().setPrettyPrinting().create().toJson(this);
		}catch(Exception e){
			logger.error(e.getMessage());
			return "";
		}
	}
	public String getProject() {
		return project;
	}
	public void setProject(String project) {
		this.project = project;
	}
	public String getSrcFile() {
		return srcFile;
	}
	public void setSrcFile(String srcFile) {
		this.srcFile = srcFile;
	}
	public String getDestDir() {
		return destDir;
	}
	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	public String getUncompress() {
		return uncompress;
	}
	public void setUncompress(String uncompress) {
		this.uncompress = uncompress;
	}

	
	
}
