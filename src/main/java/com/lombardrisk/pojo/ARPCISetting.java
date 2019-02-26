package com.lombardrisk.pojo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

public class ARPCISetting {
	private static final Logger logger = LoggerFactory.getLogger(ARPCISetting.class);
	private String ID;
    private String prefix;
    private String metadataPath;
    private String metadataStruct;
    private List<DBAndTables> databaseServerAndTables;
    private ZipSettings zipSettings;
    private String notes;
    private transient String srcPath;
    private transient String targetSrcPath;

    
    public String getID() {
		return ID;
	}
    
	public void setID(String iD) {
		ID = iD;
	}
    
	public String getPrefix() {
		return prefix;
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public String getMetadataPath() {
		return metadataPath;
	}
	
	public void setMetadataPath(String metadataPath) {
		this.metadataPath = metadataPath;
	}
	
	public String getMetadataStruct() {
		return metadataStruct;
	}
	
	public void setMetadataStruct(String metadataStruct) {
		this.metadataStruct = metadataStruct;
	}
	
	public List<DBAndTables> getDatabaseServerAndTables() {
		return databaseServerAndTables;
	}
	
	public void setDatabaseServerAndTables(List<DBAndTables> dBAndTables) {
		databaseServerAndTables = dBAndTables;
	}
	
	public ZipSettings getZipSettings() {
		return zipSettings;
	}
	
	public void setZipSettings(ZipSettings zipSettings) {
		this.zipSettings = zipSettings;
	}
	
	public String getNotes() {
		return notes;
	}
	
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public String getSrcPath() {
		return srcPath;
	}

	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}

	public String getTargetSrcPath() {
		return targetSrcPath;
	}

	public void setTargetSrcPath(String targetSrcPath) {
		this.targetSrcPath = targetSrcPath;
	}
	
	@Override
    public String toString() {
		try{
			//return new Gson().toJson(this);
			return new GsonBuilder().setPrettyPrinting().create().toJson(this);
		}catch(Exception e){
			logger.error(e.getMessage());
			return "";
		}
		
    }

}
