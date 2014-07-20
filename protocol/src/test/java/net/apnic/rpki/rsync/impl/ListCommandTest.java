package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.Module;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ListCommandTest {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    ByteBuffer bufferForString(String s) {
        return ByteBuffer.wrap(s.getBytes(UTF8));
    }

//    Protocol30 handshakenServer(List<Module> modules) throws Exception {
//        Protocol30 p30 = new Protocol30(modules);
//        p30.consume(bufferForString("@RSYNCD: 30.0\n"));
//        while (p30.write() != null) {p30.write().clear().flip();}
//        return p30;
//    }
//
//    @Test
//    public void listEmptyModules() throws Exception {
//        Protocol30 p30 = handshakenServer(null);
//        p30.consume(bufferForString("\n"));
//        ByteBuffer out = p30.write();
//        assertNotNull("out is not null", out);
//        assertThat("out has data pending", out.hasRemaining(), is(equalTo(true)));
//        byte[] expected = "@RSYNCD: EXIT\n".getBytes(UTF8);
//        byte[] given = new byte[expected.length];
//        out.get(given);
//        assertThat("out has a version pending", given, is(equalTo(expected)));
//    }

}