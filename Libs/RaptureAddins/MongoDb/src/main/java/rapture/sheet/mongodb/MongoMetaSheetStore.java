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
package rapture.sheet.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.RaptureSheetNote;
import rapture.common.RaptureSheetRange;
import rapture.common.RaptureSheetScript;
import rapture.common.RaptureSheetStyle;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.mongodb.MongoDBFactory;
import rapture.mongodb.MongoRetryWrapper;
import rapture.util.IDGenerator;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.WriteResult;

/**
 * Stores meta information for a sheet (scripts, ranges, styles)
 * 
 * Each has a unique type, a sheet name, and an individual name. + content
 * specific to the type
 * 
 * @author amkimian
 * 
 */
public class MongoMetaSheetStore {
    private String tableName;
    private String instanceName = "default";
    private static final String PREFIX = "prefix";
    private static final String VALUE = "v";
    private static final String KEY = "key";
    private static final String NAME = "name";
    private static final String TYPE = "T";
    private static final String STYLE = "S";
    private static final String SCRIPT = "R";
    private static final String RANGE = "G";
    private static final String NOTE = "N";
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(MongoMetaSheetStore.class);

    public MongoMetaSheetStore() {
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setConfig(Map<String, String> config) {
        tableName = config.get(PREFIX) + "_meta";
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.ensureIndex(KEY);
    }

    public void drop() {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.drop();
        collection.dropIndex(KEY);
    }

    /**
     * Styles are stored with type=STYLE, and the content is a JSONed
     * RaptureSheetStyle
     *
     * @param name
     * @return
     */
    public List<RaptureSheetStyle> getSheetStyles(final String name) {
        MongoRetryWrapper<List<RaptureSheetStyle>> wrapper = new MongoRetryWrapper<List<RaptureSheetStyle>>() {

            public DBCursor makeCursor() {
                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                BasicDBObject query = new BasicDBObject();
                query.append(TYPE, STYLE);
                query.append(KEY, name);
                BasicDBObject values = new BasicDBObject();
                values.append(VALUE, 1);
                return collection.find(query, values);
            }

            public List<RaptureSheetStyle> action(DBCursor cursor) {
                List<RaptureSheetStyle> ret = new ArrayList<RaptureSheetStyle>();
                while (cursor.hasNext()) {
                    BasicDBObject obj = (BasicDBObject) cursor.next();
                    String content = obj.getString(VALUE);
                    RaptureSheetStyle style = JacksonUtil.objectFromJson(content, RaptureSheetStyle.class);
                    ret.add(style);
                }
                return ret;
            }
        };
        return wrapper.doAction();
    }

    public Boolean deleteSheetStyle(String name, String styleName) {
        return standardDelete(name, styleName, STYLE);
    }

    public RaptureSheetStyle putSheetStyle(String name, RaptureSheetStyle style) {
        style.setName(style.getName());
        String content = JacksonUtil.jsonFromObject(style);
        BasicDBObject query = new BasicDBObject();
        query.append(TYPE, STYLE);
        query.append(KEY, name);
        query.append(NAME, style.getName());

        BasicDBObject write = new BasicDBObject();
        write.append(TYPE, STYLE);
        write.append(KEY, name);
        write.append(NAME, style.getName());
        write.append(VALUE, content);
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.update(query, write, true, false);
        return style;
    }

    public List<RaptureSheetScript> getSheetScripts(final String name) {
        MongoRetryWrapper<List<RaptureSheetScript>> wrapper = new MongoRetryWrapper<List<RaptureSheetScript>>() {

            public DBCursor makeCursor() {
                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                BasicDBObject query = new BasicDBObject();
                query.append(TYPE, SCRIPT);
                query.append(KEY, name);
                BasicDBObject values = new BasicDBObject();
                values.append(VALUE, 1);
                return collection.find(query, values);
            }

            public List<RaptureSheetScript> action(DBCursor cursor) {
                List<RaptureSheetScript> ret = new ArrayList<RaptureSheetScript>();
                while (cursor.hasNext()) {
                    BasicDBObject obj = (BasicDBObject) cursor.next();
                    String content = obj.getString(VALUE);
                    RaptureSheetScript script = JacksonUtil.objectFromJson(content, RaptureSheetScript.class);
                    ret.add(script);
                }
                return ret;
            }
        };
        return wrapper.doAction();
    }

    public Boolean deleteSheetScript(String name, String scriptName) {
        return standardDelete(name, scriptName, SCRIPT);

    }

    public RaptureSheetScript putSheetScript(String name, String scriptName, RaptureSheetScript script) {
        script.setName(scriptName);
        String content = JacksonUtil.jsonFromObject(script);
        BasicDBObject query = new BasicDBObject();
        query.append(TYPE, SCRIPT);
        query.append(KEY, name);
        query.append(NAME, scriptName);

        BasicDBObject write = new BasicDBObject();
        write.append(TYPE, SCRIPT);
        write.append(KEY, name);
        write.append(NAME, scriptName);
        write.append(VALUE, content);
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.update(query, write, true, false);
        return script;
    }

    public List<RaptureSheetRange> getSheetNamedSelections(final String name) {
        MongoRetryWrapper<List<RaptureSheetRange>> wrapper = new MongoRetryWrapper<List<RaptureSheetRange>>() {

            public DBCursor makeCursor() {
                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                BasicDBObject query = new BasicDBObject();
                query.append(TYPE, RANGE);
                query.append(KEY, name);
                BasicDBObject values = new BasicDBObject();
                values.append(VALUE, 1);
                return collection.find(query, values);
            }

            public List<RaptureSheetRange> action(DBCursor cursor) {
                List<RaptureSheetRange> ret = new ArrayList<RaptureSheetRange>();
                while (cursor.hasNext()) {
                    BasicDBObject obj = (BasicDBObject) cursor.next();
                    String content = obj.getString(VALUE);
                    RaptureSheetRange range = JacksonUtil.objectFromJson(content, RaptureSheetRange.class);
                    ret.add(range);
                }
                return ret;
            }
        };
        return wrapper.doAction();
    }

    public Boolean deleteSheetNamedSelection(String name, String rangeName) {
        return standardDelete(name, rangeName, RANGE);
    }

    public RaptureSheetRange putSheetNamedSelection(String name, String rangeName, RaptureSheetRange range) {
        range.setName(rangeName);
        String content = JacksonUtil.jsonFromObject(range);
        BasicDBObject query = new BasicDBObject();
        query.append(TYPE, RANGE);
        query.append(KEY, name);
        query.append(NAME, rangeName);

        BasicDBObject write = new BasicDBObject();
        write.append(TYPE, RANGE);
        write.append(KEY, name);
        write.append(NAME, rangeName);
        write.append(VALUE, content);
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.update(query, write, true, false);
        return range;
    }

    private Boolean standardDelete(String name, String itemName, String type) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.append(TYPE, type);
        query.append(KEY, name);
        query.append(NAME, itemName);
        WriteResult res = collection.remove(query);
        return res.getN() != 0;
    }

    public RaptureSheetScript getSheetScript(String name, String scriptName) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.append(TYPE, SCRIPT);
        query.append(KEY, name);
        query.append(NAME, scriptName);
        BasicDBObject values = new BasicDBObject();
        values.append(VALUE, 1);
        BasicDBObject value = (BasicDBObject) collection.findOne(query, values);
        if (value != null) {
            String content = value.getString(VALUE);
            RaptureSheetScript script = JacksonUtil.objectFromJson(content, RaptureSheetScript.class);
            return script;
        }
        return null;
    }

    public void cloneSheet(final String srcName, final String targetName) {
        // Everything with the KEY being the srcName, modify the source name to
        // targetName and store it back
        final DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        MongoRetryWrapper<Object> wrapper = new MongoRetryWrapper<Object>() {

            public DBCursor makeCursor() {

                BasicDBObject query = new BasicDBObject();
                query.append(KEY, srcName);
                return collection.find(query);
            }

            public Object action(DBCursor cursor) {
                while (cursor.hasNext()) {
                    BasicDBObject object = (BasicDBObject) cursor.next();
                    object.put(KEY, targetName);
                    object.remove("_id");
                    collection.insert(object);
                }

                return null;
            }
        };
        @SuppressWarnings("unused")
        Object o = wrapper.doAction();
    }

    public RaptureSheetRange getSheetNamedSelection(String name, String rangeName) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.append(TYPE, RANGE);
        query.append(KEY, name);
        query.append(NAME, rangeName);
        BasicDBObject values = new BasicDBObject();
        values.append(VALUE, 1);
        BasicDBObject value = (BasicDBObject) collection.findOne(query, values);
        if (value != null) {
            String content = value.getString(VALUE);
            RaptureSheetRange range = JacksonUtil.objectFromJson(content, RaptureSheetRange.class);
            return range;
        }
        return null;
    }

    public List<RaptureSheetNote> getSheetNotes(final String name) {
        MongoRetryWrapper<List<RaptureSheetNote>> wrapper = new MongoRetryWrapper<List<RaptureSheetNote>>() {

            public DBCursor makeCursor() {

                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                BasicDBObject query = new BasicDBObject();
                query.append(TYPE, NOTE);
                query.append(KEY, name);
                BasicDBObject values = new BasicDBObject();
                values.append(VALUE, 1);
                return collection.find(query, values);
            }

            public List<RaptureSheetNote> action(DBCursor cursor) {
                List<RaptureSheetNote> ret = new ArrayList<RaptureSheetNote>();
                while (cursor.hasNext()) {
                    BasicDBObject obj = (BasicDBObject) cursor.next();
                    String content = obj.getString(VALUE);
                    RaptureSheetNote note = JacksonUtil.objectFromJson(content, RaptureSheetNote.class);
                    ret.add(note);

                }
                return ret;
            }
        };
        return wrapper.doAction();
    }

    public Boolean deleteSheetNote(String name, String noteId) {
        return standardDelete(name, noteId, NOTE);
    }

    public RaptureSheetNote putSheetNote(String name, RaptureSheetNote note) {
        if (note.getId() == null || note.getId().isEmpty()) {
            note.setId(IDGenerator.getUUID());
        }
        String content = JacksonUtil.jsonFromObject(note);
        BasicDBObject query = new BasicDBObject();
        query.append(TYPE, NOTE);
        query.append(KEY, name);
        query.append(NAME, note.getId());

        BasicDBObject write = new BasicDBObject();
        write.append(TYPE, NOTE);
        write.append(KEY, name);
        write.append(NAME, note.getId());
        write.append(VALUE, content);
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.update(query, write, true, false);
        return note;
    }

    public void removeAll(String sheetName) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, sheetName);
        collection.remove(query);
    }
}
