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
package rapture.dsl.iqry;

import java.util.Collection;

public class IndexQuery {
    @Override
    public String toString() {
        return "[select " + ((distinct) ? "distinct" : "") + select + ((where == null) ? "" : " where " + where) + " order by "
                + ((direction != OrderDirection.NONE) ? orderBy.toString() : "") + " limit " + limit + " skip " + skip + "]";
    }

    private SelectList select;
    private boolean distinct = false;
    private WhereClause where;

    private SelectList orderBy = new SelectList();
    private OrderDirection direction = OrderDirection.NONE;

    public int getLimit() {
        return limit;
    }

    private int limit;
    private int skip = 0;

    public SelectList getOrderBy() {
        return orderBy;
    }

    public OrderDirection getDirection() {
        return direction;
    }

    public IndexQuery setSelect(SelectList select) {
        this.select = select;
        return this;

    }

    public IndexQuery setSelect(String select) {
        this.select = new SelectList(select);
        return this;
    }

    public IndexQuery setSelect(Collection<String> select) {
        this.select = new SelectList(select);
        return this;
    }

    public IndexQuery setWhere(WhereClause where) {
        this.where = where;
        return this;
    }

    public IndexQuery addWhereStatement(WhereJoiner joiner, WhereStatement statement) {
        getWhere().appendStatement(joiner, statement);
        return this;
    }

    public IndexQuery setOrderBy(SelectList orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public IndexQuery setOrderDirection(OrderDirection direction) {
        this.direction = direction;
        return this;
    }

    public SelectList getSelect() {
        if (select == null) select = new SelectList();
        return select;
    }

    public WhereClause getWhere() {
        if (where == null) where = new WhereClause();
        return where;
    }

    public IndexQuery setDistinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setLimit(int limit) {
        this.limit = Math.abs(limit);
    }
    
    public void setSkip(int skip) {
        this.skip = Math.abs(skip);
    }
    
    public int getSkip() {
    	return skip;
    }
}
