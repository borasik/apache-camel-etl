package com.finastra.example.camel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource; 

import lombok.Data;

@SpringBootApplication
public class CamelEtlApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(CamelEtlApplication.class, args);	
	}

	@Bean
	public ObjectMapper jacksonConfigured() {
		ObjectMapper jackson = new ObjectMapper();
		jackson.registerModules(ObjectMapper.findModules());
		return jackson;
	}

	@Component @Data
	@ConfigurationProperties(prefix = "extract")
	public class ExtractSettings {
		private Long resultLimit;
		private Long outputBatchSize;
		private String outputPath;
		private String ingestUrl;

		public String getBatchPath() { return outputPath + "/" + new SimpleDateFormat("yyyyMMdd_hhmmss").format(new Date()); }
		public Long getExpectedFilesToUpload() { return resultLimit / outputBatchSize; }
		public File getFilesProcessedRepoPath() { return new File(getBatchPath() + "/files-processed.txt"); }
	}

	@Component @Data
	@ConfigurationProperties(prefix = "azure")
	public class AzureStorageSettings {
		private String blobAccountName;
		private String blobContainerName;
		private String blobEndpointUri;
		private String blobSasToken;
	}

	@Bean
	private static DataSource setupDataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        ds.setUsername("FCC5560");
        ds.setPassword("FCC5560");
        ds.setUrl("jdbc:oracle:thin:@10.192.70.236:1521/FCC");
        return ds;
    }
}
