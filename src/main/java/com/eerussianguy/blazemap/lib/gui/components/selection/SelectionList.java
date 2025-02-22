package com.eerussianguy.blazemap.lib.gui.components.selection;

import java.util.HashMap;
import java.util.Set;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.BaseScrollable;
import com.eerussianguy.blazemap.lib.gui.trait.FocusableComponent;
import com.mojang.blaze3d.vertex.PoseStack;

public class SelectionList<T, R> extends BaseScrollable<SelectionList<T, R>> {
    protected final SelectionModel<T, R> model;
    private boolean adaptive = false;

    public SelectionList(Materializer<T> materializer, SelectionModel<T, R> model) {
        super(new ListContainer<>(1, materializer, model));
        this.model = model;
    }

    public SelectionList<T, R> adaptive() {
        this.adaptive = true;
        return this;
    }

    public SelectionModel<T, R> getModel() {
        return model;
    }

    @Override
    public int getHeight() {
        return adaptive ? getAdaptiveHeight() : super.getHeight();
    }

    protected int getAdaptiveHeight() {
        int height = super.getHeight();
        int content = container.getHeight();
        if(height - content > 2) return content + 2;
        else return height;
    }

    @Override
    public int getIndependentHeight() {
        return super.getHeight();
    }

    protected static class ListContainer<T> extends ScrollableContainer<SelectionList<T, ?>> {
        protected final HashMap<T, MaterializedWrapper<T>> components = new HashMap<>();
        protected final Materializer<T> materializer;
        protected final SelectionModel<T, ?> model;

        public ListContainer(int spacing, Materializer<T> materializer, SelectionModel<T, ?> model) {
            super(spacing);
            this.materializer = materializer;
            this.model = model;
            model.addElementListener(this::elementsChanged);
        }

        protected void elementsChanged(Set<T> elements) {
            clear();
            for(T item : model.getElements()) {
                add(components.computeIfAbsent(item, this::materialize));
            }
            recalculate();
        }

        protected MaterializedWrapper<T> materialize(T item) {
            return new MaterializedWrapper<>(item, materializer.transform(item), model);
        }
    }

    protected static class MaterializedWrapper<T> extends BaseComponent<MaterializedWrapper<T>> implements FocusableComponent {
        protected final BaseComponent<?> component;
        protected final T item;
        protected final SelectionModel<T, ?> model;

        public MaterializedWrapper(T item, BaseComponent<?> component, SelectionModel<T, ?> model) {
            this.component = component.setPosition(0, 0);
            this.item = item;
            this.model = model;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            if(model.isSelected(item)) {
                renderFlatBackground(stack, Colors.DISABLED);
            }
            else if(hasMouse) {
                renderFlatBackground(stack, 0xFF404040);
            }
            component.render(stack, hasMouse, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            model.toggle(item);
            return true;
        }

        @Override
        public int getIndependentWidth() {
            return component.getWidth();
        }

        @Override
        public int getWidth() {
            return getParent().getWidth();
        }

        @Override
        public int getHeight() {
            return component.getHeight();
        }
    }
}
