usage 
===
testudo can generates metadata files from many databases, and then compress necessary files into package(zip and lrm).
------
	java -jar testudo.jar -Dconf="d:\abc\foo\testudo.json" -Dprefix=fed -Did=bbb -Dproc=2 -Drelease -DrunOnJenkins

* [conf]
	* optional
	* conf is defined for full path of configuration file(format is json). `Highest priority`.
	* get "testudo.json" under the same folder if both [prefix] and [conf] are not provided.
	
* [prefix]
	* prefix is the folder name of product
	* get "testudo.json" under this folder.
    
* [id]
	* optional
	* id is the key(ID) of [conf](json file[{"ID":"a",...},{"ID":"b",...},{...},...]). Get the 1st if no provided.

* [proc]
	* choose "2"  by default if no provided.
	* "1" generates meta-data files. Generate meta-data structure and meta-data.
	* "2" package the files you want. Create access database if need (as dpm), and then create tables defined in metadataStruct(.ini) and import meta-data(*.csv) into dpm, and then execute SQLs if need, package files and generate packages at last.
	* "all" does the "1" and then "2".
    
* [release]
	* optional
	* use this flag means release version. It will increase build number, like 1.2.3.4 -> 1.2.3.5
	* does Not use this flag, it will adding a timestamp, like 1.2.3.4 -> 1.2.3.4-1538120420274

* [runOnJenkins]
	* optional
	* use this flag means run on jenkins. It will do everything in the original product folder.
	* does Not use this flag, it will create a copy of product folder, generate packaged files in the copied one without change anything in the original product folder.


json instruction
-------------------------------------------

```json
[
{
  "ID": "its ID",
  
  "prefix": "[optional] product prefix, i.e. fed, ecr, MAS, ...",
  
  "metadataPath": "[optional] path of meta-data folder, and which is followed product folder structure, i.e. ...\\ComplianceProduct\\fed\\src\\Metadata",
  
  "metadataStruct": "[optional] the meta-data structure which in under "metadataPath", i.e.FED_FORM_META.ini",
  "databaseServerAndTables":[
  {
   "ID":"1st",
   "databaseServer": {
     "name": "[optional] its Name",
     "driver": "three options: oracle, sqlserver, accessdb",
     "host": "<host name/ip>+@+<service naming> or <host name/ip<+\\+<instance name>, i.e. 172.20.20.49@ora11g, 172.20.20.57\\sql2012",
     "schema": "oracle's schema name, or sqlserver's database name, or accessDb's full name, i.e. ECR_12801_OFFICIAL_SYSTEM",
     "password": "[optional] default value is password."
   },
  
   "requiredTables": {
     "singles": [ "#FormVars", "#Instances", "#InstanceSets", "#RefReturns", "#Rets",
     		"i.e. export ECRFormVars from this database, stored as FormVars.csv as meta-data in metadataPath." ],
     "dividedByReturnIds": [ "#GridKey","#GridRef", "#Ref", "#Sums","#Vals","#XVals", "#XVals", 
     		"items will be exported to meta-data folder and divided by returnId, their exported files' name is table name(without #) with returnId like List_440001.csv",
     ],
     "notes": "[optional] # means product prefix, "
   }
  },
  {
  	......
  }
  ],
  "zipSettings": {
    "notes": "[optional] anything here",
    "requiredMetadata": [ "the meta-data(under "metadataPath") which need to imported to dpm, make sure they are all csv",
    	"GridRef\\*.csv",
    	"GridRef\\.csv",
    	"Sums\\*01*.csv",
    	"ExportFormatModule.csv",
    	......
     ],
    "dpmFullPath": "[optional] the full name of this product's dpm, i.e. ...\\ComplianceProduct\\fed\\src\\dpm\\FED_FORM_META.accdb",
    "productProperties": "[optional] the full name of this product's properties. i.e. ...\\ComplianceProduct\\fed\\package.properties",
    "sqlFiles":["[optional] the filtered files or all files under filtered folder will be executed on dpm",
    	"sqls\\*.sql"],
    "zipFiles": ["the filtered files or all files under filtered folder will be packaged",
    	"manifest.xml", 
    	"dpm",
    	"dpm\\*",
    	"BE185_v1",
    	"CIL_VERSION00",
    	......
    ]
  }
},
{
  "ID": "aaa",
  "prefix": "ecr",
  "metadataPath": "E:\\ComplianceProduct\\ecr\\src\\Metadata",
  "metadataStruct": "ECR_FORM_META.ini",
  "databaseServerAndTables":[
  {
	"ID":"1st",
	"databaseServer": {
		"name": "toolset system database",
    	"driver": "oracle",
    	"host": "172.20.20.49@ora11g",
    	"schema": "ECR_12801_OFFICIAL_SYSTEM",
    	"password": "password"
	},
	"requiredTables": {
    "singles": [ "#FormVars", "#Instances", "#InstanceSets",  "#RefReturns",  "#Rets"   ],
    "dividedByReturnIds": [  "#GridKey",   "#GridRef" ],
    "notes": "# means prefix"
 	}
  }
  ],
  "zipSettings": {
    "notes": "xxx",
    "requiredMetadata": [
      "GridRef\\009.csv",
      "Rets"
    ],
    "sqlFiles":["sqls/New Text Document.sql","sqls/test.sql"],
    "dpmFullPath": "E:\\ComplianceProduct\\ecr\\src\\dpm\\ECR_FORM_META.accdb",
    "productProperties": "E:\\ComplianceProduct\\ecr\\package.properties",
    "zipFiles": [
      "manifest.xml",
      "transforms",
      "forms",
      "dpm"
    ]
  }
},
{
  "ID": "bbb",
  "databaseServerAndTables":[
  {
	"ID":"1st",
	"databaseServer": {
		"name": "toolset system database",
    	"driver": "oracle",
    	"host": "172.20.20.49@ora11g",
    	"schema": "ECR_12801_OFFICIAL_SYSTEM",
    	"password": "password"
	},
	"requiredTables": {
     "singles": [ "#FormVars", "#Instances", "#InstanceSets",  "#RefReturns",  "#Rets"   ],
     "dividedByReturnIds": [  "#GridKey",   "#GridRef" ],
     "notes": "# means prefix"
 	}
  },
  {
	"ID":"2st",
	"databaseServer": {
		"driver": "accessdb",
		"schema": "E:\\ComplianceProduct\\ecr\\PRA_FORM_META_V1_2_0_1.accdb"
	},
	"requiredTables": {
		"singles": ["Taxonomy","CFG_CONFIG_DEFINED_VARIABLES"],
		"dividedByReturnIds": [],
		"notes": "# means prefix"
	}
  }
  ],
  "zipSettings": {
    "notes": "xxx",
    "requiredMetadata": [
      "GridRef\\009.csv",
      "Rets"
    ],
    "sqlFiles":["sqls/test1.sql","sqls/test2.sql"],
    "zipFiles": [
      "manifest.xml",
      "transforms",
      "forms",
      "dpm"
    ]
  }

]
```

sql instruction
-------------------------------------------
we use UCanAccess for pure read/write Access database, sql statements's format need like [test.sql](test.sql)