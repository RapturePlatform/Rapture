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
package rapture.plugin.app;

import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.tuple.Triple;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.plugin.install.PluginSandbox;
import rapture.plugin.install.PluginSandboxItem;

public class JarBasedSandboxLoader implements SandboxLoader {

    @Override
    public void loadSandboxFromEntries(String root, String variant, PluginSandbox sandbox)
            throws Exception {
        String[] files = getResourceFiles(root, variant);

        for (String f : files) {
            boolean isVariant = false;
            String path = "/" + f;
            System.out.println("Path is " + path);
            // remove PLUGIN/
            f = f.substring(7);
            if (f.startsWith(PluginSandbox.CONTENT)) {
                // remove content/ from filename
                f = f.substring(PluginSandbox.CONTENT.length());
            } else if (f.startsWith(variant + "/")) {
                // remove <variant>/ from filename
                f = f.substring(variant.length() + 1);
                isVariant = true;
            }
            System.out.println("File is " + f);
            Triple<String, String, Scheme> trip = PluginSandboxItem
                    .extractScheme(f);
            System.out.println("Triple is " + trip.toString());

            // Triple is (/curtisweb/,null,blob)
            RaptureURI uri = RaptureURI.createFromFullPathWithAttribute(
                    trip.getLeft(), trip.getMiddle(), trip.getRight());
            System.out.println("URI is " + uri.toString());
            sandbox.makeItemFromInternalEntry(uri, SelfInstaller.class.getResourceAsStream(path),
                    isVariant ? variant : null);
        }
    }

    private String[] getResourceFiles(String path, String variant) throws Exception {
        URL dirURL = SelfInstaller.class.getClass().getResource(path);
        String jarPath = dirURL.getPath().substring(5,
                dirURL.getPath().indexOf("!")); // strip out only the JAR file
        JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
        Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in
                                                       // jar
        Set<String> result = new HashSet<String>(); // avoid duplicates in case
                                                    // it is a subdirectory
        String checkPath = path.substring(1);
        String checkVariant = variant != null ? checkPath.replaceFirst(PluginSandbox.CONTENT, variant).concat("/") : null;
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (!name.endsWith("/") && // don't return directories
                    (name.startsWith(checkPath) || (checkVariant != null && name.startsWith(checkVariant)))) { // filter according to the path
                result.add(name);
            }
        }
        jar.close();
        return result.toArray(new String[result.size()]);

    }
}
