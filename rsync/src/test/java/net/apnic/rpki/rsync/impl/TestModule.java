package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.Module;

public class TestModule implements Module {
    private final String name;
    private final String description;

    TestModule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

}
