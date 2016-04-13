package rapture.common.connection;

/**
 * Created by yanwang on 3/11/16.
 */
public class ESConnectionInfoConfigurer extends ConnectionInfoConfigurer {

    public static int DEFAULT_PORT = 9300;

    @Override
    public ConnectionType getType() {
        return ConnectionType.ES;
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }
}
