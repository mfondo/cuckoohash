package com.github.mfondo;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: mikefriesen
 * Date: 10/31/14
 * Time: 5:15 PM
 */
public class CuckooHashSetTest extends TestCase {

    public void test1() {
        final Set<Integer> cuckooSet = new CuckooHashSet<Integer>(Integer.class, 100, 0.9f, new CuckooHashSet.HashFunction<Integer>() {
            @Override
            public int hash(Integer integer) {
                return integer;
            }
        }, new CuckooHashSet.HashFunction<Integer>() {
            @Override
            public int hash(Integer integer) {
                //from http://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key
                integer = ((integer >> 16) ^ integer) * 0x45d9f3b;
                integer = ((integer >> 16) ^ integer) * 0x45d9f3b;
                integer = ((integer >> 16) ^ integer);
                return integer;
            }
        });

        //todo remove - for debugging
        /*dumpContents(cuckooSet);
        cuckooSet.add(1);
        dumpContents(cuckooSet);
        cuckooSet.add(2);
        dumpContents(cuckooSet);
        for(int i = 3; i < 10; i++) {
            cuckooSet.add(i);
        }
        dumpContents(cuckooSet);*/

        final Set<Integer> hashSet = new HashSet<Integer>();

        assertAdd(cuckooSet, hashSet, 1);
        assertTrue(cuckooSet.contains(1));
        assertFalse(cuckooSet.contains(2));

        assertAdd(cuckooSet, hashSet, 2);
        assertTrue(cuckooSet.contains(1));
        assertTrue(cuckooSet.contains(2));
        assertFalse(cuckooSet.contains(3));

        assertAdd(cuckooSet, hashSet, 1);
        assertRemove(cuckooSet, hashSet, 1);

        for(int i = 0; i < 1000; i++) {
            assertAdd(cuckooSet, hashSet, i);
        }
        for(int i = 0; i < 1000; i++) {
            assertRemove(cuckooSet, hashSet, i);
        }
        for(int i = 0; i < 1000; i++) {
            final int rand = (int) Math.random() * Integer.MAX_VALUE;
            if(Math.random() > 0.5) {
                assertAdd(cuckooSet, hashSet, rand);
            } else {
                assertRemove(cuckooSet, hashSet, rand);
            }
        }
    }

    //todo remove - this is for debugging
    private void dumpContents(Set<Integer> set) {
        for(Integer i : set) {
            System.out.print(i + ",");
        }
        System.out.println();
    }

    private void assertAdd(Set<Integer> set1, Set<Integer> set2, int i) {
        set1.add(i);
        set2.add(i);
        assertEquals(set1, set2);
    }

    private void assertRemove(Set<Integer> set1, Set<Integer> set2, int i) {
        set1.remove(i);
        set2.remove(i);
        assertEquals(set1, set2);
    }
}
