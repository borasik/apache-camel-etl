package com.finastra.example.camel.processors;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finastra.example.camel.dto.Covid;
import com.finastra.example.camel.dto.ExchangeRates;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class DataPlatformIngestionProcessProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {

        Map<String, Object> body = exchange.getIn().getBody(Map.class);

        String accessToken = body.get("access_token").toString();
        if(accessToken != null)
        {
            exchange.getIn().setHeader("AccessToken", accessToken);
            log.debug("Access Token Set to Header");
        }
        else
        {
            log.error("Failed To Retrieve Access Token");
        }                
    }
}