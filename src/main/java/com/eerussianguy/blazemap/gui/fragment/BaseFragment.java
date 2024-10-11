package com.eerussianguy.blazemap.gui.fragment;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.eerussianguy.blazemap.gui.lib.AbsoluteContainer;

public abstract class BaseFragment {
    protected final boolean standalone, hosted;
    protected final Component title;
    protected final Minecraft mc = Minecraft.getInstance();
    protected final Font font = mc.font;

    protected BaseFragment(Component title) {
        this(title, true, true);
    }

    protected BaseFragment(Component title, boolean standalone, boolean hosted) {
        this.title = title;
        this.standalone = standalone;
        this.hosted = hosted;
    }

    public Component getTitle() {
        return title;
    }

    public abstract void compose(FragmentContainer container);

    public void compose(FragmentContainer container, @Nullable AbsoluteContainer absolute) {
        compose(container);
    }

    public boolean open() {
        Screen screen = mc.screen;
        if(hosted && screen instanceof FragmentHost host) {
            return openHosted(host);
        }
        if(standalone && screen == null) {
            return openStandalone();
        }
        return false;
    }

    /** Have an existing Screen host the fragment */
    protected boolean openHosted(FragmentHost host) {
        return host.consumeFragment(this);
    }

    /** Create an empty Screen to host the fragment */
    protected boolean openStandalone() {
        mc.setScreen(new HostScreen(this));
        return true;
    }
}
