package net.apnic.rpki.protocol;

import java.util.*;

/**
 * Validates that the properties supplied are legal for repositoryd.
 *
 * @author bje
 * @since 1.0
 */
class PropertiesValidator {
    private static final Set<String> legalArguments = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "verbose",              // ignored
            "info",                 // ignored
            "debug",                // ignored
            "do_stats",             // ignored
            "preserve_times",       // safe to ignore for sender
            "modify_window",        // safe to ignore for sender
            "preserve_links",       // safe to ignore when symlinks don't exist in the model
            "copy_links",           // "
            "copy_unsafe_links",    // "
            "safe_symlinks",        // "
            "copy_dirlinks",        // "
            "keep_dirlinks",        // "
            "ignore_times",         // safe to ignore for sender
            "one_file_system",      // safe to ignore when filesystems don't exist in the model
            "update_only",          // safe to ignore for sender
            "sparse_files",         // safe to ignore for sender
            "inplace",              // safe to ignore when not matching blocks
            "ignore_errors",        // safe to ignore when there are no I/O errors in the model
            "whole_file",           // safe to ignore when you assume this already
            "block_size",           // safe to ignore for sender
            "compress",             // ** supported! **
            "skip_compress",        // safe to ignore as client will accept compressed files anyway
            "def_compress_level",   // "
            "make_backups",         // safe to ignore if deletes are not permitted
            "backup_dir",           // "
            "backup_suffix",        // "
            "list_only",            // safe to ignore for sender
            "numeric_ids",          // safe to ignore if uid/gid is never permitted
            "io_timeout",           // you can say what you like, but I'll make my own choices on this one, thanks
            "iconv_opt",            // safe to ignore if iconv is unsupported
            "shell_cmd",            // ** supported! ** actually, client compatibility string
            "recurse",              // ** supported! **
            "xfer_dirs",            // ** supported! **
            "checksum_seed",        // ** supported! **
            "server",               // ** required! **
            "sender"                // ** required! **
    )));

    // Options that should never be sent to a sender or server
    //   help, version, msgs2stderr, quiet, output_motd, human_readable,
    //   archive, allow_inc_recurse, omit_dir_times, omit_link_times
    //   omit_link_times, am_root, munge_symlinks, chmod, size_only,
    //   ignore_non_existing, ignore_existing, max_size_arg, min_size_arg,
    //   preallocate_files, delete_during, delete_mode, delete_before,
    //   delete_after, delete_excluded, force_delete, max_delete, F,
    //   filter, exclude, include, exclude_from, include_from,
    //   compare_dest, copy_dest, link_dest, fuzzy, fuzzy_basis,
    //   do_compression, P, do_progress, keep_partial, partial_dir,
    //   delay_updates, prune_empty_dirs, logfile_name, logfile_format,
    //   stdout_format, itemize_changes, bwlimit, batch_name, usermap,
    //   groupmap, chown, connect_timeout, rsync_path, tmpdir, no_iconv,
    //   default_af_hint, allow_8bit_chars, bind_address, rsync_port,
    //   sockopts, password_file, blocking_io, outbuf_mode, remote_option,
    //   protocol_version, config, daemon, dparam, detach, no_detach

    // Dangerous options:
    //   preserve_perms, preserve_executability, acls, preserve_acls,
    //   xattrs, preserve_xattrs, preserve_uid, preserve_gid, D, no_D,
    //   preserve_devices, preserve_specials, hard_links, preserve_hard_links,

    // Candidates for support:
    //   dry_run: do not transfer sums or files
    //   relative_paths: it's not clear how this affects the sender
    //   implied_dirs: also not clear how this affects the sender
    //   append, append_mode: won't help for this model, and will cause server load
    //   missing_args: only single-arg form is supported, so it doesn't make a lot of sense to ignore errors
    //   remove_source_files: this can be sent, even though delete cannot, must be refused
    //   cvs_exclude: if filters are ever supported, this could be too
    //   always_checksum: affects what is sent in the file list bytestream
    //   bwlimit_arg: affects how quickly bytes are thrown at a client
    //   files_from, from0: not useful for RPKI repositories
    //   protect_args: changes the way arguments are sent on the wire; TODO: handle in RsyncHandler?
    //   use_qsort: makes the sort unstable and breaks incremental recursion, must be refused

    public static List<String> validateProperties(Map<String, List<String>> properties) {
        List<String> badArguments = new ArrayList<>();
        for (String argument: properties.keySet()) {
            if (!legalArguments.contains(argument))
                badArguments.add(argument);
        }

        return badArguments;
    }
}
