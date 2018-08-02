usage 
===
	java -jar testudo_xxx.jar -Dconf="d:\abc\foo\testudo.json" -Did="bbb" -Dproc="2" -Dbuildvar="bXXX" 

* [conf]
	* conf is defined for full path of configuration file(format is json).
	* get "testudo.json" under same folder by default if no provided.
    
* [id]
	* id is the key(ID) of [conf], get the 1st by default if no provided.

* [proc]
	* "1" generates metadata files.
	* "2" package the files you want.
	* "all" does the "1" and then "2".
	* choose "2"  by default if no provided.
    
* [buildvar]
	* this is the internal build numbers in product's manifest.xml. For example, here is 1.12.0.b005 in manifest.xml, b005 is the internal build numbers.
	* set "b000" by default if no provided.



json instruction
-------------------------------------------
		[
		{
		  "ID": "itsID",
		  "prefix": "your product prefix, i.e. FED",
		  "metadataPath": "the location of this product's metadata folder and this path is followed product folder struct, i.e. E:\\ComplianceProduct\\fed\\src\\Metadata",
		  "metadataStruct": "the metadata struct which in under `metadataPath`, i.e.FED_FORM_META.ini",
		  "databaseServer": {
		    "name": "itsName",
		    "driver": "two values you can choose: oracle, sqlserver",
		    "host": "host name/ip+@ or \\+service naming or instance name, i.e. 172.20.20.49@ora11g, 172.20.20.57\\sql2012",
		    "schema": "oracle's schema name or sqlserver's database name, i.e. ECR_12801_OFFICIAL_SYSTEM",
		    "password": "password of your schema or database, i.e. password"
		  },
		  "requiredTables": {"file name is table name without #",
		    "singles": [ "#FormVars", "#Instances", "#InstanceSets", "#RefReturns", "#Rets" ],
		    "dividedByReturnIds": [ "these tables will be divided by returnId, file name is table name(without #) with returnId",
		      "#GridKey",
		      "#GridRef",
		      "#Ref",
		      "#Sums",
		      "#Vals",
		      "#XVals",
		      "#XVals",......
		    ],
		    "notes": "# means `prefix`"
		  },
		  "zipSettings": {
		    "notes": "anything here",
		    "requiredMetadata": [ "the metadata(under `metadataPath`) which need to imported to dpm, make sure they are all csv format",
		    "GridRef\\*.csv",
		    "GridRef\\.csv",
		    "Sums\\*01*.csv",
		    "ExportFormatModule.csv",......
		     ],
		    "dpmFullPath": "the full name of this product's dpm, i.e. E:\\ComplianceProduct\\fed\\src\\dpm\\FED_FORM_META.accdb",
		    "productProperties": "the full name of this product's properties. i.e. E:\\ComplianceProduct\\fed\\package.properties",
		    "zipFiles": ["the files or all files under which folder will be zipped",
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
		  "databaseServer": {
		    "name": "toolset system database",
		    "driver": "oracle",
		    "host": "172.20.20.49@ora11g",
		    "schema": "ECR_12801_OFFICIAL_SYSTEM",
		    "password": "password"
		  },
		  "requiredTables": {
		    "singles": [
		      "#FormVars",
		      "#Instances",
		      "#InstanceSets",
		      "#RefReturns",
		      "#Rets"
		    ],
		    "dividedByReturnIds": [
		      "#GridKey",
		      "#GridRef"
		    ],
		    "notes": ""# means prefix"
		  },
		  "zipSettings": {
		    "notes": "xxx",
		    "requiredMetadata": [
		      "GridRef\\009.csv",
		      "Rets"
		    ],
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