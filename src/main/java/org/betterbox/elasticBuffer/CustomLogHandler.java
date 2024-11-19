package org.betterbox.elasticBuffer;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Level;

public class CustomLogHandler extends Handler {
    private ElasticBuffer bufferPlugin;

    public CustomLogHandler(ElasticBuffer bufferPlugin) {
        this.bufferPlugin = bufferPlugin;
    }

    @Override
    public void publish(LogRecord record) {
        if (record.getLevel().intValue() >= Level.INFO.intValue()) {
            String message = record.getMessage();
            bufferPlugin.receiveLog(message, "server-logs", "server-logs", null,null,null);
        }
    }

    @Override
    public void flush() {
        // Ta metoda może pozostać pusta, jeśli buforowanie nie jest wymagane
    }

    @Override
    public void close() throws SecurityException {
        // Tu również możesz nie robić nic specjalnego
    }
}
