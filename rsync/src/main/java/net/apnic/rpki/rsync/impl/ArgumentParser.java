package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.RsyncException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.util.*;

/**
 * Parse arguments handed from a client to a server and provide an EnumSet&lt;Options&gt;
 *
 * Server implementations in this model are always senders.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
class ArgumentParser {
    private static final Map<String, Option> legalArguments = new HashMap<>();

    static {
        // These options are fully supported
        legalArguments.put("compress", Option.COMPRESS);
        legalArguments.put("shell_cmd", Option.CLIENT_FLAGS);
        legalArguments.put("recurse", Option.RECURSIVE);
        legalArguments.put("xfer_dirs", Option.TRANSFER_DIRS);
        legalArguments.put("checksum_seed", Option.SEED);
        legalArguments.put("server", Option.SERVER);
        legalArguments.put("sender", Option.SENDER);
        legalArguments.put("protect_args", Option.PROTECT_ARGS);

        // TODO: old-compress, new-compress

        // These options must become supported but currently are not
        legalArguments.put("inplace", Option.IN_PLACE);
        legalArguments.put("whole_file", Option.WHOLE_FILE);

        // These options are safe to ignore as they only affect logging/output
        legalArguments.put("verbose", null);
        legalArguments.put("info", null);
        legalArguments.put("debug", null);
        legalArguments.put("do_stats", null);

        // These options have no effect on a sender
        legalArguments.put("preserve_times", null);
        legalArguments.put("modify_window", null);
        legalArguments.put("ignore_times", null);
        legalArguments.put("update_only", null);
        legalArguments.put("sparse_files", null);
        legalArguments.put("block_size", null);
        legalArguments.put("list_only", null);

        // These options can be ignored as symlinks are not supported on the sender
        legalArguments.put("preserve_links", null);
        legalArguments.put("copy_links", null);
        legalArguments.put("copy_unsafe_links", null);
        legalArguments.put("safe_symlinks", null);
        legalArguments.put("copy_dirlinks", null);
        legalArguments.put("keep_dirlinks", null);

        // This option can be ignored as filesystems are not recognised by the sender
        legalArguments.put("one_file_system", null);

        // The following options are a bit more gray
        legalArguments.put("ignore_errors", null);       // safe to ignore when there are no I/O errors in the model

        legalArguments.put("skip_compress", null);       // safe to ignore as client will accept compressed files anyway
        legalArguments.put("def_compress_level", null);  // "

        legalArguments.put("make_backups", null);        // safe to ignore if deletes are not permitted
        legalArguments.put("backup_dir", null);          // "
        legalArguments.put("backup_suffix", null);       // "

        legalArguments.put("numeric_ids", null);         // safe to ignore if uid/gid is never permitted

        legalArguments.put("io_timeout", null);          // you can say what you like, but I'll make my own choices on this one, thanks

        legalArguments.put("iconv_opt", null);           // safe to ignore if iconv is unsupported
    }

    private final EnumMap<Option, String> options = new EnumMap<>(Option.class);
    private final List<String> paths = new ArrayList<>();

    /**
     * Create an ArgumentParser for the given protocol version
     *
     * @param version the protocol version to conform to
     */
    ArgumentParser(int version) {
        // it turns out, version doesn't matter
    }

    /**
     * Parse the arguments provided.  Incrementally adds newly parsed arguments to getter
     * values.
     *
     * @param arguments the arguments to parse
     * @throws RsyncException if the arguments are invalid
     */
    void parse(List<String> arguments) throws RsyncException {
        CommandLine commandLine;
        try {
            commandLine = new PosixParser().parse(Arguments.rsyncOptions(),
                    arguments.toArray(new String[arguments.size()]));
        } catch (ParseException e) {
            throw new RsyncException("bad arguments (" + e.getMessage() + ")", e);
        }

        for (org.apache.commons.cli.Option option : commandLine.getOptions()) {
            String argName = option.getArgName();
            if (argName == null) argName = option.getOpt();

            if (!legalArguments.containsKey(argName)) {
                throw new RsyncException(String.format("Argument '%s' not supported", argName));
            }
            Option opt = legalArguments.get(argName);
            if (opt != null) options.put(opt, option.getValue());
        }

        paths.addAll(commandLine.getArgList());
    }

    /**
     * Returns the options selected by the object's arguments.
     *
     * @return the options selected by the object's arguments
     */
    EnumMap<Option, String> getOptions() {
        return options;
    }

    /**
     * Returns the paths requested by the client.
     *
     * @return the paths requested by the client
     */
    List<String> getPaths() {
        return paths;
    }
}
