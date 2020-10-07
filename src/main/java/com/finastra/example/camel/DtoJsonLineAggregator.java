package com.finastra.example.camel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DtoJsonLineAggregator {

    private static final byte[] NEW_LINE = "\n".getBytes(Charset.forName("UTF-8"));

    @Autowired
    ObjectMapper objectMapper;

    public ByteArrayOutputStream append(ByteArrayOutputStream collector, HashMap record)
            throws JsonGenerationException, JsonMappingException, IOException {

            if (collector == null) {
                collector = new ByteArrayOutputStream();
            }

            log.info("Appending covid.json = {}", record);
            objectMapper.writeValue(collector, record);
            collector.write(NEW_LINE);

            return collector;
        }

}
