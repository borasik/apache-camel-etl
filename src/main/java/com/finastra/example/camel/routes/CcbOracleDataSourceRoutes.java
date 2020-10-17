package com.finastra.example.camel.routes;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.io.ByteArrayInputStream;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.finastra.example.camel.DtoJsonLineAggregator;
import com.finastra.example.camel.CamelEtlApplication.AzureStorageSettings;
import com.finastra.example.camel.settings.FfdcPlatformSettings;
import com.finastra.example.camel.settings.OracleExtractSettings;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.LoggingLevel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CcbOracleDataSourceRoutes extends RouteBuilder {

    @Autowired
    AzureStorageSettings azureStorageSettings;
    @Autowired
    OracleExtractSettings oracleExtractSettings;
    @Autowired
    DtoJsonLineAggregator jsonLineAggregator;
    @Autowired
    FfdcPlatformSettings ffdcPlatformSettings;

    @Override
    public void configure() throws Exception {

        this.getContext().getGlobalOptions().put(Exchange.LOG_EIP_NAME, "com.apache.camel.etl");

        onException(Exception.class)
        .log(LoggingLevel.ERROR, "Exception detected. Quitting...")
        .to("log:com.apache.camel.etl?showAll=true").handled(true)
        .wireTap("bean:exitHandler?method=errorShutdown").stop();

        from("timer://foo?repeatCount=1")      
        .routeId("oracle-ccb-extract-route")
        .setHeader("resultLimit", constant(oracleExtractSettings.getResultLimit()))
        .to("sql:classpath:sql/oracle-extract.sql?dataSource=setupDataSource")
        .log(LoggingLevel.DEBUG, "Body from Data Source: ${body}")
        .marshal()
        .json(JsonLibrary.Jackson)
        .setHeader(Exchange.FILE_NAME, simple("ccb-gtp-account-statement-${date:now:yyyyMMdd-hhmmss-SSS}.json"))
        .process((exchange) -> {
            var fileToUpload = exchange.getIn().getBody(InputStream.class);            
            exchange.setProperty("fileToUpload", fileToUpload);
        })
        .to("file:" + oracleExtractSettings.getBatchPath() + "?fileName=${file:name}")
        .to("direct: get-access-token-for-data-set-management-api");

        from("direct: get-access-token-for-data-set-management-api")     
        .routeId("get-access-token-for-data-set-management-api")
        .process(exchange -> exchange.getIn().setBody(
                        "grant_type=client_credentials&scope=openid&client_id=1988806a-8113-4cba-ab81-22b5eada6d99&client_secret=5a13861f-f1fd-441f-9683-72cee3f9b99e"))
        .setHeader("CamelHttpMethod", constant("POST"))
        .setHeader("Content-Type", constant("application/x-www-form-urlencoded"))
        .to("netty-http:" + ffdcPlatformSettings.getLoginUrl()).unmarshal().json(JsonLibrary.Jackson)
        .process((exchange) -> {
                  var body = exchange.getIn().getBody(Map.class);
                  String accessToken = body.get("access_token").toString();
                  exchange.getIn().setHeader("AccessToken", accessToken);
                  log.info("Access Token: {}", accessToken);
        })
        .to("direct:get-sas-token-for-ingestion");

        from("direct:get-sas-token-for-ingestion")       
        .routeId("get-sas-token-from-data-set-management-api")
        .process(exchange -> exchange.getIn().setBody(
                        "{\"dataSetId\": \"trades-v1-27a8371b-9317-43ed-82c0-39835cf1ec03\",\"protocol\": \"ADLv2\",\"fileName\": \"2019-02-13T23:00:00.000Z.json\"}"))
        .setHeader("CamelHttpMethod", constant("POST")).setHeader("Accept", constant("application/json"))
        .setHeader("Content-Type", constant("application/json"))
        .setHeader("Authorization", simple("Bearer ${header.AccessToken}"))
        .to("netty-http:" + ffdcPlatformSettings.getIngestionJobUrl()).unmarshal().json(JsonLibrary.Jackson)        
        .process((exchange) -> {
                   var body = exchange.getIn().getBody(Map.class);
                   String sas = body.get("singleUseToken").toString();
                   log.info("SSS: " + sas);
                   String jobId = body.get("jobId").toString();
                   String blobUrl = body.get("blobUrl").toString();
                   String fileName = body.get("fileName").toString();
                   exchange.setProperty("jobId", jobId);
                   exchange.setProperty("sas", sas);
                   exchange.setProperty("blobUrl", blobUrl);                   
                   exchange.setProperty("fileName", fileName);  
                   exchange.setProperty("fullUrl", blobUrl + "?" + sas);  
               })
        .log(LoggingLevel.INFO, "EXC2: ${exchangeProperty.blobUrl}")
        .to("direct:ingest-data-set-to-data-lake");

        //from("file:" + oracleExtractSettings.getBatchPath() + "?fileName=${file:name}")
        from("direct:ingest-data-set-to-data-lake")    
        .routeId("ingest-data-set-to-ffdc-data-lake")        
        .process((exchange) -> {
            var fileToUpload = exchange.getProperty("fileToUpload");
            String fileName = (String)exchange.getProperty("fileName");
            log.info("fileToUpload {}", fileToUpload);
            log.info("fileNameToUpload {}", fileName);
            //InputStream is = new ByteArrayInputStream(fileToUpload);
            exchange.getIn().setBody(fileToUpload); 
        })            
        .setHeader("CamelHttpMethod", constant("PUT"))        
        .setHeader("Content-Type", constant("application/json"))
        .setHeader("x-ms-blob-type", constant("BlockBlob"))  
        .setHeader("CamelHttpUrl", simple("${exchangeProperty.fullUrl}"))   
        .log(LoggingLevel.INFO, "URL_TO_BLOB: " + "${exchangeProperty.fullUrl}")
        //.toD("netty-http: ${exchangeProperty.fullUrl}");        
        .toD("netty-http: https://p21q11101547001.blob.core.windows.net/c2896825-c0d8-408d-82b5-0470c6a1e44f/bcc148bf-3236-49cc-aa5e-9410f0d483b1?sv=2019-12-12&ss=b&srt=co&sp=wx&se=2020-10-29T23:45:27Z&st=2020-10-08T15:45:27Z&spr=https&sig=Dp0S03%2FtX5pTsMemiJUP3iMDId3SLGdmSgl9iZf8cS4%3D");

    }
}
