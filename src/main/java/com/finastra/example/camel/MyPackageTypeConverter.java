package com.finastra.example.camel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyPackageTypeConverter implements TypeConverters {

  private final ObjectMapper mapper;

  @Autowired
  public MyPackageTypeConverter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Converter
  public ByteArrayOutputStream byteArrayToByteArrayOutputStream(byte[] source) {
    try {
      return mapper.readValue(source, ByteArrayOutputStream.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}