package rapture.kernel;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.testng.Assert;

import com.google.common.collect.ImmutableSet;

import rapture.common.CallingContext;
import rapture.common.RaptureField;
import rapture.common.RaptureFieldBand;
import rapture.common.RaptureFieldPath;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureGroupingFn;
import rapture.common.Scheme;
import rapture.common.api.FieldsApi;

public class FieldsApiImplTest {

    /**
     * This test fails. From what I can see the write occurs to a StorableRepo but the read goes to a regular Repository
     * As a result the listFieldsByUriPrefix call returns nothing.
     */
    @Ignore
    @Test
    public void testRap4050() {
        CallingContext callingContext = ContextFactory.getKernelUser();
        Kernel.initBootstrap();
        String uuid = UUID.randomUUID().toString();

        {
            FieldsApi impl = Kernel.getFields();
            String auth = Scheme.FIELD.toString() + "://" + uuid;            
            
            String authority = uuid;
            String name = "Sally";
            String category = "Drama";
            String description = "Actress";
            String units = "Oscars";
            RaptureGroupingFn fn = RaptureGroupingFn.NONE;
            List<RaptureFieldBand> fieldBands = Arrays.asList(new RaptureFieldBand());
            Set<RaptureFieldPath> fieldPaths = ImmutableSet.of(new RaptureFieldPath());
            
            RaptureField field = new RaptureField();
            field.setName(name);
            field.setAuthority(authority);
            field.setCategory(category);
            field.setDescription(description);
            field.setUnits(units);
            field.setGroupingFn(fn);
            field.setBands(fieldBands);
            field.setFieldPaths(fieldPaths);

            impl.putField(callingContext, field);
            Map<String, RaptureFolderInfo> rfi = impl.listFieldsByUriPrefix(callingContext, auth, -1);
            
            Assert.assertNotNull(rfi);
            Assert.assertFalse(rfi.isEmpty());
            Assert.assertEquals(rfi.get(0).getName(), "derek");
        }
    }
}
