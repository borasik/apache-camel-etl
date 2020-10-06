package com.finastra.example.camel.dto;

import lombok.Data;

@Data
public class Covid {
    
    private String date;
    private Long states;
    private Long positive;
    private Long negative;
    private Long posNeg;
    private Long pending;
    private Long hospitalized;
    private Long death;
    private Long total;
    private String dateChecked;
    private Long totalTestResults;
    private Long deathIncrease;
    private Long hospitalizedIncrease;
    private Long negativeIncrease;
    private Long positiveIncrease;
    private Long totalTestResultsIncrease;    
    private ExchangeRates exchRates;
    
}
