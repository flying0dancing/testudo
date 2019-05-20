package com.lombardrisk;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {

    }
   /*
    public void testApp1()
    {
    	String dbpath="E:\\ComplianceProduct\\PRA(2)\\src\\dpm\\PRA_FORM_META.accdb";
    	DBInfo dbInfo=new DBInfo(new DatabaseServer("accessdb","",dbpath,"",""));
    	String tableName="Ref";
    	String csvPath="E:\\ComplianceProduct\\PRA(2)\\src\\Metadata\\Ref\\Ref_440003.csv";
    	String schemaFullName="E:\\ComplianceProduct\\PRA(2)\\src\\Metadata\\PRA_FORM_META.ini";
    	dbInfo.importCsvToAccess(tableName, csvPath, schemaFullName);
        assertTrue( true );
    }
   @Deprecated
    public void testApp2()
    {
    	String dbpath="E:\\ComplianceProduct\\PRA(2)\\src\\dpm\\PRA_FORM_META.accdb";
    	DBInfo dbInfo=new DBInfo(new DatabaseServer("accessdb","",dbpath,"",""));
    	String tableName="Rets";
    	String csvPath="E:\\ComplianceProduct\\PRA(2)\\src\\Metadata\\Rets.csv";
    	String schemaFullName="E:\\ComplianceProduct\\PRA(2)\\src\\Metadata\\PRA_FORM_META.ini";
    	dbInfo.importCsvToAccess(tableName, csvPath, schemaFullName);
        assertTrue( true );
    }*/
}
