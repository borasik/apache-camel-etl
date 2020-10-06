package com.finastra.example.camel.settings;

import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "oracle.extract")
public class OracleExtractSettings {
    private String outputPath;   
    
    public String getBatchPath() { return outputPath + "/" + new SimpleDateFormat("yyyyMMdd_hhmmss").format(new Date()); }
}
