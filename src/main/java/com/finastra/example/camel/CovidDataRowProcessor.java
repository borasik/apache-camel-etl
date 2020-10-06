package com.finastra.example.camel;

import java.util.Map;

import com.finastra.example.camel.dto.Covid;
import com.finastra.example.camel.dto.ExchangeRates;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class CovidDataRowProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {

        Map<String, Object> row = exchange.getIn().getBody(Map.class);

        log.debug("Processing {}", row);

        Covid covid = new Covid();
        ExchangeRates exchRates = new ExchangeRates();

        if (row == null)
            return;

        var dateFormatter = new SimpleDateFormat("yyyy-mm-dd");
        covid.setDate(dateFormatter.format((Date) row.get("date")));
        covid.setStates((Long) row.get("states"));
        covid.setPositive((Long) row.get("positive"));
        covid.setNegative((Long) row.get("negative"));
        covid.setPosNeg((Long) row.get("pos_neg"));
        covid.setPending((Long) row.get("pending"));
        covid.setHospitalized((Long) row.get("hospitalized"));
        covid.setDeath((Long) row.get("death"));
        covid.setTotal((Long) row.get("total"));
        covid.setDateChecked(dateFormatter.format((Date) row.get("date_checked")));
        covid.setTotalTestResults((Long) row.get("total_test_results"));
        covid.setDeathIncrease((Long) row.get("death_increase"));
        covid.setHospitalizedIncrease((Long) row.get("hospitalized_increase"));
        covid.setNegativeIncrease((Long) row.get("negative_increase"));
        covid.setPositiveIncrease((Long) row.get("positive_increase"));
        covid.setTotalTestResultsIncrease((Long) row.get("total_test_results_increase"));

        exchRates.setRate((Double) row.get("cad"));
        covid.setExchRates(exchRates);      
        
        exchange.getMessage().setBody(covid);
    }

}
