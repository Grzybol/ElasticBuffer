package org.betterbox.elasticBuffer;
import org.betterbox.elasticBuffer.ElasticBuffer;

public class ElasticBufferAPI {
    private final ElasticBuffer elasticBuffer;


    // Konstruktor przyjmuje instancję ElasticBuffer, która będzie używana do logowania.
    public ElasticBufferAPI(ElasticBuffer elasticBuffer) {
        this.elasticBuffer = elasticBuffer;
    }

    public void log(String message, String level, String pluginName) {
        if (elasticBuffer != null) {
            elasticBuffer.receiveLog(message, level, pluginName);
        }
    }
}
