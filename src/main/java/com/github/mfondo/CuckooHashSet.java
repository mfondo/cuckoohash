package com.github.mfondo;

import com.google.common.collect.AbstractIterator;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Iterator;

/**
 * http://en.wikipedia.org/wiki/Cuckoo_hashing
 * Implementation of http://www.cs.tau.ac.il/~shanir/advanced-seminar-data-structures-2007/bib/pagh01cuckoo.pdf
 *
 * TODO shrink the values to default initial size when enough elements removed
 * TODO use one T[] instead of two
 */
public class CuckooHashSet<T> extends AbstractSet<T> {

    private static final int DEFAULT_INITIAL_SIZE = 8;

    private static final int UPPER_BITS_MASK = 0xFFFF0000;
    private static final int UPPER_BITS_SHIFT = 16;
    private static final int LOWER_BITS_MASK = 0xFFFF;

    private final Class<T> valueClazz;
    private final HashFunction<T> hashFunction1;
    private final HashFunction<T> hashFunction2;
    private final int maxInsertLoops;
    private final float loadFactor;

    private T[] values1;
    private T[] values2;
    private int size = 0;

    /**
     * Uses {@link Object#hashCode()} as the hash function
     * @param valueClazz type of elements stored in this set
     * @param maxInsertLoops maximum number of loops when inserting an element before resizing
     * @param loadFactor how close to being full before the table is resized
     */
    public CuckooHashSet(Class<T> valueClazz, int maxInsertLoops, float loadFactor) {
        this(valueClazz, maxInsertLoops, loadFactor, new HashFunction<T>() {
            @Override
            public int hash(T t) {
                return t.hashCode();
            }
        });
    }

    /**
     * Like the constructor below, except splits the upper/lower bits of hashFunction1's output to be used for the two hashes
     * @param valueClazz type of elements stored in this set
     * @param maxInsertLoops maximum number of loops when inserting an element before resizing
     * @param loadFactor how close to being full before the table is resized
     * @param hashFunction1 hash function whose result will be split into upper/lower halves to serves as two hash outputs
     */
    public CuckooHashSet(Class<T> valueClazz, int maxInsertLoops, float loadFactor, HashFunction<T> hashFunction1) {
        this(valueClazz, maxInsertLoops, loadFactor, hashFunction1, null);
    }

    /**
     * @param valueClazz type of elements stored in this set
     * @param maxInsertLoops maximum number of loops when inserting an element before resizing
     * @param loadFactor how close to being full before the table is resized
     * @param hashFunction1 first hash function
     * @param hashFunction2 second hash function - must be independent of first hash function
     */
    public CuckooHashSet(Class<T> valueClazz, int maxInsertLoops, float loadFactor, HashFunction<T> hashFunction1, HashFunction<T> hashFunction2) {
        this.valueClazz = valueClazz;
        this.maxInsertLoops = maxInsertLoops;
        this.loadFactor = loadFactor;
        this.hashFunction1 = hashFunction1;
        this.hashFunction2 = hashFunction2;
        values1 = (T[])Array.newInstance(valueClazz, DEFAULT_INITIAL_SIZE);
        values2 = (T[])Array.newInstance(valueClazz, DEFAULT_INITIAL_SIZE);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() < 1;
    }

    @Override
    public boolean contains(Object o) {
        return getHolderAndPosition((T)o) != null;
    }

    private ValuesAndPosition<T> getHolderAndPosition(T val) {
        final int hashFunc1Pos = hashFunction1.hash(val);
        int pos;
        if(hashFunction2 == null) {
            pos = (hashFunc1Pos & UPPER_BITS_MASK) >> UPPER_BITS_SHIFT;
        } else {
            pos = hashFunc1Pos;
        }
        pos %= values1.length;
        T t = values1[pos];
        T[] values = values1;
        if(t == null || !t.equals(val)) {
            if(hashFunction2 == null) {
                pos = hashFunc1Pos & LOWER_BITS_MASK;
            } else {
                pos = hashFunction2.hash(val);
            }
            pos %= values2.length;
            t = values2[pos];
            values = values2;
        }
        ValuesAndPosition<T> ret;
        if(t != null) {
            ret = t.equals(val) ? new ValuesAndPosition<T>(values, pos) : null;
        } else {
            ret = null;
        }
        return ret;
    }

    @Override
    public boolean add(T t) {
        if(contains(t)) {
            return false;
        }
        if(((float)size) > (values1.length * loadFactor)) {
            resize();
        }
        add(values1, values2, t);
        size++;
        return true;
    }

    /**
     * @return true if resize actually occurred
     */
    private boolean resize() {
        boolean didResize;
        int currentSize = values1.length;
        int newSize = size < 1 ? 16 : currentSize * 2;
        if(newSize == currentSize) {
            didResize = false;
        } else {
            T[] tmp1 = (T[]) Array.newInstance(valueClazz, newSize);
            T[] tmp2 = (T[])Array.newInstance(valueClazz, newSize);
            addValues(values1, tmp1, tmp2);
            addValues(values2, tmp1, tmp2);
            values1 = tmp1;
            values2 = tmp2;
            didResize = true;
        }
        return didResize;
    }

    private void addValues(T[] from, T[] tmp1, T[] tmp2) {
        if(from != null) {
            for(T t : from) {
                if(t != null) {
                    add(tmp1, tmp2, t);
                }
            }
        }
    }

    private T add(T[] values1, T[] values2, T t) {
        T ret = null;
        int pos;
        int hash;
        int loops;
        outer:
        while(true) {
            for(loops = 0; loops < maxInsertLoops; loops++) {
                hash = hashFunction1.hash(t);
                if(hashFunction2 == null) {
                    pos = (hash & UPPER_BITS_MASK) >> UPPER_BITS_SHIFT;
                } else {
                    pos = hash;
                }
                pos %= values1.length;
                ret = values1[pos];
                if(ret == null) {
                    values1[pos] = t;
                    break;
                }
                values1[pos] = t;
                t = ret;
                if(hashFunction2 == null) {
                    pos = hashFunction1.hash(t) & LOWER_BITS_MASK;
                } else {
                    pos = hashFunction2.hash(t);
                }
                pos %= values2.length;
                ret = values2[pos];
                if(ret == null) {
                    values2[pos] = t;
                    break;
                }
                values2[pos] = t;
                t = ret;
            }
            if(loops >= maxInsertLoops) {
                if(!resize()) {
                    throw new IllegalStateException("maxInsertLoops exceeded and resize did not occur");
                }
            } else {
                break outer;
            }
        }
        return ret;
    }

    @Override
    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {

            private boolean inValues1 = true;
            private int currentPos = 0;

            @Override
            protected T computeNext() {
                T ret = null;
                T t;
                if(inValues1) {
                    for(; currentPos < values1.length; currentPos++) {
                        t = values1[currentPos];
                        if(t != null) {
                            ret = t;
                            break;
                        }
                    }
                    if(currentPos > values1.length - 1) {
                        inValues1 = false;
                        currentPos = 0;
                    }
                }
                if(!inValues1) {
                    for(; currentPos < values2.length; currentPos++) {
                        t = values2[currentPos];
                        if(t != null) {
                            ret = t;
                            break;
                        }
                    }
                }
                currentPos++;
                if(ret == null) {
                    ret = endOfData();
                }
                return ret;
            }
        };
    }

    @Override
    public boolean remove(Object o) {
        if(size < 1) {
            return false;
        }
        ValuesAndPosition<T> hop = getHolderAndPosition((T)o);
        boolean ret;
        if(hop != null) {
            hop.values[hop.pos] = null;
            ret = true;
        } else {
            ret = false;
        }
        size--;
        return ret;
    }

    public static interface HashFunction<K> {
        int hash(K k);
    }

    private static class ValuesAndPosition<T> {
        private final T[] values;
        private final int pos;

        private ValuesAndPosition(T[] values, int pos) {
            this.values = values;
            this.pos = pos;
        }
    }
}
