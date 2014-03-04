repositoryd
===========

An rsync-compatible RPKI repository daemon.

Roadmap
-------

Version 1.0

+ Check all arguments sent from the client for compatibility.

  Some arguments are not supported by this system, and must be rejected so that clients don't get unexpected behaviour.

+ Add a configuration system to the repositoryd module.

  Rather than running the dummy test main from the server module, the repositoryd module should allow users to
  configure repositories, logging, and access control.

Version 1.1

+ Add an rpki-repository protocol handler.

  A module to listen for rpki-repository publish and withdraw commands would provide an alternative data source to
  the file system repository.  Such a module should be configurable for its client list, allow each client to publish
  to a separate rsync module, and persist changes between restarts.

+ Add a JMS protocol handler.

  A module to listen for rpki repository update messages on a JMS topic to allow a distributed architecture.

+ Enable OSGi support.

  Use OSGi services to configure available modules.

+ Enable RPKI transaction support in file system repositories

  Only reload a file system repository when an RPKI transaction has completed, to ensure clients always see a
  consistent state.
