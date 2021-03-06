package com.nao20010128nao.Wisecraft.misc;

import java.util.*;

public class RconModule_Factories {
    public static String[] strArray(int size) {
        if (size == 0) return RconModule_Constant.EMPTY_STRING_ARRAY;
        return new String[size];
    }

    public static String[] strArray(Collection<String> col) {
        return col.toArray(strArray(col.size()));
    }

    public static byte[] byteArray(int size) {
        return new byte[size];
    }

    public static <T> ArrayList<T> arrayList() {
        return new ArrayList<>();
    }

    public static <T> ArrayList<T> arrayList(List<T> list) {
        return new ArrayList<>(list);
    }

    public final static class FreeSizeNullLenList<T> implements List<T> {
        static Map<Integer, FreeSizeNullLenList> cache = new HashMap<>();
        final Object[] forTest;

        public static <T> FreeSizeNullLenList<T> obtainList(int len) {
            if (cache.containsKey(len))
                return (FreeSizeNullLenList<T>) cache.get(len);
            FreeSizeNullLenList<T> list = new FreeSizeNullLenList<>(len);
            cache.put(len, list);
            return list;
        }

        int length;

        private FreeSizeNullLenList(int length) {
            this.length = length;
            forTest = new Object[length];
        }

        @Override
        public List<T> subList(int p1, int p2) {
            int len = p2 - p1;
            return obtainList(len);
        }

        @Override
        public int indexOf(Object p1) {
            if (p1 != null)
                return -1;
            return 0;
        }

        @Override
        public void add(int p1, T p2) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public T set(int p1, T p2) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public boolean add(T p1) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public Iterator<T> iterator() {
            return new FSNLL_Iterator();
        }

        @Override
        public <T extends Object> T[] toArray(T[] p1) {
            if (p1.length <= length)
                Arrays.fill(p1, null);
            if (p1.length > length)
                for (int i = 0; i < length; i++)
                    p1[i] = null;

            return p1;
        }

        @Override
        public T remove(int p1) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public int size() {
            return length;
        }

        @Override
        public boolean remove(Object p1) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public boolean containsAll(Collection<?> p1) {
            for (Object o : p1)
                if (o != null)
                    return false;
            return true;
        }

        @Override
        public Object[] toArray() {
            return new Object[length];
        }

        @Override
        public boolean removeAll(Collection<?> p1) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public boolean addAll(int p1, Collection<? extends T> p2) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public boolean addAll(Collection<? extends T> p1) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public void clear() {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public boolean retainAll(Collection<?> p1) {
            throw new RuntimeException("This list is immutable.");
        }

        @Override
        public int lastIndexOf(Object p1) {
            if (p1 != null)
                return -1;
            return length - 1;
        }

        @Override
        public boolean isEmpty() {
            return length == 0;
        }

        @Override
        public ListIterator<T> listIterator() {
            return new FSNLL_Iterator();
        }

        @Override
        public ListIterator<T> listIterator(int p1) {
            return (ListIterator<T>) obtainList(length - p1).listIterator();
        }

        @Override
        public T get(int p1) {
            return (T) (forTest[p1]);
        }

        @Override
        public boolean contains(Object p1) {
            if (p1 != null)
                return false;
            return true;
        }


        class FSNLL_Iterator implements Iterator<T>, ListIterator<T> {
            int count = 0;

            @Override
            public void remove() {
                throw new RuntimeException("This list is immutable.");
            }

            @Override
            public T next() {
                return get(count++);
            }

            @Override
            public boolean hasNext() {
                return count < length;
            }

            @Override
            public void set(T p1) {
                throw new RuntimeException("This list is immutable.");
            }

            @Override
            public boolean hasPrevious() {
                if (count == 0)
                    return false;
                return true;
            }

            @Override
            public int nextIndex() {
                return Math.min(count + 1, length - 1);
            }

            @Override
            public void add(T p1) {
                throw new RuntimeException("This list is immutable.");
            }

            @Override
            public T previous() {
                return get(count--);
            }

            @Override
            public int previousIndex() {
                return Math.max(count - 1, 0);
            }
        }
    }
}
