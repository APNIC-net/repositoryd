package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.apnic.rpki.rsync.Module;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ListCommandTest extends ServerTestBase {

    Server handshakenServer(List<? extends Module> modules) throws Exception {
        Server p30 = new Server(modules);
        p30.read(bufferForString("@RSYNCD: 30.0\n"));

        ByteBuf output = Unpooled.buffer(128, 256);
        p30.write(output);
        return p30;
    }

    @Test
    public void listEmptyModules() throws Exception {
        Server p30 = handshakenServer(new ArrayList<Module>());
        p30.read(bufferForString("\n"));
        ByteBuf output = Unpooled.buffer(128, 256);
        p30.write(output);

        checkOutput("@RSYNCD: EXIT\n", output);
    }

    @Test
    public void listSomeModules() throws Exception {
        Server p30 = handshakenServer(Arrays.asList(
                new TestModule("mod1", "desc1"), new TestModule("mod2", "desc2")
        ));
        p30.read(bufferForString("\n"));
        ByteBuf output = Unpooled.buffer(128, 256);
        p30.write(output);

        checkOutput("mod1\tdesc1\nmod2\tdesc2\n@RSYNCD: EXIT\n", output);
    }

}