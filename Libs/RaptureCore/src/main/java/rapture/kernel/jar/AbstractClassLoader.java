package rapture.kernel.jar;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;

/**
 * Class loader for workflows that uses Rapture's internal Jar system to add dependencies. Dependencies are listed in the workflow definition at either the step
 * or workflow level. Step-level dependencies get priority over workflow-level dependencies. This is an abstract class. The concrete implementations are in
 * ParentFirstClassLoader and ChildFirstClassLoader which differ in terms of where jars are loaded first.
 * 
 * @author dukenguyen
 *
 */
public abstract class AbstractClassLoader extends URLClassLoader {

    private static final Logger log = Logger.getLogger(AbstractClassLoader.class);

    protected CallingContext ctx;
    protected Map<String, String> classNameMap = new HashMap<>();

    public AbstractClassLoader(ClassLoader parent, CallingContext ctx, List<String> jarUris) throws ExecutionException {
        super(new URL[0], parent);
        this.ctx = ctx;
        if (CollectionUtils.isNotEmpty(jarUris)) {
            // reverse the list, so that jars given first take precedence since the map will overwrite duplicate names last
            Collections.reverse(jarUris);
            for (String jarUri : jarUris) {
                List<String> expandedUris = JarUtils.expandWildcardUri(ctx, jarUri);
                for (String expandedUri : expandedUris) {
                    List<String> classNames = JarCache.getInstance().getClassNames(ctx, expandedUri);
                    for (String className : classNames) {
                        classNameMap.put(className, expandedUri);
                    }
                }
            }
        }
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        log.debug("findClass() for className: " + className);
        String jarUri = classNameMap.get(className);
        if (StringUtils.isBlank(jarUri)) {
            log.debug("Not found, delegating to parent: " + className);
            return super.findClass(className);
        }
        try {
            byte[] classBytes = JarCache.getInstance().getClassBytes(ctx, jarUri, className);
            return defineClass(className, classBytes, 0, classBytes.length);
        } catch (ExecutionException e) {
            throw new ClassNotFoundException("Could not get class bytes from the cache", e);
        }
    }
}