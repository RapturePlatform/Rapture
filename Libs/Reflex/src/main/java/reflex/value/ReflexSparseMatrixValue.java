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
package reflex.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reflex.value.internal.ReflexNullValue;

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;
import com.google.common.collect.TreeBasedTable;

/**
 * For now, implement a sparse matrix as simply a list
 * of coordinate points (each coordinate being a dimension-tuple, with a ReflexValue as the value)
 * 
 * Also store the unique set of keys for each dimension, so that (a) it can be sorted and (b) a non-sparse
 * matrix can be produced as output.
 * 
 * The sparse matrix will return NULL for values not set
 * 
 * @author alanmoore
 *
 */
public class ReflexSparseMatrixValue {
	private Table<ReflexValue, ReflexValue, ReflexValue> table;
	private List<ReflexValue> rowOrder;
	private List<ReflexValue> colOrder;
	
	public ReflexSparseMatrixValue(int dimension) {
		table = TreeBasedTable.create();
		rowOrder = new ArrayList<ReflexValue>();
		colOrder = new ArrayList<ReflexValue>();
	}
	
	public void set(ReflexValue row, ReflexValue column, ReflexValue value) {
		table.put(row, column, value);
		if (!rowOrder.contains(row)) {
		    rowOrder.add(row);
		}
		if (!colOrder.contains(column)) {
		    colOrder.add(column);
		}
	}
	
	public ReflexValue get(ReflexValue row, ReflexValue column) {
		ReflexValue ret = table.get(row, column);
		if (ret == null) {
			ret = new ReflexNullValue();
		}
		return ret;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(ReflexValue c : colOrder) {
			sb.append(",");
			sb.append(maybeQuote(c.asString()));			
		}
		sb.append("\n");
		for(ReflexValue row : rowOrder) {
			sb.append(maybeQuote(row.asString()));
			for(ReflexValue c : colOrder) {
				sb.append(",");
				ReflexValue v = table.get(row, c);
				if (v != null) {
					sb.append(maybeQuote(v.asString()));
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private String maybeQuote(String theString) {
		if (theString.contains(",") || theString.contains("\"") || theString.contains("\'")) {
			return "\"" + theString + "\"";
		}
		return theString;
	}

	public Set<ReflexValue> getRowSet() {
		return table.rowKeySet();
	}
	
	public Set<ReflexValue> getColumnSet() {
		return table.columnKeySet();
	}

	public List<ReflexValue> getColumnOrder() {
	    return colOrder;
	}
	
	public List<ReflexValue> getRowOrder() {
	    return rowOrder;
	}
	
	public Set<Cell<ReflexValue, ReflexValue, ReflexValue>> getCells() {
		return table.cellSet();
	}

    public void merge(ReflexSparseMatrixValue asMatrix) {
        // Merge one into the other (the second wins)
        for(ReflexValue r : asMatrix.getRowOrder()) {
            for(ReflexValue c : asMatrix.getColumnOrder()) {
                set(r,c, asMatrix.get(r, c));
            }
        }
    }

    public ReflexSparseMatrixValue transpose() {
        ReflexSparseMatrixValue ret = new ReflexSparseMatrixValue(2);
        ret.table = Tables.transpose(table);
        return ret;
    }

    public ReflexSparseMatrixValue filter(SparseFilter sparseFilter) {
       // Create a new ReflexSparseMatrix value with rows filtered according to whether
        // the sparsefilter returns true
        
        ReflexSparseMatrixValue ret = new ReflexSparseMatrixValue(2);
        for(ReflexValue r : getRowOrder()) {
            Map<ReflexValue, ReflexValue> vals = table.row(r);
            if (sparseFilter.filter(vals)) {
                for(ReflexValue c : getColumnOrder()) {
                    ret.set(r, c, vals.get(c));
                }
            }
        }
        return ret;
    }

    public void copyOrder(ReflexSparseMatrixValue smv) {
        this.colOrder = smv.colOrder;
        this.rowOrder = smv.rowOrder;
    }

}
