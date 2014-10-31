package com.github.mfondo;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * http://en.wikipedia.org/wiki/Cuckoo_hashing
 * Implementation of http://www.cs.tau.ac.il/~shanir/advanced-seminar-data-structures-2007/bib/pagh01cuckoo.pdf
 */
public class CuckooHashSet<T> implements Set<T> {

    private final HashFunction<T> hashFunction1;
    private final HashFunction<T> hashFunction2;
    private final int maxInsertLoops;
    private final float loadFactor;

    private T[] values1;
    private T[] values2;
    private int size = 0;

    public CuckooHashSet(int maxInsertLoops, float loadFactor, HashFunction<T> hashFunction1, HashFunction<T> hashFunction2) {
        this.maxInsertLoops = maxInsertLoops;
        this.loadFactor = loadFactor;
        this.hashFunction1 = hashFunction1;
        this.hashFunction2 = hashFunction2;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean isEmpty() {
        return size() < 1;
    }

    @Override
    public boolean contains(Object o) {
        int hash = hashFunction1.hash((T)o) % values1.length;
        T t = values1[hash];
        if(t == null) {
            hash = hashFunction2.hash((T)o) % values2.length;
            t = values2[hash];
        }
        return t != null;
    }

    @Override
    public boolean add(T t) {
//        T ret = remove(t);
//        if(ret != null) {
//            todo replace ret with v
//        }
        if(values1 == null || size > (values1.length * loadFactor)) {
            //resize
        }
        T ret = null;
        int hash;
        for(int i = 0; i < maxInsertLoops; i++) {
            hash = hashFunction1.hash(t) % values1.length;
            ret = values1[hash];
            if(ret == null) {
                values1[hash] = t;
                break;
            }
            values1[hash] = t;
            t = ret;
            hash = hashFunction2.hash(t) % values2.length;
            ret = values2[hash];
            if(ret == null) {
                values2[hash] = t;
                break;
            }
            values2[hash] = t;
            t = ret;
        }
        size++;
        return true;//todo wrong
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public <T1 extends Object> T1[] toArray(T1[] a) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean remove(Object o) {
        size--;
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();//todo
    }

    public static interface HashFunction<K> {
        int hash(K k);
    }

    private boolean containsValue(T[] table, Object o) {
        boolean ret = false;
        for(T t : table) {
            if(t.equals(o)) {
                ret = true;
                break;
            }
        }
        return ret;
    }
}
