package me.waffles.additional.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AbyssAPIUtilsTransportTest {
    private HttpServer server;
    private String baseUrl;
    private final List<Integer> clientPorts = new CopyOnWriteArrayList<>();
    private final List<String> acceptEncodings = new CopyOnWriteArrayList<>();

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/plain", exchange -> respond(exchange, 200, "{\"player\":{}}".getBytes(StandardCharsets.UTF_8), false));
        server.createContext("/gzip", exchange -> respond(exchange, 200, gzip("<a>\nb</a>\r\n<c>✧</c>"), true));
        server.createContext("/busy", exchange -> respond(exchange, 503, new byte[0], false));
        server.createContext("/missing", exchange -> respond(exchange, 404, new byte[0], false));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void gzipBodiesAreDecompressedAndJoinedLikeTheOldLineReader() {
        String body = AbyssAPIUtils.fetchPlayerDataOnce(baseUrl + "/gzip", "test", "*/*");

        assertEquals("<a>b</a><c>✧</c>", body);
        assertEquals("gzip", acceptEncodings.get(0));
    }

    @Test
    public void sequentialRequestsReuseOneConnection() {
        assertEquals("{\"player\":{}}", AbyssAPIUtils.fetchPlayerDataOnce(baseUrl + "/plain", "test"));
        assertEquals("{\"player\":{}}", AbyssAPIUtils.fetchPlayerDataOnce(baseUrl + "/plain", "test"));

        // The same client port means the second request skipped a new TCP handshake.
        assertEquals(2, clientPorts.size());
        assertEquals(clientPorts.get(0), clientPorts.get(1));
    }

    @Test
    public void statusCodesKeepTheirRetrySignals() {
        // null asks the retrying fetch to try again; "" is a final answer.
        assertNull(AbyssAPIUtils.fetchPlayerDataOnce(baseUrl + "/busy", "test"));
        assertEquals("", AbyssAPIUtils.fetchPlayerDataOnce(baseUrl + "/missing", "test"));
    }

    private void respond(HttpExchange exchange, int status, byte[] body, boolean gzip) throws IOException {
        clientPorts.add(exchange.getRemoteAddress().getPort());
        String encoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
        acceptEncodings.add(encoding == null ? "" : encoding);
        if (gzip) {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
        }
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] gzip(String text) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }
}
