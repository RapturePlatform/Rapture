package rapture.mongodb;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.client.FindIterable;

import rapture.common.exception.ExceptionToString;

public abstract class MongoRetryWrapper<T> {
    private static Logger log = Logger.getLogger(MongoRetryWrapper.class);
    private int retryCount = MongoDBFactory.getRetryCount();
        
    public MongoRetryWrapper() {
    }
    
    public abstract T action(FindIterable<Document> cursor);
    
    public FindIterable<Document> makeCursor() {
        return null;
    }
    
    public T doAction() {
        T object = null;
        FindIterable<Document> cursor = null;
        while (retryCount-- > 0) {
            try {
            	cursor = makeCursor();
                object = action(cursor);
                retryCount = 0;
            } catch (com.mongodb.MongoException e) {
                log.info("Exception talking to Mongo: \n" + ExceptionToString.format(e));
                log.info("Remaining tries: " + retryCount);
            }
        }
        return object;
    }
}
