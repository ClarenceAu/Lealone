/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.lealone.mvstore;

import com.codefollower.lealone.mvstore.type.DataType;
import com.codefollower.lealone.mvstore.type.ObjectDataType;

/**
 * A stored map. Read operations can happen concurrently with all other
 * operations, without risk of corruption.
 * <p>
 * Write operations first read the relevant area from disk to memory
 * concurrently, and only then modify the data. The in-memory part of write
 * operations is synchronized. For scalable concurrent in-memory write
 * operations, the map should be split into multiple smaller sub-maps that are
 * then synchronized independently.
 *
 * @param <K> the key class
 * @param <V> the value class
 */
public class MVMapConcurrent<K, V> extends MVMap<K, V> {

    public MVMapConcurrent(DataType keyType, DataType valueType) {
        super(keyType, valueType);
    }

    protected Page copyOnWrite(Page p, long writeVersion) {
        return p.copy(writeVersion);
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        beforeWrite();
        try {
            // even if the result is the same, we still update the value
            // (otherwise compact doesn't work)
            get(key);
            long writeVersion = store.getCurrentVersion();
            synchronized (this) {
                Page p = copyOnWrite(root, writeVersion);
                p = splitRootIfNeeded(p, writeVersion);
                V result = (V) put(p, writeVersion, key, value);
                newRoot(p);
                return result;
            }
        } finally {
            afterWrite();
        }
    }

    void waitUntilWritten(long version) {
        // no need to wait
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        beforeWrite();
        try {
            V result = get(key);
            if (result == null) {
                return null;
            }
            long writeVersion = store.getCurrentVersion();
            synchronized (this) {
                Page p = copyOnWrite(root, writeVersion);
                result = (V) remove(p, writeVersion, key);
                newRoot(p);
            }
            return result;
        } finally {
            afterWrite();
        }
    }

    /**
     * A builder for this class.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    public static class Builder<K, V> implements MapBuilder<MVMapConcurrent<K, V>, K, V> {

        protected DataType keyType;
        protected DataType valueType;

        /**
         * Create a new builder with the default key and value data types.
         */
        public Builder() {
            // ignore
        }

        /**
         * Set the key data type.
         *
         * @param keyType the key type
         * @return this
         */
        public Builder<K, V> keyType(DataType keyType) {
            this.keyType = keyType;
            return this;
        }

        /**
         * Set the key data type.
         *
         * @param valueType the key type
         * @return this
         */
        public Builder<K, V> valueType(DataType valueType) {
            this.valueType = valueType;
            return this;
        }

        @Override
        public MVMapConcurrent<K, V> create() {
            if (keyType == null) {
                keyType = new ObjectDataType();
            }
            if (valueType == null) {
                valueType = new ObjectDataType();
            }
            return new MVMapConcurrent<K, V>(keyType, valueType);
        }

    }

}
