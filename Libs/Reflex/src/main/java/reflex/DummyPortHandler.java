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
package reflex;

import java.util.HashMap;
import java.util.Map;

/**
 * A dummy port handler simply works with in memory objects
 * 
 * @author amkimian
 * 
 */
public class DummyPortHandler implements IReflexPortHandler {
    private Map<String, StringBuilder> inFlightPorts = new HashMap<String, StringBuilder>();
    private Map<String, String> closedPorts = new HashMap<String, String>();

    @Override
    public boolean advanceRecord(String portName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close(String portName) {
        if (inFlightPorts.containsKey(portName)) {
            closedPorts.put(portName, inFlightPorts.get(portName).toString());
            inFlightPorts.remove(portName);
        }
    }

    @Override
    public void open(String portName, String config) {
        inFlightPorts.put(portName, new StringBuilder());
    }

    @Override
    public String readField(String portName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeField(String portName, String field) {
        if (inFlightPorts.containsKey(portName)) {
            StringBuilder buf = inFlightPorts.get(portName);
            buf.append(field);
            buf.append(",");
        }
    }

    @Override
    public void writeRecord(String portName) {
        if (inFlightPorts.containsKey(portName)) {
            StringBuilder buf = inFlightPorts.get(portName);
            buf.append("\n");
        }
    }

    @Override
    public boolean hasCapability() {
        return true;
    }

}
