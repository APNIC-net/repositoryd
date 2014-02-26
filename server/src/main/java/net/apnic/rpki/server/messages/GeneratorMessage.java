package net.apnic.rpki.server.messages;

import io.netty.buffer.ByteBuf;
import net.apnic.rpki.protocol.Checksums;
import net.apnic.rpki.protocol.ItemFlag;
import net.apnic.rpki.protocol.TransferAttributes;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.EnumSet;

/**
 * Encapsulates a message from an rsync generator requesting a transfer.
 *
 * Because this message is complex on the wire, this class provides helper functionality to decode a message.
 * TODO: this helper functionality should move to a factory class.
 *
 * @author bje
 * @since 0.9
 */
public class GeneratorMessage extends WireMessage {
    private final int fileIndex;
    private EnumSet<ItemFlag> iflags;
    private byte basisType;
    private String xname;
    private int checksumBlockCount;
    private int checksumBlockSize;
    private int checksumLength;
    private int checksumRemainder;

    private enum Phase {
        IFLAGS,
        BASIS_TYPE,
        XNAME,
        CHECKSUM_HEADER,
        CHECKSUMS,
        DONE
    }

    private Phase phase = Phase.IFLAGS;

    /**
     * Constructs a new GeneratorMessage for the given fileIndex.
     *
     * @param fileIndex the file index of interest.
     * @since 0.9
     */
    public GeneratorMessage(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    /**
     * Progressively constructs a GeneratorMessage from received bytes.
     *
     * @param in the received bytes to process for message construction
     * @return true when the GeneratorMessage is fully constructed
     * @since 0.9
     */
    public boolean constructWithBytes(ByteBuf in) {
        // it would be so nice if this could be made the default
        in = in.order(ByteOrder.LITTLE_ENDIAN);

        switch (phase) {
            case DONE:
                return true;
            case IFLAGS:
                if (in.readableBytes() < 2) return false;
                iflags = ItemFlag.setFromInt(in.readUnsignedShort());

                phase = Phase.BASIS_TYPE;
                // Fall through to BASIS_TYPE now
            case BASIS_TYPE:
                if (iflags.contains(ItemFlag.ITEM_BASIS_TYPE_FOLLOWS)) {
                    if (in.readableBytes() < 1) return false;
                    basisType = in.readByte();
                }
                phase = Phase.XNAME;
                // Fall through to XNAME now
            case XNAME:
                if (iflags.contains(ItemFlag.ITEM_XNAME_FOLLOWS)) {
                    in.markReaderIndex();

                    int len = in.readUnsignedByte();
                    if ((len & 0x80) != 0) {
                        len = ((len & ~0x80) << 8) + in.readUnsignedByte();
                    }
                    if (in.readableBytes() < len) {
                        in.resetReaderIndex(); // reset the length read
                        return false;
                    }

                    byte[] data = new byte[len];
                    in.readBytes(data);
                    xname = new String(data, Charset.forName("UTF-8"));
                }

                phase = Phase.CHECKSUM_HEADER;
                // Fall through to CHECKSUM_HEADER now
            case CHECKSUM_HEADER:
                if (iflags.contains(ItemFlag.ITEM_TRANSFER)) {
                    if (in.readableBytes() < 16) return false;

                    checksumBlockCount = in.readInt();      // number of blocks
                    checksumBlockSize = in.readInt();       // should be max. 131,072
                    checksumLength = in.readInt();    // should be max. 16, MD5 digest length
                    checksumRemainder = in.readInt();   // bytes left after checksummed blocks
                }

                phase = Phase.CHECKSUMS;
                // Fall through to CHECKSUMS now
            case CHECKSUMS:
                // for the purposes of this client, checksum blocks are ignored
                int checksumBlockBytes = checksumBlockCount * (4 + checksumLength);
                if (in.readableBytes() < checksumBlockBytes) return false;
                in.skipBytes(checksumBlockBytes);

                // Well, that's all.
                phase = Phase.DONE;
                return true;
        }

        return false;
    }

    /**
     * Returns the attributes of the request.
     *
     * @return the attributes of the request
     * @since 0.9
     */
    public TransferAttributes getAttributes() {
        return new TransferAttributes(
                fileIndex,
                iflags,
                basisType,
                xname
        );
    }

    /**
     * Returns the checksums of the request.
     *
     * @return the checksums of the request
     * @since 0.9
     */
    public Checksums getChecksums() {
        return new Checksums(
                checksumBlockCount,
                checksumBlockSize,
                checksumLength,
                checksumRemainder
        );
    }
}
