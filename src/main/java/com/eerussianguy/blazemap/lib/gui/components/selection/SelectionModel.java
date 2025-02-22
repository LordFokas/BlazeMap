package com.eerussianguy.blazemap.lib.gui.components.selection;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import com.eerussianguy.blazemap.lib.ArraySet;

/**
 * Behavioral model for selection components.
 * @param <T> the type of values to be selected.
 * @param <R> the type of selection.
 *           Expected to be T for single models, and an equivalent of {@code Set<T>} for multi.
 */
public abstract class SelectionModel<T, R> {
    protected final ArraySet<T> elements = new ArraySet<>();
    protected final Set<T> elements_view = Collections.unmodifiableSet(elements);
    private final ArraySet<Consumer<Set<T>>> elementListeners = new ArraySet<>();
    private final ArraySet<Consumer<R>> selectionListeners = new ArraySet<>();

    protected SelectionModel() {}

    protected SelectionModel(Set<T> elements) {
        setElements(elements);
    }

    // SELECTION BEHAVIOR ==============================================================================================
    /** Get all possible selectable values */
    public Set<T> getElements() {
        return elements_view;
    }

    /** Toggle a value. Actual behavior varies between single and multi selection model implementations. */
    abstract void toggle(T item);

    public abstract boolean isSelected(T item);

    public abstract R getSelected();

    public abstract void setSelected(R selected);

    /** Whether the elements list is ordered (and re-orderable) or not. (see the move methods) */
    public boolean isOrdered() {
        return false;
    }

    /**
     * Move an item up or down in the order.
     * @param direction how many slots to move. Up if negative, down if positive.
     */
    public void move(T item, int direction) {}

    /** Whether an item can be moved further up (is not first item) */
    public boolean canMoveUp(T item) {
        return false;
    }

    /** Wether an item can be moved further down (is not last item) */
    public boolean canMoveDown(T item) {
        return false;
    }

    // ELEMENT MANIPULATION ============================================================================================
    public void setElements(Set<T> elements) {
        this.elements.clear();
        this.elements.addAll(elements);
        fireElementsChanged();
    }

    public void addElement(T element) {
        if(elements.add(element)){
           fireElementsChanged();
        }
    }

    public void removeElement(T element) {
        if(elements.remove(element)){
            fireElementsChanged();
        }
    }

    // LISTENERS =======================================================================================================
    protected void fireElementsChanged() {
        for(var listener : elementListeners) {
            listener.accept(elements_view);
        }
    }

    protected void fireSelectionChanged() {
        R selection = getSelected();
        for(var listener : selectionListeners) {
            listener.accept(selection);
        }
    }

    public void addElementListener(Consumer<Set<T>> listener) {
        listener.accept(getElements());
        elementListeners.add(listener);
    }

    public void addSelectionListener(Consumer<R> listener) {
        listener.accept(getSelected());
        selectionListeners.add(listener);
    }
}
