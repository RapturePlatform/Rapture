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
package rapture.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TreeCache {
    private Node root = new Node("", true);
    private final Callback callback;

    public TreeCache(Callback callback) {
        this.callback = callback;
    }

    public void registerKey(String key) {
        String[] elements = massagePath(key).split("/");
        Node nut = root;
        int index = 0;
        boolean isFolder = (elements.length > 1);
        while (index < elements.length) {
            if (elements[index].length() == 0) {
                index++;
                continue;
            }
            Node bolt = nut.find(elements[index]);
            if (bolt == null) {
                bolt = nut.add(elements[index], isFolder);
                callback.register(combine(elements, index), isFolder);
            } else if (!bolt.isLeaf() && !isFolder) {
                bolt.alsoLeaf();
                callback.register(combine(elements, index), false);
            } else if (!bolt.isFolder() && isFolder) {
                bolt.alsoFolder();
                callback.register(combine(elements, index), true);
            }
            index++;
            nut = bolt;
            isFolder = (index + 1 < elements.length);
        }
    }

    // this is not safe in the general case -- it only works when elements is non-empty and offset < path length
    private String combine(String[] elements, int offset) {
        StringBuilder sb = new StringBuilder();
        sb.append(elements[0]);
        for (int i = 1; i < offset + 1; i++) {
            sb.append("/");
            sb.append(elements[i]);
        }
        return sb.toString();
    }
    
    public void invalidateKey(String key) {
        unregisterKey(key, false, true);
        unregisterKey(key, true, true);
    }
    
    public void unregisterFolder(String key) {
        unregisterKey(key, true, false);
    }

    public void unregisterKey(String key) {
        unregisterKey(key, false, false);
    }
    
    public void unregisterKey(String key, boolean isFolder, boolean cacheOnly) {
        if (!cacheOnly) callback.unregister(key, false);
        List<Node> path = Lists.newArrayList();
        String[] elements = massagePath(key).split("/");
        Node nut = root;
        int index = 0;
        boolean notFirst = isFolder;
        while (index < elements.length) {
            if (elements[index].length() == 0) {
                index++;
                continue;
            }
            Node bolt = nut.find(elements[index]);
            if (bolt == null) {
                notFirst = true;
                break;
            }
            path.add(bolt);
            nut = bolt;
            index++;
        }
        for (int i = elements.length - 1; i >= 0; i--) {
            int j = i - 1;
            Node child = safeGet(path, i);
            Node parent = safeGet(path, j);
            if (child != null) {
                if (child.isFolder() && child.isLeaf()) {
                    if (notFirst) {
                        child.onlyFolder();
                    } else {
                        child.onlyLeaf();
                    }
                    return;
                }
                if (parent != null) {
                    if (parent.remove(elements[i], child)) return;
                }
            }
            notFirst = true;
        }
    }

    private Node safeGet(List<Node> list, int index) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public interface Callback {
        public void register(String key, boolean isFolder);

        public void unregister(String key, boolean isFolder);
    }

    private static class Node {
        private boolean isFolder;
        private boolean isLeaf;
        private Map<String, Node> children = null;

        Node(String element, boolean isFolder) {
            this.isFolder = isFolder;
            this.isLeaf = !isFolder;
        }

        synchronized public boolean remove(String key, Node child) {
            if (child == null) return size() > 0;
            children.remove(key);
            return size() > 0;
        }

        public int size() {
            return (children == null) ? 0 : children.size();
        }

        public void onlyLeaf() {
            isFolder = false;
        }

        public void onlyFolder() {
            isLeaf = false;
        }

        public void alsoFolder() {
            isFolder = true;
        }

        public void alsoLeaf() {
            isLeaf = true;
        }

        Node find(String key) {
            if (children == null) return null;
            return children.get(key);
        }

        // calibration tuning knob
        private static int HASH_THRESHOLD = 6;

        synchronized Node add(String key, boolean isFolder) {
            Node result = new Node(key, isFolder);
            if (children == null) children = new ListMap<String, Node>(key, result);
            else if (children.size() == HASH_THRESHOLD) {
                Map<String, Node> old_kids = children;
                children = Maps.newHashMap();
                children.putAll(old_kids);
                children.put(key, result);
            } else {
                children.put(key, result);
            }
            return result;
        }

        public boolean isFolder() {
            return isFolder;
        }

        public boolean isLeaf() {
            return isLeaf;
        }
    }

    // map implementation for small data sizes to avoid initializing hash maps until needed
    private static class ListMap<K, V> implements Map<K, V> {
        private List<Entry<K, V>> data = Lists.newArrayList();

        ListMap(K k, V v) {
            data.add(new Entry<K, V>(k, v));
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean containsKey(Object key) {
            for (Entry<K, V> e : data) {
                if (e.getKey().equals(key)) return true;
            }
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            for (Entry<K, V> e : data) {
                if (value.equals(e.getValue())) return true;
            }
            return false;
        }

        @Override
        public V get(Object key) {
            for (Entry<K, V> e : data) {
                if (e.getKey().equals(key)) return e.getValue();
            }
            return null;
        }

        @Override
        public V put(K key, V value) {
            V result = get(key);
            data.add(new Entry<K, V>(key, value));
            return result;
        }

        @Override
        public V remove(Object key) {
            Iterator<Entry<K, V>> iter = data.iterator();
            while (iter.hasNext()) {
                Entry<K, V> e = iter.next();
                if (key.equals(e.getKey())) {
                    iter.remove();
                    return e.getValue();
                }
            }
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            Set<K> result = Sets.newHashSet();
            for (Entry<K, V> e : data) {
                result.add(e.getKey());
            }
            return result;
        }

        @Override
        public Collection<V> values() {
            Set<V> result = Sets.newHashSet();
            for (Entry<K, V> e : data) {
                result.add(e.getValue());
            }
            return result;
        }

        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet() {
            Set<java.util.Map.Entry<K, V>> s = Sets.newHashSet();
            s.addAll(data);
            return s;
        }

        static class Entry<K, V> implements java.util.Map.Entry<K, V> {
            final K k;
            final V v;

            Entry(K k, V v) {
                this.k = k;
                this.v = v;
            }

            public K getKey() {
                return k;
            }

            public V getValue() {
                return v;
            }

            @Override
            public V setValue(V value) {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static String massagePath(String path) {
        return path.replaceAll("/+", "/").replaceAll("^/+", "").replaceAll("/$", "");
    }
}
