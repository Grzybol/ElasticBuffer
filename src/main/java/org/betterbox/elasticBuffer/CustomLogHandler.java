package org.betterbox.elasticBuffer;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Level;


public class CustomLogHandler{
    private ElasticBuffer bufferPlugin;

    public CustomLogHandler(ElasticBuffer bufferPlugin) {
        this.bufferPlugin = bufferPlugin;
    }
}
