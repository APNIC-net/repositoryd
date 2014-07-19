The system is split into three tiers:
    1. Data services
    2. Protocol
    3. Networking

-----
Data services provides the following interfaces:
    Repository: the store of data to be transferred
    Node: an entry in a Repository

A concrete implementation of the Repository and Node interfaces is provided for file system backed service.

-----
Protocol support provides the following interfaces:
    Protocol: the communication state between remote and local end
    Module: a top level target provided by a Protocol in sender mode
    FileList: a list of files inside a Module

    -- scratch that --

    A sending rsync will:
        send file lists
        receive requests for files including checksummed blocks
        send the rsync optimised file data

    A receiving rsync will:
        receive file lists
        determine files to fetch and send requests for those files, including checksums of existing data
        receive the rsync optimised file data

    Module: the provider of a list of files to be transferred
        FileList: an iterable list of files
            File: a single file to transfer
                checksums, contents

    Protocol: the top level interface that interprets bytes received and determines state changes
        void receive(ByteBuffer input) throws ProtocolException // should return indication of termination
        ByteBuffer transmit(int bufferSize)     // return NULL if nothing pending
    ProtocolBuilder: a concrete builder providing a Protocol for a particular purpose
        Protocol proto = new ProtocolBuilder()
            .sender()
            .withModule(Module module)
            .enableThis()
            .disableThat()
            .build()

        Protocol proto = new ProtocolBuilder()
            .receiver()
            .target(File target)
            .enableThis()
            .disableThat()
            .build()

-----
Networking provides a Channel implementation for TCP/IP sockets.

On connect:
    Establish a Protocol object

On receive:
    Forward the bytes into the Protocol object
    Restart sending if it is stopped

On send buffer empty:
    Ask the Protocol object for more data to send

On disconnect:
    Clean up the Protocol object
