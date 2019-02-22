package com.lombardrisk.pojo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

public class ZipSettings {
	private static final Logger logger = LoggerFactory.getLogger(ZipSettings.class);
	
	private String excludeFileFilters;
	private List<String> requiredMetadata;
	private List<ExternalProject> externalProjects;
	private String dpmFullPath;
	private String productProperties;
	private List<String> sqlFiles;
	private List<String> zipFiles;
	private String notes;
	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public String getDpmFullPath() {
		return dpmFullPath;
	}

	public void setDpmFullPath(String accessDBFullPath) {
		this.dpmFullPath = accessDBFullPath;
	}

	public List<String> getRequiredMetadata() {
		return requiredMetadata;
	}

	public void setRequiredMetadata(List<String> requiredMetadata) {
		this.requiredMetadata = requiredMetadata;
	}
	public List<String> getZipFiles() {
		return zipFiles;
	}

	public void setZipFiles(List<String> zipFiles) {
		this.zipFiles = zipFiles;
	}
	
	public String getProductProperties() {
		return productProperties;
	}

	public void setProductProperties(String productProperties) {
		this.productProperties = productProperties;
	}
	
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

	public List<String> getSqlFiles() {
		return sqlFiles;
	}

	public void setSqlFiles(List<String> sqlFiles) {
		this.sqlFiles = sqlFiles;
	}

	public String getExcludeFileFilters() {
		return excludeFileFilters;
	}

	public void setExcludeFileFilters(String excludeFileFilters) {
		this.excludeFileFilters = excludeFileFilters;
	}

	public List<ExternalProject> getExternalProjects() {
		return externalProjects;
	}

	public void setExternalProjects(List<ExternalProject> externalProjects) {
		this.externalProjects = externalProjects;
	}
}
