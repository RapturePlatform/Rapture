/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.common;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.util.StringUtil;

public class RaptureCubeResultTest {
    @Test
    public void testSimple() {
        List<String> colNames = StringUtil.list("A,B");
        List<String> groupNames = StringUtil.list("1,2,3");

        RaptureCubeResult res = new RaptureCubeResult();

        List<RaptureField> colFieldInfo = new ArrayList<RaptureField>();
        RaptureField AColumn = new RaptureField();
        AColumn.setCategory("CATA");
        AColumn.setDescription("Description of Field A");
        AColumn.setGroupingFn(RaptureGroupingFn.AVERAGE);
        AColumn.setUnits("duration");
        colFieldInfo.add(AColumn);
        RaptureField BColumn = new RaptureField();
        BColumn.setCategory("CATB");
        BColumn.setDescription("Description of Field B");
        BColumn.setGroupingFn(RaptureGroupingFn.SUM);
        BColumn.setUnits("mn");
        colFieldInfo.add(BColumn);

        List<String> grpResults1 = StringUtil.list("ONE,TWO,THREE");
        List<String> colResults1 = StringUtil.list("1.0,2.0");

        List<String> grpResults2 = StringUtil.list("ONE,BETA,THREE");
        List<String> colResults2 = StringUtil.list("10.0,20.0");

        List<String> grpResults3 = StringUtil.list("ONE,BETA,FOUR");
        List<String> colResults3 = StringUtil.list("12.0,3.0");

        res.setColFieldInfo(colFieldInfo);
        res.addEntry("1", grpResults1, colResults1);
        res.addEntry("2", grpResults2, colResults2);
        res.addEntry("3", grpResults3, colResults3);

        res.setColumnNames(colNames);
        res.setGroupNames(groupNames);

        // Finally we want to sort the output

        res.finish();

        System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(res)));
    }
}
