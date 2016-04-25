package rapture.kernel.jar;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;

import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class AbstractClassLoaderTest {

    protected CallingContext ctx = ContextFactory.getKernelUser();
    protected static final String JAR_URI1 = "jar://somerepo/test.jar";
    protected static final String JAR_URI2 = "jar://anotherrepo/xmlunit-1.6.jar";

    @Before
    public void setup() throws IOException {
        Kernel.getKernel().restart();
        Kernel.initBootstrap();
        Kernel.setCategoryMembership("alpha");

        // this test.jar is created in the WorkflowsCore project by running the testJar gradle task
        // it has references to xmlunit so we add that jar to the classloader below
        byte[] jarBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream("/workflowTestJars/test.jar"));
        Kernel.getJar().putJar(ctx, JAR_URI1, jarBytes);
        assertArrayEquals(jarBytes, Kernel.getJar().getJar(ctx, JAR_URI1).getContent());

        jarBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream("/workflowTestJars/xmlunit-1.6.jar"));
        Kernel.getJar().putJar(ctx, JAR_URI2, jarBytes);
        assertArrayEquals(jarBytes, Kernel.getJar().getJar(ctx, JAR_URI2).getContent());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testClassLoader(AbstractClassLoader acl1, AbstractClassLoader acl2)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ExecutionException, IOException {

        Class class1 = acl1.loadClass("rapture.dp.ClassLoaderTest1");
        Class class2 = acl1.loadClass("rapture.dp.ClassLoaderTest2");

        AbstractInvocable a1 = (AbstractInvocable) class1.getConstructor(String.class, String.class).newInstance(null, null);
        AbstractInvocable a2 = (AbstractInvocable) class2.getConstructor(String.class, String.class).newInstance(null, null);
        assertEquals("1", a1.invoke(ctx));
        assertEquals("1", a2.invoke(ctx));
        assertEquals("2", a1.invoke(ctx));
        assertEquals("2", a2.invoke(ctx));

        // Using a new classloader you should lose the static variables and the counter is reset back to 0
        Class class1x = acl2.loadClass("rapture.dp.ClassLoaderTest1");
        Class class2x = acl2.loadClass("rapture.dp.ClassLoaderTest2");

        AbstractInvocable a1x = (AbstractInvocable) class1x.getConstructor(String.class, String.class).newInstance(null, null);
        AbstractInvocable a2x = (AbstractInvocable) class2x.getConstructor(String.class, String.class).newInstance(null, null);
        assertEquals("1", a1x.invoke(ctx));
        assertEquals("1", a2x.invoke(ctx));

        // These values should still be there in the old classloader
        assertEquals("3", a1.invoke(ctx));
        assertEquals("3", a2.invoke(ctx));

        acl1.close();
        acl2.close();
    }

}
