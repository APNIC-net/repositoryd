package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.Module;

import java.util.List;

/**
 * A list of modules offered by an rsync server.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
class ModuleListMessage extends AbstractBaseMessage {
    ModuleListMessage(List<? extends Module> modules) {
        StringBuilder builder = new StringBuilder();
        for (Module module : modules) {
            builder.append(module.getName());
            builder.append("\t");
            builder.append(module.getDescription());
            builder.append("\n");
        }
        builder.append("@RSYNCD: EXIT\n");
        setData(builder.toString().getBytes(UTF8));
    }
}
