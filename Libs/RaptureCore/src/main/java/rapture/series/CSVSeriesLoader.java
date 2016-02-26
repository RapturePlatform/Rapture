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
package rapture.series;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpSeriesApi;

public class CSVSeriesLoader {
    private final int sortIndex;
    private final Set<Binding> bindings;
    private final int headerCount;
    private static HttpSeriesApi api;
    
    public static void setApi(HttpSeriesApi seriesApi) {
        api = seriesApi;
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    private CSVSeriesLoader(int headerCount, int sortIndex, Set<Binding> bindings) {
        this.headerCount = headerCount;
        this.sortIndex = sortIndex;
        this.bindings = bindings;
    }
    
    public void load(String filename) throws IOException {
        load(new File(filename));
    }
    
    public void load(String filename, boolean showProgress) throws IOException {
        load(new File(filename), false);
    }
    
    private String sanitize(String in) {
        char buf[] = in.trim().toCharArray();
        for(int i = 0; i < buf.length; i++) {
            if (!Character.isJavaIdentifierPart(buf[i])) {
                buf[i] = '_';
            }
        }
        return new String(buf);
    }
    
    public void load(File f) throws IOException {
        load(f, false);
    }
    
    public void load(File f, boolean showProgress) throws IOException {
        BufferedReader in = null;
        String header[] = null;
        try {
            in = new BufferedReader(new FileReader(f));

            for (int i = 1; i <= headerCount; i++) {
                String headers = in.readLine();
                if (i < headerCount) {
                    continue;
                }
                header = headers.split(",");
            }
            
            for(int i = 0; i < header.length; i++) {
                header[i] = sanitize(header[i]);
            }

            int count = 0;
            while (true) {
                String line = in.readLine();
                //TODO MEL deal with escape syntax
                String value[] = line.split(",");
                //TODO MEL transform date from and to formats
                String column = value[sortIndex];
                for(Binding binding:bindings) {
                    count++;
                    if (showProgress && (count == 1 || count%10 == 0)) {
                        System.out.println("\tRow "+count);
                    }
                    if (binding.column == -1) {
                        for (int i=0; i<value.length; i++) {
                            if (i == sortIndex) {
                                continue;
                            }
                            double v;
                            try {
                                v = Double.parseDouble(value[i]);
                            } catch (NumberFormatException ex) {
                                v = Double.NaN;
                            }
                            api.addDoubleToSeries(RaptureURI.builder(Scheme.SERIES, binding.authority).docPath(binding.path + header[i]).asString(), column, v);
                        }
                    }
                    else switch(binding.type) {
                        case DECIMAL:
                            double v;
                            try {
                                v = Double.parseDouble(value[binding.column]);           
                            } catch (NumberFormatException ex) {
                                v = Double.NaN;
                            }
                            api.addDoubleToSeries(RaptureURI.builder(Scheme.SERIES, binding.authority).docPath(binding.path).asString(), column, v);
                            break;
                        case LONG:
                            try {
                                long l = Long.parseLong(value[binding.column]);
                                api.addLongToSeries(RaptureURI.builder(Scheme.SERIES, binding.authority).docPath(binding.path).asString(), column, l);
                            } finally {
                                // ignore unparsable items
                            }
                            break;
                        case STRING:
                            String s = value[binding.column];
                            api.addStringToSeries(RaptureURI.builder(Scheme.SERIES, binding.authority).docPath(binding.path).asString(), column, s);
                            break;
                    }
                }         
            }
        } finally {
            if (in != null) in.close();
        }
    }
    
    public static class Builder {
        private String authority;
        private String repo;
        private int sortIndex = 0;
        private int headerCount = 1;
        private Set<Binding> bindings = new HashSet<Binding>();

        public Builder setAuthority(String authority) {
            this.authority = authority;
            return this;
        }

        public Builder setRepo(String repo) {
            this.repo = repo;
            return this;
        }

        public Builder setSortColumn(int index) {
            this.sortIndex = index;
            return this;
        }

        public Builder bindDecimalColumn(int index, String path) {
            return bindColumn(index, path, Binding.Type.DECIMAL);
        }
        
        public Builder bindLongColumn(int index, String path) {
            return bindColumn(index, path, Binding.Type.LONG);
        }
        
        public Builder bindStringColumn(int index, String path) {
            return bindColumn(index, path, Binding.Type.STRING);
        }
        
        private Builder bindColumn(int index, String path, Binding.Type type) {
            bindings.add(new Binding(index, authority, path, type));
            return this;
        }
        
        public Builder bindDecimalColumn(int index, String authority, String path) {
            return bindColumn(index, authority, repo, Binding.Type.DECIMAL);
        }
        
        public Builder bindLongColumn(int index, String authority, String path) {
            return bindColumn(index, authority, path, Binding.Type.LONG);
        }
        
        public Builder bindStringColumn(int index, String authority, String path) {
            return bindColumn(index, authority, path, Binding.Type.STRING);
        }

        private Builder bindColumn(int index, String authority, String path, Binding.Type type) {
            bindings.add(new Binding(index, authority, path, type));
            return this;
        }
        
        public Builder bindAllAsDecimals(String prefix) {
            bindings.add(new Binding(-1, authority, prefix, Binding.Type.DECIMAL));
            return this;
        }

        public CSVSeriesLoader build() {
            return new CSVSeriesLoader(headerCount, sortIndex, bindings);
        }      
    }
    
    private static class Binding {
        public final int column;
        public final String authority;
        public final String path;
        enum Type {
            DECIMAL,
            LONG,
            STRING
        };
        
        public final Type type;
        
        public Binding(int column, String authority, String path, Type type) {
            this.column = column;
            this.authority = authority;
            this.path = path;
            this.type = type;
        }
    }
}
