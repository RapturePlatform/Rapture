/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.common.connection;

import rapture.common.CallingContext;
import rapture.common.ConnectionInfo;

/**
 * Created by yanwang on 12/3/15.
 */
public class MongoConnectionInfoConfigurer extends ConnectionInfoConfigurer {

    public static int DEFAULT_PORT = 27017;

    @Override
    public ConnectionType getType() {
        return ConnectionType.MONGODB;
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    public static String getUrl(ConnectionInfo info) {
        //mongodb://test:test@127.0.0.1/integrationTest
        return String.format("mongodb://%s:%s@%s:%d/%s", info.getUsername(), info.getPassword(),
                info.getHost(), info.getPort(), info.getDbName());
    }
}
