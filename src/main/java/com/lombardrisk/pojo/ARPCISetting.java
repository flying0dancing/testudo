package com.lombardrisk.pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

public class ARPCISetting {
	private static final Logger logger = LoggerFactory.getLogger(ARPCISetting.class);
	private String ID;
    private String prefix;
    private String metadataPath;
    private String metadataStruct;
    private DatabaseServer databaseServer;
    private RequiredTables requiredTables;
    private ZipSettings zipSettings;
    private transient String srcPath;
	public DatabaseServer getDatabaseServer() {
		return databaseServer;
	}
	public void setDatabaseServer(DatabaseServer databaseServer) {
		this.databaseServer = databaseServer;
	}
	public RequiredTables getRequiredTables() {
		return requiredTables;
	}
	public void setRequiredTables(RequiredTables requiredTables) {
		this.requiredTables = requiredTables;
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
	public ZipSettings getZipSettings() {
		return zipSettings;
	}
	public void setZipSettings(ZipSettings zipSettings) {
		this.zipSettings = zipSettings;
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
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSrcPath() {
		return srcPath;
	}

	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}
	public String getID() {
		return ID;
	}
	public void setID(String iD) {
		ID = iD;
	}
}
