package com.lombardrisk.pojo;

import java.util.ArrayList;
import java.util.List;

public final class DivideTableFieldList {
    private static List<DivideTableField> divideTableFieldList=new ArrayList<DivideTableField>(){
        //for solve ARPA-70:Split by Return ID for CFG_Page_Schedules and CFG_Schedules tables
        {
            String returnID="Return_ID";
            add(new DivideTableField("CFG_Page_Schedules",returnID));
            add(new DivideTableField("CFG_Schedules",returnID));
            add(new DivideTableField("CFG_VALIDATION_RULE",returnID));
        }
    };

    private DivideTableFieldList(){}

    public static List<DivideTableField> getDivideTableFieldList() {
        return divideTableFieldList;
    }

    public static String getDividedField(String tableName){
        //for solve ARPA-70:Split by Return ID for CFG_Page_Schedules and CFG_Schedules tables
        for (DivideTableField tableField:getDivideTableFieldList()
                ) {
            if(tableName.equalsIgnoreCase(tableField.getTablename())){
                return tableField.getFieldname();
            }
        }
        return "ReturnId";
    }
}
