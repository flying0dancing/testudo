package com.lombardrisk.pojo;

public class DivideTableField {
    private String tablename;
    private String fieldname;

    public DivideTableField(final String tablename, final String fieldname) {
        this.tablename = tablename;
        this.fieldname=fieldname;
    }

    public String getTablename() {
        return tablename;
    }

    public void setTablename(final String tablename) {
        this.tablename = tablename;
    }

    public String getFieldname() {
        return fieldname;
    }

    public void setFieldname(final String fieldname) {
        this.fieldname = fieldname;
    }
}
