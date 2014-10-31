package com.github.mfondo;

import junit.framework.TestCase;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: mikefriesen
 * Date: 10/31/14
 * Time: 5:15 PM
 */
public class CuckooHashSetTest extends TestCase {

    public void test1() {
        Set<Integer> set = new CuckooHashSet<Integer>(100, new CuckooHashSet.HashFunction<Integer>() {
            @Override
            public int hash(Integer integer) {
                return integer % 7;
            }
        }, new CuckooHashSet.HashFunction<Integer>() {
            @Override
            public int hash(Integer integer) {
                return integer % 11;
            }
        });

        set.add(3);
        assertTrue(set.contains(3));
    }
}
