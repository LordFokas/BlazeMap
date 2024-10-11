package com.eerussianguy.blazemap.gui.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.components.EditBox;

public class IntEnforcer {
    private final Supplier<Integer> source;
    private final Consumer<Integer> target;

    public IntEnforcer(Supplier<Integer> source, Consumer<Integer> target) {
        this.source = source;
        this.target = target;
    }

    public void setSubject(EditBox subject) {
        subject.setResponder(s -> {
            if(s == null || s.equals("") || s.equals("-")) return;
            try {
                int v = Integer.parseInt(s);
                target.accept(v);
            }
            catch(NumberFormatException ex) {
                subject.setValue(String.valueOf(source.get()));
            }
        });
    }
}
