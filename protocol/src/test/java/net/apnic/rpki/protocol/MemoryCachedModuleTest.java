package net.apnic.rpki.protocol;

import net.apnic.rpki.data.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MemoryCachedModuleTest {
    private MemoryCachedModule module;

    private static byte[] bytes(int... values) {
        final byte[] result = new byte[values.length];
        int ndx = 0;
        for (int i : values) {
            result[ndx++] = (byte)i;
        }

        return result;
    }

    @Before
    public void mockRepository() throws Exception {
        final byte[] oneByte = new byte[] { 1 };
        final byte[] hello = bytes(0x62, 0x61, 0x64, 0x75, 0x6d, 0x70, 0x73, 0x68, 0x0a);

        final InputStream certStream = getClass().getResourceAsStream("/-Dcw_Tkbb492_vMXbtufxvVUHkA.cer");
        final byte[] cert = new byte[certStream.available()];
        int used = certStream.read(cert);
        assert used == cert.length;

        final Repository repository = mock(Repository.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((Repository.Watcher)(invocationOnMock.getArguments()[0])).repositoryUpdated(repository);
                return null;
            }
        }).when(repository).setWatcher(Matchers.any(Repository.Watcher.class));
        when(repository.getRepositoryRoot()).thenReturn(
                new NodeBuilder(true)
                        .withName("repository")
                        .withSize(170)
                        .withChild(NodeBuilder.fileNode("repository/apnic-rpki-root-iana-origin.cer", hello))
                        .withChild(new NodeBuilder(true)
                                .withName("repository/838DB214166511E2B3BC286172FD1FF2")
                                .withChild(NodeBuilder.fileNode("repository/838DB214166511E2B3BC286172FD1FF2/C5zKkN0Neoo3ZmsZIX_g2EA3t6I.crl", oneByte))
                                .withChild(NodeBuilder.fileNode("repository/838DB214166511E2B3BC286172FD1FF2/C5zKkN0Neoo3ZmsZIX_g2EA3t6I.mft", oneByte))
                                .withChild(NodeBuilder.fileNode("repository/838DB214166511E2B3BC286172FD1FF2/-Dcw_Tkbb492_vMXbtufxvVUHkA.cer", cert))
                                .build())
                        .withChild(new NodeBuilder(true)
                                .withName("repository/expandable")
                                .withChild(NodeBuilder.fileNode("repository/expandable/file-1", oneByte))
                                .withChild(new NodeBuilder(true)
                                        .withName("repository/expandable/subdir")
                                        .build())
                                .build())
                        .build()
        );


        module = new MemoryCachedModule(
                "repository", "repository", repository
        );
    }

    @Test
    public void findsSubPath() throws Exception {
        FileList fileList = module.getFileList("repository/expandable", false);
        assertNotNull("Found the sub-path", fileList);
        assertThat("The sub-path has no children", fileList.getSize(), is(equalTo(1)));
    }

    @Test
    public void findsSubDirectory() throws Exception {
        FileList fileList = module.getFileList("repository/expandable/", false);
        assertNotNull("Found the sub-path", fileList);
        assertThat("The sub-path has children", fileList.getSize(), is(greaterThan(1)));
    }

    @Test
    public void encodesBasicStructure() throws Exception {
        FileList fileList = module.getFileList("repository", true);

        assertNotNull("There should be a file list generated", fileList);

        byte[] listBytes = fileList.getFileListData();
        assertThat("There are some list bytes contained", listBytes.length, is(greaterThan(0)));
        assertThat("The first few bytes are of a known structure",
                Arrays.copyOfRange(listBytes, 0, 14),
                is(equalTo(new byte[]{
                        0x01 | 0x08 | 0x10,                     // flags TOP_DIR|SAME_UID|SAME_GID
                        0x01,                                   // name length
                        0x2e,                                   // name
                        0x00, (byte) 0xaa, 0x00,                 // varint(3) encoded size
                        0x52, 0x00, (byte) 0x82, (byte) 0xf4,     // varlong(4) encoded mtime
                        (byte) 0xfd, 0x41, 0x00, 0x00,           // mode 040775
                })));
        assertNull("The contents of the top directory should be null", fileList.getFile(0).getContents());

        assertThat("There is at least one file", fileList.getSize(), is(greaterThan(1)));
        byte[] compressed = fileList.getFile(1).getCompressedContents();
        assertNotNull("The first file has compressed data", compressed);
        assertThat("The first file's compressed data is correct", compressed,
                is(equalTo(bytes(0x4a, 0x4a, 0x4c, 0x29, 0xcd, 0x2d, 0x28, 0xce, 0xe0, 0x02, 0x00))));
    }

    @Test
    public void compressedCorrectly() throws Exception {
        FileList fileList = module.getFileList("repository", true);

        assertNotNull("There should be a file list generated", fileList);
        assertThat("The file list should contain 9 files", fileList.getSize(), is(equalTo(9)));
        RsyncFile file = fileList.getFile(3);
        assertNotNull("The fourth file has compressed data", file.getCompressedContents());
        assertThat("The compressed data is 1,198 bytes long", file.getCompressedContents().length, is(equalTo(1198)));
    }

    @Rule
    public final ExpectedException unknownPathException = ExpectedException.none();

    @Test
    public void rejectsUnknownItem() throws Exception {
        unknownPathException.expect(NoSuchPathException.class);
        module.getFileList("repository/made-up-thing", true);
    }
}
