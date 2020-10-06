package com.finastra.example.camel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finastra.example.camel.dto.Covid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DtoJsonLineAggregator {

    private static final byte[] NEW_LINE = "\n".getBytes(Charset.forName("UTF-8"));

    @Autowired
    ObjectMapper objectMapper;

    public ByteArrayOutputStream append(ByteArrayOutputStream collector, Covid covid)
            throws JsonGenerationException, JsonMappingException, IOException {

            if (collector == null) {
                collector = new ByteArrayOutputStream();
            }

            log.debug("Appending covid.json = {}", covid);
            objectMapper.writeValue(collector, covid);
            collector.write(NEW_LINE);

            return collector;
        }

}
