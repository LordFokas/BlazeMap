package com.eerussianguy.blazemap.lib.gui.components.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.Positionable;
import com.eerussianguy.blazemap.lib.gui.trait.FocusableComponent;
import com.eerussianguy.blazemap.lib.gui.trait.KeyboardControls;
import com.mojang.blaze3d.vertex.PoseStack;

public class SelectionGrid<T> extends BaseComponent<SelectionGrid<T>> implements FocusableComponent, KeyboardControls {
    protected final ArrayList<T> elements = new ArrayList<>();
    protected final Function<T, ResourceLocation> image;
    protected final int size, spacing, grain, offset;
    protected Consumer<T> listener = $ -> {};

    private final Positionable<?> target = new Positionable<>();
    private int rows, rowLength, x, y;
    private T[][] content;
    private T selected;

    public SelectionGrid(Function<T, ResourceLocation> image, int size, int spacing, Collection<T> elements) {
        this.image = image;
        this.size = size;
        this.spacing = spacing;
        this.grain = size + spacing;
        this.offset = spacing + 1;
        this.elements.addAll(elements);
        this.target.setSize(size, size);

        if(elements.size() == 0) {
            setEnabled(false);
            return;
        }

        int count = (int) Math.ceil(Math.sqrt(this.elements.size()));
        int optimal = (count + 1) * spacing + count * size + 2;
        setSize(optimal, optimal);
        x = y = 0;
        selected = content[y][x];
    }

    public T getValue() {
        return selected;
    }

    public SelectionGrid<T> setInitialValue(T value) {
        Objects.requireNonNull(value);
        if(!elements.contains(value)) {
            elements.add(0, value);
            rearrange();
        }
        return setValue(value);
    }

    public SelectionGrid<T> setValue(T value) {
        if(!elements.contains(value)) throw new IllegalStateException("Value not on elements list");

        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < rowLength; col++) {
                if(content[row][col].equals(value)) {
                    valueChanged(value, col, row);
                    return this;
                }
            }
        }

        throw new RuntimeException("This state is not possible");
    }

    public SelectionGrid<T> setListener(Consumer<T> listener) {
        this.listener = listener;
        listener.accept(selected);
        return this;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderFocusableBackground(stack);
        if(elements.size() == 0) return;

        stack.translate(offset, offset, 0);
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < rowLength; col++) {
                T element = content[row][col];
                if(element == null) break;
                int px = col * grain, py = row * grain;
                if(element.equals(selected)) {
                    stack.pushPose();
                    stack.translate(px-1, py-1, 0);
                    RenderHelper.fillRect(stack.last().pose(), size+2, size+2, 0xFFFFDD00);
                    stack.translate(1, 1, 0);
                    RenderHelper.fillRect(stack.last().pose(), size, size, Colors.BLACK);
                    stack.popPose();
                }
                RenderHelper.drawTexturedQuad(image.apply(element), Colors.NO_TINT, stack, px, py, size, size);
            }
        }
    }

    @Override
    public SelectionGrid<T> setSize(int w, int h) {
        super.setSize(w, h);
        rearrange();
        return this;
    }

    protected void rearrange() {
        if(elements.size() == 0) return;

        int width = getWidth();
        rowLength = (width - 2 - spacing) / grain;
        rows = (int) Math.ceil((double) elements.size() / rowLength);
        content = (T[][]) new Object[rows][rowLength];
        super.setSize(width, rows * size + (rows+1) * spacing + 2);

        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < rowLength; col++) {
                int index = row * rowLength + col;

                if(index >= elements.size()) return;
                Object element = content[row][col] = elements.get(index);

                if(element.equals(selected)) {
                    x = col;
                    y = row;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(mouseX < offset || mouseY < offset) return true; // too short to hit anything

        // calculate content matrix coordinates
        int col = (int) ((mouseX - offset) / grain);
        int row = (int) ((mouseY - offset) / grain);
        if(row >= content.length || col >= rowLength) return true; // too far to hit anything

        // calculate origin coordinates of closet target
        int px = col * grain + offset;
        int py = row * grain + offset;

        // check for hit
        target.setPosition(px, py);
        if(!target.mouseIntercepts(mouseX, mouseY)) return true; // no hit, skip

        // update selection
        T hit = content[row][col];
        if(hit == null) return true; // end of last row might be void
        valueChanged(hit, col, row);

        return true;
    }

    protected void valueChanged(T value, int col, int row) {
        x = col;
        y = row;
        valueChanged(value);
    }

    protected void valueChanged(T value) {
        if(this.selected.equals(value)) return;

        this.selected = value;
        this.listener.accept(selected);
    }

    protected boolean nextValue(int dx, int dy) {
        if(dx != 0) { // move horizontally
            x += dx;
            if(x < 0 || x >= rowLength) {
                x = (x+rowLength) % rowLength;
                y += dx;
                if(y < 0 || y >= rows) {
                    y = (y+rows) % rows;
                }
            }
        } else if(dy != 0){ // move vertically
            y += dy;
            if(y < 0 || y >= rows) {
                y = (y+rows) % rows;
                x += dy;
                if(x < 0 || x >= rowLength) {
                    x = (x+rowLength) % rowLength;
                }
            }
        } else return true; // no move? y u silly?

        // Deal with gaps in last row
        while(content[y][x] == null) {
            x--;
            if(x < 0) throw new IllegalStateException("Poorly structured content matrix");
        }

        // update and notify
        valueChanged(content[y][x]);

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return nextValue(scroll > 0 ? -1 : 1, 0);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if(isKeyUp(key))     return nextValue( 0, -1);
        if(isKeyDown(key))   return nextValue( 0, +1);
        if(isKeyLeft(key))   return nextValue(-1,  0);
        if(isKeyRight(key))  return nextValue(+1,  0);

        return false;
    }
}
