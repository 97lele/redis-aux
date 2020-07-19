import com.xl.redisaux.transport.client.TcpHeartBeatClient;
import com.xl.redisaux.transport.server.HeartBeatServer;

/**
 * @author lulu
 * @Date 2020/7/18 21:13
 */
public class Test {
    public static void main(String args[]){
        HeartBeatServer server=new HeartBeatServer("127.0.0.1",1210);
        server.start();
    }
}
