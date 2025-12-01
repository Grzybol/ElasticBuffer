package org.betterbox.elasticBuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ElasticBufferAPI {
    private final ElasticBuffer elasticBuffer;


    // Konstruktor przyjmuje instancję ElasticBuffer, która będzie używana do logowania.
    public ElasticBufferAPI(ElasticBuffer elasticBuffer) {
        this.elasticBuffer = elasticBuffer;
    }

    public void log(String message, String level, String pluginName, String transactionID,String playerName,String uuid) {
        log(message, level, pluginName, transactionID, playerName, uuid, Collections.emptyMap());
    }
    public void log(String message, String level, String pluginName, String transactionID) {
        log(message, level, pluginName, transactionID, "N/A", "N/A", Collections.emptyMap());
    }
    public void log(String message, String level, String pluginName, String transactionID,String playerName,String uuid,double keyValue) {
        Map<String,Object> additionalFields = new HashMap<>();
        additionalFields.put("keyValue", keyValue);
        log(message, level, pluginName, transactionID, playerName, uuid, additionalFields);

    }

    public void log(String message,
                    String level,
                    String pluginName,
                    String transactionID,
                    String playerName,
                    String uuid,
                    Map<String, Object> additionalFields) {
        if (elasticBuffer != null) {
            elasticBuffer.receiveLog(message, level, pluginName, transactionID, playerName, uuid, additionalFields);
        }
    }
}
