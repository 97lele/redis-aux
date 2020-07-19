import com.xl.redisaux.transport.client.TcpHeartBeatClient;
import com.xl.redisaux.transport.config.TransportConfig;

/**
 * @author lulu
 * @Date 2020/7/18 21:16
 */
public class Test2 {
    public static void main(String args[]) throws Exception {
        TransportConfig.set(TransportConfig.CONNECT_TIMEOUT_MS,"3000");
        TransportConfig.set(TransportConfig.HOST_NAME,"host1");
        TransportConfig.set(TransportConfig.HEARTBEAT_CLIENT_PORT,"2003");
        TcpHeartBeatClient client = new TcpHeartBeatClient("127.0.0.1", 1210);
        client.start();

    }
}
