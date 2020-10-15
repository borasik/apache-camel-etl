package com.finastra.example.camel.routes;

import java.util.Map;

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

    private static String sas;

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

        onException(Exception.class).log(LoggingLevel.ERROR, "Exception detected. Quitting...")
                .to("log:com.apache.camel.etl?showAll=true").handled(true)
                .wireTap("bean:exitHandler?method=errorShutdown").stop();

        from("timer://foo?repeatCount=1").noAutoStartup().routeId("oracle-ccb-extract-route")
        .setHeader("resultLimit", constant(oracleExtractSettings.getResultLimit()))
        .to("sql:classpath:sql/oracle-extract.sql?dataSource=setupDataSource")
        .log(LoggingLevel.DEBUG, "Body from Data Source: ${body}")
        .marshal()
        .json(JsonLibrary.Jackson)
        .setHeader(Exchange.FILE_NAME, simple("ccb-gtp-account-statement-${date:now:yyyyMMdd-hhmmss-SSS}.json"))
        .to("file:" + oracleExtractSettings.getBatchPath() + "?fileName=${file:name}");

        from("direct:start").routeId("get-access-token-for-data-set-management-api")
        .process(exchange -> exchange.getIn().setBody(
                        "grant_type=client_credentials&scope=openid&client_id=1988806a-8113-4cba-ab81-22b5eada6d99&client_secret=5a13861f-f1fd-441f-9683-72cee3f9b99e"))
        .setHeader("CamelHttpMethod", constant("POST"))
        .setHeader("Content-Type", constant("application/x-www-form-urlencoded"))
        .to("netty-http:" + ffdcPlatformSettings.getLoginUrl()).unmarshal().json(JsonLibrary.Jackson)
        .process((exchange) -> {
                  var body = exchange.getIn().getBody(Map.class);
                  String accessToken = body.get("access_token").toString();
                  exchange.getIn().setHeader("AccessToken", accessToken);
        }).to("direct:get-sas-token-for-ingestion");

        from("direct:get-sas-token-for-ingestion").routeId("get-sas-token-from-data-set-management-api")
        .process(exchange -> exchange.getIn().setBody(
                        "{\"dataSetId\": \"trades-v1-27a8371b-9317-43ed-82c0-39835cf1ec03\",\"protocol\": \"ADLv2\",\"fileName\": \"2019-02-13T23:00:00.000Z.json\"}"))
        .setHeader("CamelHttpMethod", constant("POST")).setHeader("Accept", constant("application/json"))
        .setHeader("Content-Type", constant("application/json"))
        .setHeader("Authorization", simple("Bearer ${header.AccessToken}"))
        .to("netty-http:" + ffdcPlatformSettings.getIngestionJobUrl()).unmarshal().json(JsonLibrary.Jackson)
        .process((exchange) -> {
                   var body = exchange.getIn().getBody(Map.class);
                   String sas = body.get("singleUseToken").toString();
                   String jobId = body.get("singleUseToken").toString();
                   exchange.setProperty("JobId", jobId);
                   exchange.getIn().setHeader("sas", sas);
           }).to("direct:ingest-data-set-to-data-lake");

        from("direct:ingest-data-set-to-data-lake")
        .routeId("ingest-data-set-to-ffdc-data-lake")
        .setHeader(BlobConstants.BLOB_NAME, simple("${header.CamelFileName}"))
        .bean(CcbOracleDataSourceRoutes.class, "updateSasToken(\"ALEX SAS\")")
        // .process((exchange) -> {
        //     updateSasToken("ALEXSAS");
        // })
        .to("azure-storage-blob://"
                + azureStorageSettings.getBlobAccountName() + "/"
                + azureStorageSettings.getBlobContainerName()
                + "?blobName=myBlob&operation=uploadBlockBlob&serviceClient=#blobServiceClient2");

    }

    @Bean    
    BlobServiceClient blobServiceClient2() {       
        if(sas == null)            
        {
            sas = azureStorageSettings.getBlobSasToken();
        }
        log.info("just get sas: " + sas);
        final BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                // .endpoint(azureStorageSettings.getBlobEndpointUri()).sasToken(azureStorageSettings.getBlobSasToken())
                .endpoint(azureStorageSettings.getBlobEndpointUri()).sasToken(sas)
                .buildClient();
        log.info("blobServiceClient: " + blobServiceClient);
        return blobServiceClient;
    }

    @Handler
    void updateSasToken(String newSas){
        sas = newSas;     
        log.info("just updated sas to: " + sas); 
    }
}
