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
package rapture.series.mem;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import rapture.parser.CSVCallback;
import rapture.series.SeriesStore;

public class CSVSeriesCallback implements CSVCallback {
    private static Logger log = Logger.getLogger(CSVSeriesCallback.class);

    private boolean firstLine = true;
    private boolean typeLine; 
    private List<String> headers;
    private int counter = 0;
    private SeriesStore repo;
    private String currentColumn;
    private List<Type> types;
    private String prefix;
    
    public CSVSeriesCallback(CSVSeriesStore repo, boolean typeLine, String prefix) {
        this.repo = repo;
        this.typeLine = typeLine;
        if (typeLine) types = Lists.newArrayList();
        this.prefix = prefix;
    }

    @Override
    public void startNewLine() {
        if(firstLine) {
            if (headers == null) headers = Lists.newArrayList();
            else firstLine = false;
        } else if (typeLine) {
            typeLine = false;
        } else {
            counter = 0;
        }
    }

    @Override
    public void addCell(String cell) {
        if (firstLine) {
            headers.add(prefix + cell);
        } else if (typeLine) {
            types.add(typeDef(cell));
        } else {
            addValue(counter, cell);
            counter++;
        }
    }
    
    private Type typeDef(String typeDef) {
        if (typeDef.equals("double")) {
            return Type.DECIMAL;
        } else if (typeDef.equals("long")) {
            return Type.LONG;
        } else if (typeDef.equals("json")) {
            return Type.STRUCTURE;
        } else {
            return Type.STRING;
        }
    }

    public void addValue(int counter, String value) {
        switch (getType(counter)) {
            case STRING:
                repo.addStringToSeries(headers.get(counter), currentColumn, value);
                break;
            case DECIMAL:
                try {
                    repo.addDoubleToSeries(headers.get(counter), currentColumn, Double.parseDouble(value));
                } catch(Exception ex) {
                    log.error("Failure projecting CSV value " + value + "as double in series", ex);
                }
                break;
            case LONG:
                try {
                    repo.addLongToSeries(headers.get(counter), currentColumn, Long.parseLong(value));
                } catch(Exception ex) {
                    log.error("Failure projecting CSV value " + value + " as long in series", ex);
                }
                break;
            case STRUCTURE:
                try {
                    repo.addStructureToSeries(headers.get(counter), currentColumn, value);
                } catch(Exception ex) {
                    log.error("Failure projecting CSV value " + value + " as structure in series", ex);
                }
                break;
            case COLUMN:
                currentColumn = value;
                break;
        }        
    }
    
    public Type getType(int columnCount) {
        if (columnCount == 0) return Type.COLUMN;
        else if (types != null) return types.get(columnCount);
        else return Type.STRING;
    }
    
    public enum Type {
        DECIMAL, LONG, STRUCTURE, STRING, COLUMN
    };
}
