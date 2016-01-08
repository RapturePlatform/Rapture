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
package reflex.util.function;

import static org.junit.Assert.assertTrue;

import org.antlr.runtime.tree.CommonTree;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import reflex.Function;
import reflex.util.NamespaceStack;

public class LanguageRegistryTest {
    LanguageRegistry registry = new LanguageRegistry();
    String namespacePrefix = "a.b";
    String name = "func";
    int numParams = 2;
    FunctionKey key = new FunctionKey(namespacePrefix, name, numParams);
    CommonTree tree = new CommonTree();
    private NamespaceStack stack = new NamespaceStack();
    Function function = new Function(name, tree, null, stack );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
        registry.registerFunction(key, function);
        assertTrue(registry.hasFunction(namespacePrefix, name, numParams));
    }
}
