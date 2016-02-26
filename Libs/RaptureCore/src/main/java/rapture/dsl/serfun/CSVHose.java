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
package rapture.dsl.serfun;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import rapture.common.Hose;
import rapture.common.SeriesValue;

/**
 * this class takes a number of stream hoses as inputs and turns them into a stream of strings.
 * Each string is the line for a CSVfile comprising the streams, except the first which is the headers
 * @author mel
 *
 */
public class CSVHose extends ComplexHose {
    private final ImmutableList<String> inputKeys;
    private Set<Integer> doneSet = Sets.newHashSet();
    private SeriesValue head[];
    private final String header[];
    private boolean headersDone = false;
    
    public static class Factory implements HoseFactory {
        @Override
        public Hose make(List<HoseArg> args) {
            checkArgument(args.size()%2 == 0, "Usage: series2csv [header stream]+");
            int size = args.size() / 2;
            String header[] = new String[size];
            Hose stream[] = new HoseArg[size];
            for(int pair = 0; pair < size; pair++) {
                HoseArg nextHeader = args.get(pair*2);
                HoseArg nextStream = args.get(pair*2+1);
                checkArgument(nextHeader.isString(), "series2csv: headers must be String values (pair#"+pair+")");
                header[pair] = nextHeader.asString();
                checkArgument(nextStream.isSeries(), "series2csv: scalar value not allowed as stream(pair#"+pair+")");
                stream[pair] = nextStream.asStream();
            }
            return new CSVHose(header, stream);
        }
    }
    
    public CSVHose(String header[], Hose stream[]) {
        super(header.length, 1); 
        head = new SeriesValue[header.length];
        this.header = header;
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (int i = 0; i < header.length; i++) {
            builder.add("in" + i);
            bind(stream[i], 0, i);
        }
        inputKeys = builder.build();
    }

    @Override
    public String getName() {
        return "series2csv();";
    }

    @Override
    public List<String> getInputKeys() {
        return inputKeys;
    }

    @Override
    public List<String> getOutputKeys() {
        return SIMPLEKEYLIST;
    }

    @Override
    public void pushValue(SeriesValue v, int index) {
        throw new UnsupportedOperationException("CSV Converted only works in pull mode");
    }

    @Override
    public void terminateStream(int index) {
        doneSet.add(index);
        if (doneSet.size() == upstream.length) {
            downstream[0].terminateStream();
        }
    }

    @Override
    public void terminateStream() {
        downstream[0].terminateStream();
    }

    @Override
    public SeriesValue pullValue(int index) {
        if (!headersDone) {
            headersDone = true;
            return headers();
        }
        StringBuilder sb = new StringBuilder();
        String date = primeHeads();
        if (date == null) {
            return null;
        } else {
            sb.append(date);
        }
        for(int i=0; i<head.length; i++) {
            if(dateMatch(i, date)) {
                sb.append(",");
                sb.append(escape(head[i].asString()));
                head[i] = null;
            } else {
                //TODO type sensitivity
                sb.append(",NaN");
            }
        }
        sb.append('\n');
        return new StringSeriesValue(sb.toString(), date);
    } 
    
    private SeriesValue headers() {
        StringBuilder sb = new StringBuilder();
        sb.append("Date");
        for(int i=0; i<header.length; i++) {
            sb.append(',');
            sb.append(escape(header[i]));
        }
        return new StringSeriesValue(sb.toString());
    }
    
    private String primeHeads() {
        String min = null;
        for(int i=0; i<head.length; i++) {
            if (head[i] == null) head[i] = upstream[i].pullValue();
            if (head[i] == null) continue;
            if (min == null) {
                min = head[i].getColumn();
            } else {
                if (min.compareTo(head[i].getColumn()) > 0) min = head[i].getColumn();
            }
        }
        return min;
    }
    
    private boolean dateMatch(int i, String date) {
        if (head[i] == null) return false;
        return date.equals(head[i].getColumn());
    }
    
    private String escape(String in) {
        // TODO protect against commas and newlines in values
        return in;
    }
}
