package org.betterbox.elasticBuffer;

import java.util.LinkedList;
import java.util.List;

public class LogBuffer {
    private final List<LogEntry> buffer;  // Lista do przechowywania logów jako obiekty LogEntry

    public LogBuffer() {
        buffer = new LinkedList<>();
    }

    /**
     * Dodaje log do bufora.
     * @param log Treść logu do dodania.
     * @param level Poziom logowania.
     */
    public synchronized void add(String log, String level, String pluginName, long timestamp,String transactionID,String playerName,String uuid) {
        buffer.add(new LogEntry(log, level, pluginName, timestamp,transactionID,playerName,uuid));
    }

    /**
     * Pobiera wszystkie logi z bufora i czyści bufor.
     * @return Lista logów do wysłania.
     */
    public synchronized List<LogEntry> getAndClear() {
        List<LogEntry> logsToSend = new LinkedList<>(buffer);  // Tworzy kopię logów
        buffer.clear();  // Czyści bufor
        return logsToSend;  // Zwraca kopię logów
    }
}
