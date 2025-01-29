package org.betterbox.elasticBuffer;
import org.betterbox.elasticBuffer.ElasticBuffer;

public class ElasticBufferAPI {
    private final ElasticBuffer elasticBuffer;


    // Konstruktor przyjmuje instancję ElasticBuffer, która będzie używana do logowania.
    public ElasticBufferAPI(ElasticBuffer elasticBuffer) {
        this.elasticBuffer = elasticBuffer;
    }

    public void log(String message, String level, String pluginName, String transactionID,String playerName,String uuid) {
        if (elasticBuffer != null) {
            elasticBuffer.receiveLog(message, level, pluginName,transactionID,playerName,uuid);
        }
    }
    public void log(String message, String level, String pluginName, String transactionID) {
        if (elasticBuffer != null) {
            elasticBuffer.receiveLog(message, level, pluginName,transactionID);
        }
    }
    public void log(String message, String level, String pluginName, String transactionID,String playerName,String uuid,double keyValue) {
        if (elasticBuffer != null) {
            elasticBuffer.receiveLog(message, level, pluginName,transactionID,playerName,uuid,keyValue);
        }

    }
}
