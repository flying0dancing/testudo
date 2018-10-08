
alter table GridKey add column ThresholdExpression text;
alter table GridKey add column RowLimit text;
alter table GridRef add column Threshold int;
alter table GridRef add column IsInnerGridCell datetime;
alter table GridRef add column ReportLine boolean;
alter table List add column SOURCETYPE nvarchar(255);
alter table Ref add column ReportLine nvarchar(255);
alter table Sums add column Type nvarchar(30);
alter table Sums add column RegRuleId nvarchar(30);
alter table Vals add column Type nvarchar(30);
alter table Vals add column RegRuleId nvarchar(30);

update Rets set CompOutputTable='BRV1ARP' where ReturnId=440001;
update GridRef set IsInnerGridCell=0;
