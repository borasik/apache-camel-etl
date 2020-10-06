package com.finastra.example.camel;

import com.finastra.example.camel.settings.OracleExtractSettings;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CcbOracleDataSourceRoutes extends RouteBuilder {
    
    @Autowired OracleExtractSettings oracleExtractSettings;

    @Override
    public void configure() throws Exception {
        
        this.getContext().getGlobalOptions().put(Exchange.LOG_EIP_NAME, "com.apache.camel.etl");

        onException(Exception.class)
        .log(LoggingLevel.ERROR, "Exception detected. Quitting...")
        .to("log:com.apache.camel.etl?showAll=true")
        .handled(true)
        .wireTap("bean:exitHandler?method=errorShutdown")
        .stop();

        from("timer://runOnce?repeatCount=10").to("direct:bounded-sql-to-dto-stream");

        from("direct:bounded-sql-to-dto-stream").routeId("bounded-sql-to-dto-stream")
        .setHeader("resultLimit", constant(oracleExtractSettings.getOutputPath()))
        .setBody(constant("select ACCOUNT_ID,STATEMENT_ID,BRCH_CODE,TYPE,IDX,SEQ_IDX,DESCRIPTION,VALUE_DATE FROM GTP_ACCOUNT_STATEMENT"))
        .to("jdbc:setupDataSource")
        .log(LoggingLevel.DEBUG, "query.resultSet = ${body}")
        .to("direct:dto-bounded-to-jsonlines-file");

        from("direct:dto-bounded-to-jsonlines-file")
        .log(LoggingLevel.DEBUG, "dto = ${body}")
        .setHeader(Exchange.FILE_NAME, simple("covid-${date:now:yyyyMMdd-hhmmss-SSS}.json"))
        .to("file:" + oracleExtractSettings.getBatchPath() + "?doneFileName=${file:name}.ready");
    }
}
