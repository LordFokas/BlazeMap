package com.eerussianguy.blazemap.lib.gui.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.gui.components.events.GuiEventListener;

import com.mojang.blaze3d.vertex.PoseStack;

public abstract class BaseContainer<T extends BaseContainer<T>> extends BaseComponent<T> implements UIEventListener {
    private final List<BaseComponent<?>> renderables = new ArrayList<>();
    private final List<GuiEventListener> listeners = new ArrayList<>();
    private GuiEventListener focus;

    protected void add(BaseComponent<?> child) {
        renderables.add(child.withParent(this));
        if(child instanceof GuiEventListener listener) {
            listeners.add(listener);
        }
    }

    public void remove(BaseComponent<?> child) {
        renderables.remove(child);
        if(child instanceof GuiEventListener) {
            listeners.remove(child);
        }
    }

    protected void clear() {
        if(size() == 0) return;

        // Clear focus if it is our direct child
        // FIXME: when focus is deeper child
        BaseComponent<?> parent = this;
        while(parent != null) {
            if(parent instanceof BaseContainer<?> container) {
                if(listeners.contains(container.focus)) {
                    container.focus = null;
                    break; // only root container can have focus
                }
            }
            parent = parent.getParent();
        }

        renderables.clear();
        listeners.clear();
    }

    public int size() {
        return renderables.size();
    }

    protected void renderBackground(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {}

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        getComponentAt(mouseX, mouseY, ReferenceFrame.PARENT).ifPresent(child -> {
            int childX = child.getPositionX();
            int childY = child.getPositionY();
            stack.pushPose();
            stack.translate(childX, childY, 0.1);
            child.renderTooltip(stack, mouseX - childX, mouseY - childY, service);
            stack.popPose();
        });
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderBackground(stack, hasMouse, mouseX, mouseY);
        for(var child : renderables) {
            stack.pushPose();
            int childX = child.getPositionX(), childY = child.getPositionY();
            int childMouseX = mouseX - childX, childMouseY = mouseY - childY;
            stack.translate(childX, childY, 0.1);
            child.renderInternal(stack, hasMouse && child.mouseIntercepts(childMouseX, childMouseY), childMouseX, childMouseY);
            stack.popPose();
        }
    }

    public Optional<GuiEventListener> getListenerAt(double x, double y, ReferenceFrame reference) {
        return getElementAt(listeners, x, y, reference);
    }

    public Optional<GuiEventListener> getListenerAt(double x, double y) {
        return getElementAt(listeners, x, y, getReferenceFrame());
    }

    public Optional<GuiEventListener> getLeafListenerAt(double x, double y) {
        var result = getListenerAt(x, y);
        if(result.isEmpty()) return result;
        if(result.get() instanceof BaseContainer<?> container) {
            return container.getLeafListenerAt(x - container.getPositionX(), y - container.getPositionY());
        } else {
            return result;
        }
    }

    public Optional<BaseComponent<?>> getComponentAt(double x, double y, ReferenceFrame reference) {
        return getElementAt(renderables, x, y, reference);
    }

    public Optional<BaseComponent<?>> getComponentAt(double x, double y) {
        return getElementAt(renderables, x, y, getReferenceFrame());
    }

    protected <E> Optional<E> getElementAt(List<E> list, double x, double y, ReferenceFrame reference) {
        if(reference == ReferenceFrame.GLOBAL) {
            x -= getPositionX();
            y -= getPositionY();
        }
        var components = (List<BaseComponent<?>>) list;
        for(var component : components) {
            if(component.mouseIntercepts(x - component.getPositionX(), y - component.getPositionY())) {
                return Optional.of((E) component);
            }
        }
        return Optional.empty();
    }

    protected boolean passthrough(double x, double y, Predicate<GuiEventListener> function, boolean fallback) {
        if(!isVisible()) return fallback;

        var listener = getListenerAt(x, y);
        if(listener.isEmpty()) return fallback;

        return function.test(listener.get());
    }

    protected double offsetX(double x, GuiEventListener child) {
        int selfX = getReferenceFrame() == ReferenceFrame.GLOBAL ? getPositionX() : 0;
        int childX = ((BaseComponent<?>)child).getPositionX();
        return x - (selfX + childX);
    }

    protected double offsetY(double y, GuiEventListener child) {
        int selfY = getReferenceFrame() == ReferenceFrame.GLOBAL ? getPositionY() : 0;
        int childY = ((BaseComponent<?>)child).getPositionY();
        return y - (selfY + childY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        getListenerAt(mouseX, mouseY).ifPresent(child -> child.mouseMoved(offsetX(mouseX, child), offsetY(mouseY, child)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(getReferenceFrame() == ReferenceFrame.GLOBAL) {
            var clicked = getLeafListenerAt(mouseX, mouseY).orElse(null);
            if(clicked != focus) {
                boolean changed = false;
                if(clicked != null) {
                    changed = clicked.changeFocus(true);
                }
                if(focus != null) {
                    focus.changeFocus(false);
                }
                focus = changed ? clicked : null;
            }
        }

        return passthrough(mouseX, mouseY, listener -> listener.mouseClicked(offsetX(mouseX, listener), offsetY(mouseY, listener), button), false);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return passthrough(mouseX, mouseY, listener -> listener.mouseReleased(offsetX(mouseX, listener), offsetY(mouseY, listener), button), false);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
        return passthrough(mouseX, mouseY, listener -> listener.mouseDragged(offsetX(mouseX, listener), offsetY(mouseY, listener), button, draggedX, draggedY), false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return passthrough(mouseX, mouseY, listener -> listener.mouseScrolled(offsetX(mouseX, listener), offsetY(mouseY, listener), scroll), false);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if(getReferenceFrame() == ReferenceFrame.GLOBAL && focus != null) {
            return focus.keyPressed(key, 0, 0);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int key, int scancode, int modifiers) {
        if(getReferenceFrame() == ReferenceFrame.GLOBAL && focus != null) {
            return focus.keyReleased(key, 0, 0);
        }
        return false;
    }

    @Override
    public boolean charTyped(char ch, int modifier) {
        if(getReferenceFrame() == ReferenceFrame.GLOBAL && focus != null) {
            return focus.charTyped(ch, modifier);
        }
        return false;
    }

    @Override
    public boolean changeFocus(boolean focused) {
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isVisible() && this.mouseIntercepts(mouseX, mouseY);
    }

    protected int sum(SizeSupplier supplier){
        int size = 0;
        for(var component : renderables) {
            size += supplier.get(component);
        }
        return size;
    }

    protected int max(SizeSupplier supplier){
        int size = 0;
        for(var component : renderables) {
            size = Math.max(supplier.get(component), size);
        }
        return size;
    }

    protected void forEach(Consumer<BaseComponent<?>> consumer) {
        for(var component : renderables) {
            consumer.accept(component);
        }
    }

    @FunctionalInterface
    protected interface SizeSupplier {
        int get(BaseComponent<?> component);
    }
}
