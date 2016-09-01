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
package rapture.field;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rapture.common.RaptureField;
import rapture.common.RaptureFieldTransform;
import rapture.common.RaptureStructure;
import rapture.common.impl.jackson.JacksonUtil;

public class ResourceLoader implements FieldLoader, StructureLoader, ScriptLoader, FieldTransformLoader {

    @Override
    public RaptureStructure getStructure(String uri) {
        String val = getResourceAsString(this, "/structure" + uri + ".json");
        return JacksonUtil.objectFromJson(val, RaptureStructure.class);
    }
    
    @Override
    public RaptureField getField(String uri) {
        String val = getResourceAsString(this, "/field" + uri + ".json");
        return JacksonUtil.objectFromJson(val, RaptureField.class);
       
    }
    
    public String getData(String uri) {
        return getResourceAsString(this, "/data" + uri + ".json");
    }
    
    @Override
    public String getScript(String uri) {
        return getResourceAsString(this, "/script" + uri + ".script");
    }
    
    @Override
    public RaptureFieldTransform getFieldTransform(String uri) {
        String val = getResourceAsString(this, "/fieldtransform" + uri + ".json");
        return JacksonUtil.objectFromJson(val, RaptureFieldTransform.class);
    }
    
    @Override
    public List<String> getFieldTransforms(String prefix) {
        try {
            String p =  "/fieldtransform";
            List<String> ret = getResources(null, p + prefix);
            Stream<String> mapped = ret.stream().map(e -> e.substring(p.length()));
            return mapped.collect(Collectors.toList());
        } catch(java.io.IOException e) {
            return null;
        }
    }
    
    public static List<String> getResources(Object context, String path) throws java.io.IOException {
        Enumeration<URL>  resources = (context == null ? ResourceLoader.class : context.getClass()).getClassLoader().getResources(path);
        // For now the above will be zero size, so try the file route
        if (!resources.hasMoreElements()) {
            File f = new File("src/test/resources" + path);
            //System.out.println(f.getAbsolutePath());
            String[] files = f.list();
            List<String> ret = new ArrayList<String>();
            for(String fi : files) {
                int extPoint = fi.indexOf('.');
                String uri = path + "/" + fi.substring(0, extPoint);
                ret.add(uri);
            }
            return ret;
        }
        return null;
    }
    
    public static String getResourceAsString(Object context, String path) {
        InputStream is = (context == null ? ResourceLoader.class : context.getClass()).getResourceAsStream(path);

        // Can't open as a resource - try as a file
        if (is == null) {
            File f = new File("src/test/resources" + path);
            String s = f.getAbsolutePath();
            //System.out.println(s);
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                System.out.println("Problem loading file " + e.getMessage());
            }
        }

        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return writer.toString();
        }
        // Can't open. Return null so that the caller doesn't assume we
        // succeeded.
        return null;
    }

   
}
