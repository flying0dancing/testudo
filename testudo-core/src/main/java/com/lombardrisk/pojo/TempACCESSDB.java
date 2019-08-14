package com.lombardrisk.pojo;

public enum TempACCESSDB {
    INSTANCE;
    private String name;
    private String dbFullName;

    public String getDbFullName() {
        return dbFullName;
    }
    public void initial(final String dbFullName,final String name) {
        this.dbFullName = dbFullName;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}



