package com.byoutline.mockserver;

import com.byoutline.mockserver.internal.ConfigParser;
import com.byoutline.mockserver.internal.MockNetworkLag;
import com.byoutline.mockserver.internal.ParsedConfig;
import com.byoutline.mockserver.internal.ResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Local mock HTTP server.
 */
public class HttpMockServer implements Container {

    private final static Logger LOGGER = Logger.getLogger(HttpMockServer.class.getName());

    public static boolean DEBUG = true;
    private final Connection conn;
    private final ResponseHandler responseHandler;

    private static HttpMockServer sMockServer;

    public HttpMockServer(@Nonnull JSONObject jsonObject, @Nonnull ConfigReader configReader, @Nonnull NetworkType simulatedNetworkType)
            throws IOException, JSONException {
        ParsedConfig config = new ConfigParser(configReader).parseConfig(jsonObject);
        MockNetworkLag networkLag = new MockNetworkLag(simulatedNetworkType);
        this.responseHandler = new ResponseHandler(config.responses, networkLag, configReader);
        Server server = new ContainerServer(this);
        conn = new SocketConnection(server);
        final SocketAddress sa = new InetSocketAddress(config.port);
        conn.connect(sa);
    }

    /**
     * Starts mock server and keeps reference to it.
     *
     * @param configReader         wrapper for platform specific bits
     * @param simulatedNetworkType delay time before response is sent.
     */
    public static HttpMockServer startMockApiServer(@Nonnull ConfigReader configReader,
                                                    @Nonnull NetworkType simulatedNetworkType) {
        try {
            String configJson = new String(readInitialData(configReader.getMainConfigFile()));
            JSONObject jsonObj = configJson.isEmpty() ? new JSONObject() : new JSONObject(configJson);
            sMockServer = new HttpMockServer(jsonObj, configReader, simulatedNetworkType);
            return sMockServer;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "MockServer error:", e);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "MockServer error:", e);
        }
        return null;
    }

    public void reset() {
        this.responseHandler.stopResponding();
    }

    public void shutdown() throws Exception {
        conn.close();
        this.reset();
    }

    @Override
    public void handle(Request req, Response resp) {
        responseHandler.handle(req, resp);
    }

    static byte[] readInitialData(@Nullable InputStream inputStream)
            throws IOException {
        if (inputStream == null) {
            return new byte[0];
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int i = inputStream.read();
        while (i != -1) {
            byteArrayOutputStream.write(i);
            i = inputStream.read();
        }
        inputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

}
