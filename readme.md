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
	* id is the key(ID) of [conf]. get the 1st if no provided.

* [proc]
	* "1" generates metadata files.
	* "2" package the files you want.
	* "all" does the "1" and then "2".
	* choose "2"  by default if no provided.
    
* [release]
	* optional
	* set it means release version. 

* [runOnJenkins]
	* optional
	* set it means run on jenkins. 

json instruction
-------------------------------------------

```json
[
{
  "ID": "its ID",
  
  "prefix": "[optional] your product prefix, i.e. fed, ecr, MAS, ...",
  
  "metadataPath": "[optional] the location of this product's meta-data folder, and which is followed product folder structure, i.e. E:\\ComplianceProduct\\fed\\src\\Metadata",
  
  "metadataStruct": "[optional] the meta-data structure which in under "metadataPath", i.e.FED_FORM_META.ini",
   "databaseServerAndTables":[
  {
  "ID":"1st",
  "databaseServer": {
    "name": "its Name",
    "driver": "two values you can choose: oracle, sqlserver",
    "host": "<host name/ip>+@+<service naming> or <host name/ip<+\\+<instance name>, i.e. 172.20.20.49@ora11g, 172.20.20.57\\sql2012",
    "schema": "oracle's schema name or sqlserver's database name, i.e. ECR_12801_OFFICIAL_SYSTEM",
    "password": "[optional] password of your schema or database, default value is password."
  },
  
  "requiredTables": {"these items which table name with "#", and will be exported to meta-data folder, their exported files' name is table name without #",
    "singles": [ "#FormVars", "#Instances", "#InstanceSets", "#RefReturns", "#Rets" ],
    "dividedByReturnIds": [ "these items will be exported to meta-data folder and will be divided by returnId, their exported files' name is table name(without #) with returnId suffixed",
      "#GridKey","#GridRef", "#Ref", "#Sums","#Vals","#XVals", "#XVals", ......
    ],
    "notes": "# means product prefix"
  },
  }
  ],
  "zipSettings": {
    "notes": "anything here",
    "requiredMetadata": [ "the meta-data(under "metadataPath") which need to imported to dpm, make sure they are all csv",
    "GridRef\\*.csv",
    "GridRef\\.csv",
    "Sums\\*01*.csv",
    "ExportFormatModule.csv",......
     ],
    "dpmFullPath": "[optional] the full name of this product's dpm, i.e. E:\\ComplianceProduct\\fed\\src\\dpm\\FED_FORM_META.accdb",
    "productProperties": "[optional] the full name of this product's properties. i.e. E:\\ComplianceProduct\\fed\\package.properties",
    "sqlFiles":["the filtered files or all files under filtered folder will be executed in dpmFullPath"],
    "zipFiles": ["the filtered files or all files under filtered folder will be packaged",
    "manifest", 
    "dpm",
    "dpm\\*",
    "BE185_v1",
    "CIL_VERSION00",......
    ]
  }
},
{
  "ID": "aaa",
  "prefix": "ECR",
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
    "notes": ""# means prefix"
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
}

]
```