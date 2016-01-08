/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.plugin.install;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This implements a topological sort in linear time
 * 
 * @author mel
 */
public class TopoSort<T> {
    private Set<T> entries = new HashSet<T>();
    private Set<Constraint<T>> constraints = new HashSet<Constraint<T>>();
    
    public List<T> sort() {
        return TopoSort.sort(entries, constraints);
    }
    
    public void addConstraint(T before, T after) {
        entries.add(before);
        entries.add(after);
        constraints.add(new Constraint<T>(before, after));
    }
    
    public void addEntry(T entry) {
        entries.add(entry);
    }
    
    public static <T> List<T> sort(Set<T> entries, Set<Constraint<T>> constraints) {
        Map<T,Scratch<T>> key2scratch = new HashMap<T,Scratch<T>>();
        List<T> result = new ArrayList<T>(entries.size());
        
        for(T entry: entries) {
            key2scratch.put(entry, new Scratch<T>());
        }
        
        for(Constraint<T> constraint: constraints) {
            key2scratch.get(constraint.getAfter()).increment();
            key2scratch.get(constraint.getBefore()).addFollower(constraint.getAfter());
        }
        
        List<T> thisWave = new LinkedList<T>();
        for(Entry<T, Scratch<T>> entry: key2scratch.entrySet()) {
            if (entry.getValue().ready()) {
                thisWave.add(entry.getKey());
            }
        }
        while(thisWave.size() > 0) {
            List<T> nextWave = new LinkedList<T>();
            for(T t: thisWave) {
                result.add(t);
                Scratch<T> scratch = key2scratch.get(t);
                for (T after: scratch.getFollowers()) {
                    if (key2scratch.get(after).decrement()) {
                        nextWave.add(after);
                    }
                }
            }
            thisWave = nextWave;
        }
        if(result.size() != entries.size()) {
            throw new IllegalStateException("Circular Dependency Detected");
        }
        return result;
    }
    
    /**
     * For testing only
     */
    Set<T> getEntries() {
        return Collections.unmodifiableSet(entries);
    }
    
    /**
     * For testing only
     */
    Set<Constraint<T>> getConstraints() {
        return Collections.unmodifiableSet(constraints);
    }
    
    /**
     * An immutable pair.
     * @author mel
     */
    static class Constraint<T> {
        final public T before;
        final public T after;
        Constraint(T before, T after) {
            this.before = before;
            this.after = after;
        }
        
        public final T getBefore() {
            return before;
        }
        
        public final T getAfter() {
            return after;
        }
    }
    
    /**
     * An ad hoc class to keep track of each element
     * 
     * @author mel
     */
    private static class Scratch<T> {
        int priorCount = 0;
        List<T> followers = new LinkedList<T>();
        public final void increment() {
            priorCount++;
        }
        
        public final boolean decrement() {
            priorCount--;
            return ready();
        }
        
        public final boolean ready() {
            return priorCount == 0;
        }
        
        public final void addFollower(T follower) {
            followers.add(follower);
        }
        
        public final Iterable<T> getFollowers() {
            return Collections.unmodifiableCollection(followers);
        }
    }
}
