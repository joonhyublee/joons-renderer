package org.sunflow.util;

import java.util.Iterator;

/**
 * Fast hash map implementation which uses array storage along with quadratic
 * probing to resolve collisions. The capacity is doubled when the load goes
 * beyond 50% and is halved when the load drops below 20%.
 *
 * @param <K>
 * @param <V>
 */
public class FastHashMap<K, V> implements Iterable<FastHashMap.Entry<K, V>> {

    private static final int MIN_SIZE = 4;

    public static class Entry<K, V> {

        private final K k;
        private V v;

        private Entry(K k, V v) {
            this.k = k;
            this.v = v;
        }

        private boolean isRemoved() {
            return v == null;
        }

        private void remove() {
            v = null;
        }

        public K getKey() {
            return k;
        }

        public V getValue() {
            return v;
        }
    }
    private Entry<K, V>[] entries;
    private int size;

    public FastHashMap() {
        clear();
    }

    public final void clear() {
        size = 0;
        entries = alloc(MIN_SIZE);
    }

    public V put(K k, V v) {
        int hash = k.hashCode(), t = 0;
        int pos = entries.length; // mark invalid position
        for (;;) {
            hash &= entries.length - 1;
            if (entries[hash] == null) {
                break; // done probing
            } else if (entries[hash].isRemoved() && pos == entries.length) {
                pos = hash; // store, but keep searching
            } else if (entries[hash].k.equals(k)) {
                // update entry
                V old = entries[hash].v;
                entries[hash].v = v;
                return old;
            }
            t++;
            hash += t;
        }
        // did we find a spot for insertion among the deleted values ?
        if (pos < entries.length) {
            hash = pos;
        }
        entries[hash] = new Entry<K, V>(k, v);
        size++;
        if (size * 2 > entries.length) {
            resize(entries.length * 2);
        }
        return null;
    }

    public V get(K k) {
        int hash = k.hashCode(), t = 0;
        for (;;) {
            hash &= entries.length - 1;
            if (entries[hash] == null) {
                return null;
            } else if (!entries[hash].isRemoved() && entries[hash].k.equals(k)) {
                return entries[hash].v;
            }
            t++;
            hash += t;
        }
    }

    public boolean containsKey(K k) {
        int hash = k.hashCode(), t = 0;
        for (;;) {
            hash &= entries.length - 1;
            if (entries[hash] == null) {
                return false;
            } else if (!entries[hash].isRemoved() && entries[hash].k.equals(k)) {
                return true;
            }
            t++;
            hash += t;
        }
    }

    public void remove(K k) {
        int hash = k.hashCode(), t = 0;
        for (;;) {
            hash &= entries.length - 1;
            if (entries[hash] == null) {
                return; // not found, return
            } else if (!entries[hash].isRemoved() && entries[hash].k.equals(k)) {
                entries[hash].remove(); // flag as removed
                size--;
                break;
            }
            t++;
            hash += t;
        }
        // do we need to shrink?
        if (entries.length > MIN_SIZE && size * 10 < 2 * entries.length) {
            resize(entries.length / 2);
        }
    }

    /**
     * Resize internal storage to the specified capacity. The capacity must be a
     * power of two.
     *
     * @param capacity new capacity for the internal array
     */
    private void resize(int capacity) {
        assert (capacity & (capacity - 1)) == 0;
        assert capacity >= MIN_SIZE;
        Entry<K, V>[] newentries = alloc(capacity);
        for (Entry<K, V> e : entries) {
            if (e == null || e.isRemoved()) {
                continue;
            }
            int hash = e.k.hashCode(), t = 0;
            for (;;) {
                hash &= newentries.length - 1;
                if (newentries[hash] == null) {
                    break;
                }
                assert !newentries[hash].k.equals(e.k);
                t++;
                hash += t;
            }
            newentries[hash] = new Entry<K, V>(e.k, e.v);
        }
        // copy new entries over old ones
        entries = newentries;
    }

    /**
     * Wrap the entry array allocation because it requires silencing some
     * generics warnings.
     *
     * @param size number of elements to allocate
     * @return
     */
    @SuppressWarnings("unchecked")
    private Entry<K, V>[] alloc(int size) {
        return new Entry[size];
    }

    private class EntryIterator implements Iterator<Entry<K, V>> {

        private int index;

        private EntryIterator() {
            index = 0;
            if (!readable()) {
                inc();
            }
        }

        private boolean readable() {
            return !(entries[index] == null || entries[index].isRemoved());
        }

        private void inc() {
            do {
                index++;
            } while (hasNext() && !readable());
        }

        @Override
        public boolean hasNext() {
            return index < entries.length;
        }

        @Override
        public Entry<K, V> next() {
            try {
                return entries[index];
            } finally {
                inc();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return new EntryIterator();
    }
}