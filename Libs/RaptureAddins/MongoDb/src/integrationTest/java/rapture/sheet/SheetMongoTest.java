/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package rapture.sheet;

import org.junit.Before;

import rapture.config.MultiValueConfigLoader;
import rapture.config.ValueReader;
import rapture.kernel.sheet.SheetContract;
import rapture.repo.SheetRepo;

public class SheetMongoTest extends SheetContract {

    @Override
    protected SheetRepo getSheetRepo() {
        String authority = "testAuthority";
        String config = "SHEET {} using MONGODB { prefix=\"" + "sheet.testalicious.prefix" + "\"}";
        ;
        return new SheetRepo(SheetStoreFactory.getSheetStore(authority, config));
    }

    @Before
    public void setupMongo() {
        MultiValueConfigLoader.setEnvReader(new ValueReader() {
            @Override
            public String getValue(String property) {
                if (property.equals("MONGODB-DEFAULT")) {
                    return "mongodb://test:test@localhost/test";
                }
                return null;
            }
        });
    }

}
