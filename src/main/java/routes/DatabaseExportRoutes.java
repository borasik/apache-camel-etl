package routes;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.finastra.example.camel.CovidDataRowProcessor;
import com.finastra.example.camel.DtoJsonLineAggregator;
import com.finastra.example.camel.CamelEtlApplication.AzureStorageSettings;
import com.finastra.example.camel.CamelEtlApplication.ExtractSettings;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseExportRoutes extends RouteBuilder {

    @Autowired ExtractSettings extractSettings;
    @Autowired AzureStorageSettings azureStorageSettings;
    @Autowired DtoJsonLineAggregator jsonLineAggregator;

    @Override
    public void configure() throws Exception {
        this.getContext().getGlobalOptions().put(Exchange.LOG_EIP_NAME, "com.finastra.example.camel");

        onException(Exception.class)
            .log(LoggingLevel.ERROR, "Exception detected. Quitting...")
            .to("log:com.finastra.example.camel?showAll=true")
            .handled(true)
            .wireTap("bean:exitHandler?method=errorShutdown")
            .stop();

        from("timer://runOnce?repeatCount=1").to("direct:bounded-sql-to-dto-stream");

        from("direct:bounded-sql-to-dto-stream").routeId("bounded-sql-to-dto-stream")
            .setHeader("resultLimit", constant(extractSettings.getResultLimit()))
            .to("sql:classpath:sql/covid-extract.sql")
            .log(LoggingLevel.DEBUG, "query.resultSet = ${body}")
            .split(body())
                .log(LoggingLevel.DEBUG, "record.map = ${body}")
                .process(new CovidDataRowProcessor())
                .to("direct:dto-stream-to-jsonlines-file");

        from("direct:dto-stream-to-jsonlines-file").routeId("dto-stream-to-jsonlines-file")
                .log(LoggingLevel.DEBUG, "record.dto = ${body}")
                .aggregate(constant(true), AggregationStrategies.beanAllowNull(jsonLineAggregator, "append"))
                    .completionSize(extractSettings.getOutputBatchSize().toString())
                .log(LoggingLevel.DEBUG, "jsonlines = ${body}")
                .setHeader(Exchange.FILE_NAME, simple("covid-${date:now:yyyyMMdd-hhmmss-SSS}.json"))
                // .marshal().zipFile()
                .to("file:" + extractSettings.getBatchPath() + "?doneFileName=${file:name}.ready");

        from("file:" + extractSettings.getBatchPath()
                + "?doneFileName=${file:name}.ready&idempotent=true&idempotentRepository=#fileIdempotentRepository")
                    .routeId("file-to-blobstorage")
                    .process((exchange) -> {exchange.setProperty("ReadableCamelBatchIndex",
                                            ((Integer) exchange.getProperty("CamelBatchIndex")) + 1);})
                    .log(LoggingLevel.INFO, "Uploading file ${header.CamelFileName}, to Azure Blob Storage - " 
                                            + "${header.ReadableCamelBatchIndex} of ${header.CamelBatchSize}")
                    .setHeader(BlobConstants.BLOB_NAME, simple("${header.CamelFileName}"))
                    .to("azure-storage-blob://"
                            + azureStorageSettings.getBlobAccountName() + "/"
                            + azureStorageSettings.getBlobContainerName()
                            + "?blobName=myBlob&operation=uploadBlockBlob&serviceClient=#blobServiceClient")
                    .onCompletion().onCompleteOnly()
                        .to("log:com.finastra.example.camel?level=DEBUG&showAll=true")
                        .choice()
                            .when(simple("${header.CamelBatchComplete} == 'true'"))
                            .to("direct:signal-ingest-api");

        from("direct:signal-ingest-api").routeId("signal-ingest-api")
            .log(LoggingLevel.INFO, "All files uploaded to Azure. Starting ingest...")
            .process((exchange) -> {
                exchange.getMessage().setBody("{ \"status\": \"COMPLETE_UPLOAD\" }");
                exchange.getMessage().setHeader("CamelHttpMethod", "PUT");
                exchange.getMessage().setHeader("Content-Type", "application/json; charset=\"UTF-8\"");
            })
            .to("netty-http:" + extractSettings.getIngestUrl())
            .log(LoggingLevel.INFO, "Returned ${header.CamelHttpResponseCode} : '${body}'");

    }

    @Bean
    public IdempotentRepository fileIdempotentRepository() {
        return FileIdempotentRepository.fileIdempotentRepository(extractSettings.getFilesProcessedRepoPath(), 1);
    }

    @Bean
    BlobServiceClient blobServiceClient() {
        final BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(azureStorageSettings.getBlobEndpointUri()).sasToken(azureStorageSettings.getBlobSasToken())
                .buildClient();

        return blobServiceClient;
    }
}