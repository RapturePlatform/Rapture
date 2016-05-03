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
package external.multipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class ParamGetter {
    private static final Logger log = Logger.getLogger(ParamGetter.class.getName());

    public static Map<String, Object> getProps(MultipartParser parser) throws IOException {
        Map<String, Object> props = new HashMap<>();
        while (true) {
            System.out.println("Looping in getProps");
            Part p = parser.readNextPart();
            if (p == null) {
                break;
            }
            if (p.isParam()) {
                System.out.println("Got parameter - " + p.getName());
                props.put(p.getName(), ((ParamPart) p).getStringValue());
            } else {
                if (p instanceof FilePart) {
                    FilePart fp = (FilePart) p;
                    System.out.println("Would work with a file part");
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    fp.write(bao);
                    String content = new String(bao.toByteArray());
                    props.put(p.getName(), content);
                }
            }
        }
        return props;
    }

    public static Map<String, Object> getProps(String url) {
        Map<String, Object> props = new HashMap<>();
        String[] parts = url.split("&");
        for (String p : parts) {
            String[] bits = p.split("=");
            if (bits.length == 1) {
                props.put(bits[0], "");
            } else {
                props.put(bits[0], bits[1]);
                // RAP-3296 review this code and refactor as needed-- likely bug
                if (bits[1].equals("RUNVIEW") || bits[1].equals("CREATESCRIPT") || bits[1].equals("CREATETYPE") || bits[1].equals("CREATEIDGEN")
                        || bits[1].equals("RUNOPERATION")) {
                    String tmpstr = parts[1].substring(7);
                    log.info(tmpstr);
                    props.put("PARAMS", tmpstr);
                    break;
                }
            }
        }
        return props;
    }
}
