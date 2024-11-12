package org.betterbox.elasticBuffer;
import org.betterbox.elasticBuffer.ElasticBuffer;

public class ElasticBufferAPI {
    private static ElasticBufferAPI instance = new ElasticBufferAPI();

    private ElasticBufferAPI() {
    }

    public static ElasticBufferAPI getInstance() {
        return instance;
    }

    public void log(String message, String level, String pluginName) {
        ElasticBuffer plugin = ElasticBuffer.getInstance();
        if (plugin != null) {
            plugin.receiveLog(message, level, pluginName);
        }
    }
}
