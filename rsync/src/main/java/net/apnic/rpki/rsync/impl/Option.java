package net.apnic.rpki.rsync.impl;

/**
* Created by bje on 21/07/2014.
*/
enum Option {
    // File list options
    INC_RECURSIVE,
    RECURSIVE,
    TRANSFER_DIRS,

    // Handshake/compatibility options
    SEED,
    CLIENT_FLAGS,
    PROTECT_ARGS,
    SERVER,
    SENDER,

    // Transfer options
    COMPRESS,
    IN_PLACE,
    WHOLE_FILE

}
