package com.eerussianguy.blazemap.lib.gui.components;

import java.util.regex.Pattern;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import com.eerussianguy.blazemap.lib.IntHolder;
import com.eerussianguy.blazemap.lib.ObjHolder;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.WrappedComponent;
import com.eerussianguy.blazemap.lib.gui.util.IntEnforcer;

public interface VanillaComponents {
    static BaseComponent<?> makeTextField(Font font, int w, int h, ObjHolder<String> value) {
        EditBox textBox = new EditBox(font, 0, 0, w - 2, h - 2, TextComponent.EMPTY);
        textBox.setValue(value.get());
        textBox.setResponder(value::set);
        return ((WrappedComponent.WrappedVanilla) WrappedComponent.of(textBox)).setPadding(1);
    }

    // FIXME: very broken for some unholy reason
    static BaseComponent<?> makeRGBHexField(Font font, int w, int h, ObjHolder<String> value) {
        EditBox textBox = new EditBox(font, 0, 0, w - 2, h - 2, TextComponent.EMPTY);
        textBox.setValue(value.get());
        textBox.setResponder(value::set);
        textBox.setMaxLength(6);
        textBox.setFilter(Pattern.compile("^[0-9a-fA-F]$").asPredicate());
        return ((WrappedComponent.WrappedVanilla) WrappedComponent.of(textBox)).setPadding(1);
    }

    static BaseComponent<?> makeIntField(Font font, int w, int h, IntHolder value) {
        EditBox textBox = new EditBox(font, 0, 0, w - 2, h - 2, TextComponent.EMPTY);
        textBox.setValue(String.valueOf(value.get()));
        IntEnforcer enforcer = new IntEnforcer(value::get, value::set);
        enforcer.setSubject(textBox);
        return ((WrappedComponent.WrappedVanilla) WrappedComponent.of(textBox)).setPadding(1);
    }
}
