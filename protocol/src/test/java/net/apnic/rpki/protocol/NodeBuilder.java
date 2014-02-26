package net.apnic.rpki.protocol;

import net.apnic.rpki.data.Repository;

import java.util.ArrayList;
import java.util.List;

/**
* @author bje
*/
class NodeBuilder {
    private String name = "unnamed";
    private long lastModifiedTime = 1391755776;
    private boolean isDirectory = false;
    private byte[] content = null;
    private long size = 0;
    private List<Repository.Node> children = null;


    public NodeBuilder(boolean isDirectory) {
        this.isDirectory = isDirectory;
        if (isDirectory) children = new ArrayList<>();
    }

    public Repository.Node build() {
        return new Repository.Node() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getLastModifiedTime() {
                return lastModifiedTime;
            }

            @Override
            public boolean isDirectory() {
                return isDirectory;
            }

            @Override
            public byte[] getContent() {
                return content;
            }

            @Override
            public long getSize() {
                return size;
            }

            @Override
            public List<Repository.Node> getChildren() {
                return children;
            }
        };
    }

    public NodeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public NodeBuilder withLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
        return this;
    }

    NodeBuilder withContent(byte[] content) {
        this.content = content;
        this.size = content.length;
        return this;
    }

    public NodeBuilder withSize(long size) {
        this.size = size;
        return this;
    }

    public NodeBuilder withChild(Repository.Node child) {
        this.children.add(child);
        return this;
    }

    public static Repository.Node fileNode(String name, byte[] content) {
        return new NodeBuilder(false).withName(name).withContent(content).build();
    }
}
