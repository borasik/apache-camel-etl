package com.finastra.example.camel.routes;

import java.io.ByteArrayOutputStream;

import com.finastra.example.camel.DtoJsonLineAggregator;
import com.finastra.example.camel.settings.OracleExtractSettings;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.camel.builder.AggregationStrategies;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CcbOracleDataSourceRoutes extends RouteBuilder {

    @Autowired
    OracleExtractSettings oracleExtractSettings;
    @Autowired
    DtoJsonLineAggregator jsonLineAggregator;

    @Override
    public void configure() throws Exception {

        this.getContext().getGlobalOptions().put(Exchange.LOG_EIP_NAME, "com.apache.camel.etl");

        onException(Exception.class).log(LoggingLevel.ERROR, "Exception detected. Quitting...")
                .to("log:com.apache.camel.etl?showAll=true").handled(true)
                .wireTap("bean:exitHandler?method=errorShutdown").stop();

        // from("timer://runOnce?repeatCount=10").to("direct:bounded-sql-to-dto-stream");

        // from("direct:bounded-sql-to-dto-stream").routeId("bounded-sql-to-dto-stream")
        // .setHeader("resultLimit", constant(oracleExtractSettings.getOutputPath()))
        // .setBody(constant("select acc.*,ent.* from gtp_account acc join
        // gtp_entity_account ent on acc.account_id = ent.account_id "))
        // .to("jdbc:setupDataSource")
        // .marshal().json(true)
        // .log(LoggingLevel.DEBUG, "query.resultSet = ${body}")
        // .setHeader(Exchange.FILE_NAME,
        // simple("ccb-gtp-account-statement-${date:now:yyyyMMdd-hhmmss-SSS}.json"))
        // .to("file:" + oracleExtractSettings.getBatchPath() +
        // "?doneFileName=${file:name}.ready");
        // .to("direct:dto-bounded-to-jsonlines-file");

        // from("direct:dto-bounded-to-jsonlines-file")
        // .log(LoggingLevel.TRACE, "dto = ${body}")
        // .marshal().json(true)
        // // //.convertBodyTo(java.io.ByteArrayOutputStream.class)
        // // .aggregate(constant(true),
        // AggregationStrategies.beanAllowNull(jsonLineAggregator, "append"))
        // // .completionSize(oracleExtractSettings.getOutputBatchSize().toString())
        // .setHeader(Exchange.FILE_NAME, simple("ccb-gtp-account-statement-${date:now:yyyyMMdd-hhmmss-SSS}.json"))
        // .to("file:" + oracleExtractSettings.getBatchPath() +
        // "?doneFileName=${file:name}.ready");

        from("timer://foo?repeatCount=1").routeId("oracle-ccb-extract-route")
        .log(LoggingLevel.INFO, "${body} DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD")
        .setHeader("resultLimit", constant(oracleExtractSettings.getResultLimit()))
        .to("sql:classpath:sql/oracle-extract.sql?dataSource=setupDataSource")
        .log(LoggingLevel.INFO, "${body} ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ")
        //.convertBodyTo(ByteArrayOutputStream.class)        
        .split(body())      
        //.process((exchange) -> {exchange.setProperty("ReadableCamelBatchIndex", ((Integer) exchange.getProperty("CamelBatchIndex")) + 1);}) 
        //.unmarshal().json(JsonLibrary.Jackson)
        //.to("bean:jacksonConfigured?method=writeValueAsString")
        .log(LoggingLevel.INFO, "${body} YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY")   
        .aggregate(constant(true), AggregationStrategies.beanAllowNull(jsonLineAggregator, "append"))
        .completionSize("10")       
        .log(LoggingLevel.INFO, "${body} DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD")
        .setHeader(Exchange.FILE_NAME, simple("ccb-gtp-account-statement-${date:now:yyyyMMdd-hhmmss-SSS}.json"))
        .to("file:" + oracleExtractSettings.getBatchPath() + "?doneFileName=${file:name}.ready");
    }
}
