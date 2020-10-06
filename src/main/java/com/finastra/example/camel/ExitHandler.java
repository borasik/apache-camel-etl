package com.finastra.example.camel;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component
public class ExitHandler {
    
    public void errorShutdown(Exchange exchange) {
        if (!alreadyShuttingDown(exchange)) {
            shutdownCamel(exchange);
            System.exit(1);
        }
    }
            
    public void shutdown(Exchange exchange) {
        if (!alreadyShuttingDown(exchange)) {
            shutdownCamel(exchange);
            System.exit(0);
        }
    }

    private boolean alreadyShuttingDown(Exchange exchange) {
        return exchange.getContext().isStopping() || exchange.getContext().isStopped();
    }

    private void shutdownCamel(Exchange exchange) {
        exchange.getContext().getShutdownStrategy().setLogInflightExchangesOnTimeout(false);
        exchange.getContext().getShutdownStrategy().setTimeout(1);
        exchange.getContext().getShutdownStrategy().setShutdownNowOnTimeout(true);
        exchange.getContext().shutdown();
    }

}
