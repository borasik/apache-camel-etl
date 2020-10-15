package com.finastra.example.camel.aggregators;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonBasicAggregator implements AggregationStrategy {

    // @Autowired ObjectMapper objectMapper;

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        ByteArrayOutputStream collector = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();

        if (oldExchange == null) {
            Object newLine = newExchange.getIn().getBody();
            try{
                objectMapper.writeValue(collector, newLine);
            }            
            catch(JsonGenerationException ex){
                log.info("Custom JsonGenerationException EXCEPTION in JsonBasicAggregator", ex);
            }
            catch(JsonMappingException ex){
                log.info("Custom  JsonMappingExceptionEXCEPTION in JsonBasicAggregator", ex);
            }
            catch(IOException ex){
                log.info("Custom IOException EXCEPTION in JsonBasicAggregator", ex);
            }
            catch(Exception ex){
                log.info("Custom EXCEPTION in JsonBasicAggregator", ex);
            }

            newExchange.getIn().setBody("[" + collector.toString());

            return newExchange;
        }

        try{
            Object newLine = newExchange.getIn().getBody();
            objectMapper.writeValue(collector, newLine);
        }            
        catch(JsonGenerationException ex){
            log.info("Custom JsonGenerationException EXCEPTION in JsonBasicAggregator", ex);
        }
        catch(JsonMappingException ex){
            log.info("Custom  JsonMappingExceptionEXCEPTION in JsonBasicAggregator", ex);
        }
        catch(IOException ex){
            log.info("Custom IOException EXCEPTION in JsonBasicAggregator", ex);
        }
        catch(Exception ex){
            log.info("Custom EXCEPTION in JsonBasicAggregator", ex);
        }

        String body = oldExchange.getIn().getBody(String.class) + "," + collector.toString();          

        oldExchange.getIn().setBody(body);

        return oldExchange;
    }

    @Override
    public void onCompletion(Exchange exchange) {
        if(exchange == null)
        {
            return;
        }
    }
}
