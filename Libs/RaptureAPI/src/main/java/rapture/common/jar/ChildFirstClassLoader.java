package rapture.common.jar;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import rapture.common.api.ScriptingApi;

/**
 * Load all jars referenced in the Rapture jar uris first before consulting the jars in the parent
 * 
 * @author dukenguyen
 * @param <K>
 *
 */
public class ChildFirstClassLoader extends AbstractClassLoader {

    private static final Logger log = Logger.getLogger(ChildFirstClassLoader.class);

    private ClassLoader system;
    public ChildFirstClassLoader(ClassLoader parent, ScriptingApi api, List<String> jarUris) throws ExecutionException {
        super(parent, api, jarUris);
        system = getSystemClassLoader();
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        log.debug("Calling loadClass() with classname: " + name);
        // 1. Check if the class has already been loaded
        // 2. Check the child ClassLoader
        // 3. Check the parent ClassLoader
        // 4. Check the system ClassLoader
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                log.debug("Not found in local");
            }
            if (c == null) {
                try {
                    c = super.loadClass(name, resolve);
                } catch (ClassNotFoundException e) {
                    log.debug("Not found in parent");
                }
                if (c == null) {
                    if (system != null) {
                        c = system.loadClass(name);
                    } else {
                        throw new ClassNotFoundException("SystemClassLoader not present.  Could not find class: " + name);
                    }
                }
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = this.getStreamForName(name);
        if (is != null) return is;
        return super.getResourceAsStream(name);
    }

}
