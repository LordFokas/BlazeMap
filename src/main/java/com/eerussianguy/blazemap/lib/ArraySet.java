package com.eerussianguy.blazemap.lib;

import java.util.*;

import org.jetbrains.annotations.NotNull;

/**
 * This is obvious, don't make me explain myself. But TL;DR:
 *
 * Java has all these complicated Set implementations and yet it doesn't offer a single simple one.
 * This is backed by an array, orderable, etc. Basically just an ArrayList that enforces uniqueness.
 * The intent here is to achieve high performance on tiny sets that don't get changed too much.
 * This is NOT meant for general use.
 *
 * @author LordFokas
 */
public class ArraySet<E> implements Set<E> {
    private final ArrayList<E> list;

    @SafeVarargs
    public static <E> ArraySet<E> of(E... elements) {
        Objects.requireNonNull(elements);
        ArraySet<E> set = new ArraySet<>(elements.length);
        for(E element : elements) {
            set.add(element);
        }
        return set;
    }

    public ArraySet() {
        list = new ArrayList<>();
    }

    public ArraySet(int initialCapacity) {
        list = new ArrayList<>(initialCapacity);
    }

    public ArraySet(Collection<E> initialValues) {
        this(initialValues.size());
        addAll(initialValues);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object element) {
        return list.contains(element);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] array) {
        return list.toArray(array);
    }

    @Override
    public boolean add(E element) {
        if(list.contains(element)) return false;
        return list.add(element);
    }

    /** ArrayList functionality */
    public boolean add(int index, E element) {
        if(list.contains(element)) return false;
        list.add(index, element);
        return true;
    }

    /** ArrayList functionality */
    public int indexOf(E element) {
        return list.indexOf(element);
    }

    /** ArrayList functionality */
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public boolean remove(Object element) {
        return list.remove(element);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return list.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return list.addAll(collection.stream().distinct().filter(e -> !contains(e)).toList());
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        return list.retainAll(collection);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        return list.removeAll(collection);
    }

    @Override
    public void clear() {
        list.clear();
    }
}