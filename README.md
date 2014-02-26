repositoryd
===========

An rsync-compatible RPKI repository daemon.

Roadmap
-------

Version 1.0

+ Check all arguments sent from the client for compatibility.

  Some arguments are not supported by this system, and must be rejected so that clients don't get unexpected behaviour.

+ Add a configuration system to the distrib module.

  Rather than running the dummy test main from the server module, the distrib module should allow users to
  configure repositories, logging, and access control.

+ Add an rpki-repository protocol handler.

  A module to listen for rpki-repository publish and withdraw commands would provide an alternative data source to
  the file system repository.  Such a module should be configurable for its client list, allow each client to publish
  to a separate rsync module, and persist changes between restarts.
