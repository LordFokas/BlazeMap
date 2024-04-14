package com.eerussianguy.blazemap.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.common.ForgeConfigSpec;

public class MinimapConfigFacade {
    public interface IWidgetConfig {
        IntFacade positionX();
        IntFacade positionY();
        IntFacade width();
        IntFacade height();

        default void resize(int width, int height) {
            width().set(width);
            height().set(height);
        }
    }

    public static class IntFacade {
        private final Supplier<Integer> getter;
        private final Consumer<Integer> setter;

        public IntFacade(ForgeConfigSpec.IntValue value) {
            getter = value::get;
            setter = value::set;
        }

        public IntFacade(Supplier<Integer> getter, Consumer<Integer> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        public int get() {
            return getter.get();
        }

        public void set(int value) {
            setter.accept(value);
        }
    }
}
