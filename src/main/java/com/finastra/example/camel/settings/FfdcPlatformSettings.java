package com.finastra.example.camel.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "platform")
public class FfdcPlatformSettings {

    private String loginUrl;
    public String ingestionJobUrl;
    
}
