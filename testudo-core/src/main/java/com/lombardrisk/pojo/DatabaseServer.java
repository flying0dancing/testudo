package com.lombardrisk.pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

public class DatabaseServer {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseServer.class);
	private int id;
    private String name;
    private String driver;
    private String url;
    private String host;
    private String port;
    private String version;
    private String instance;
    private String schema;
    private String dump;
    private String username;
    private String password;
    
    public DatabaseServer(String driver,String host, String schema,String username,String password)
    {
    	setDriver(driver);
    	setHost(host);
    	setSchema(schema);
    	setUsername(username);
    	setPassword(password);
    }
    
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDriver() {
		return driver;
	}
	public void setDriver(String driver) {
		this.driver = driver;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getInstance() {
		return instance;
	}
	public void setInstance(String instance) {
		this.instance = instance;
	}
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		if(System.getProperty("file.separator").equals("/")){
			schema=schema.replace("\\", "/");
		}else{
			schema=schema.replace("/", "\\");
		}
		this.schema = schema;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getDump() {
		return dump;
	}
	public void setDump(String dump) {
		this.dump = dump;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
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
}
