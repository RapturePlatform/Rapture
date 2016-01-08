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
package com.incapture.rapgen.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import rapture.common.RaptureJob;
import rapture.common.dp.Workflow;
import rapture.common.model.RaptureUser;
import rapture.persistence.storable.mapper.WorkflowExtended;

import org.junit.Test;

/**
 * @author bardhi
 * @since 7/21/15.
 */
public class StorableMappersLoaderTest {

    @Test
    public void testGetStorableMappers() throws Exception {
        StorableSerDeserRepo repo = StorableMappersLoader.loadSerDeserHelpers();

        assertNotNull(repo.getSerDeserHelper(Workflow.class));
        assertNotNull(repo.getSerDeserHelper(RaptureUser.class));
        assertNotNull(repo.getSerDeserHelper(RaptureJob.class));
        assertEquals(3, repo.getAll().size());

        assertEquals(WorkflowExtended.class, repo.getExtendedClass(Workflow.class));
    }
}
