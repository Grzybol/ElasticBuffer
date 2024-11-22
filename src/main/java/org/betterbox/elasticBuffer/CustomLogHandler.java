package org.betterbox.elasticBuffer;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Level;


public class CustomLogHandler extends Handler{
    private final StringBuilder logBuilder = new StringBuilder();
    private ElasticBuffer elasticBuffer;
    public CustomLogHandler(ElasticBuffer elasticBuffer){
        this.elasticBuffer=elasticBuffer;
    }
    @Override
    public void publish(LogRecord record) {
            // Przygotuj informacje logu
            String message = record.getMessage();
            String level = record.getLevel().getName();
            String pluginName = "Console"; // Jako że to logi konsoli, nazwa pluginu może być stała
            String transactionID = "N/A"; // Nie dostępne dla logów konsoli
            String playerName = "N/A"; // Nie dotyczy logów konsoli
            String uuid = "N/A"; // Nie dotyczy logów konsoli

            // Dodaj log do bufora
            elasticBuffer.receiveLog(message, level, pluginName, transactionID, playerName, uuid);
    }

    @Override
    public void flush() {
        // Tutaj można by zaimplementować logikę czyszczenia bufora, jeśli jest to potrzebne.
    }

    @Override
    public void close() throws SecurityException {
        // Tutaj można zaimplementować logikę zwalniania zasobów, jeśli jest to potrzebne.
    }

    public String getLogs() {
        return logBuilder.toString();
    }

}
