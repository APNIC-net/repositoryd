package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.RsyncException;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ListCommandTest {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    @Test
    public void listEmptyModules() throws Exception {
    }

}