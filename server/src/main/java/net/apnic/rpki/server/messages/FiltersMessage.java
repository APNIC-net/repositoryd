package net.apnic.rpki.server.messages;

import java.util.List;

/**
 * Encapsulates a client request to filter file lists.
 *
 * @author bje
 * @since 0.9
 */
public class FiltersMessage extends WireMessage {
    private final List<String> filters;

    /**
     * Creates a new FiltersMessage.
     *
     * @param filters the filters
     * @since 0.9
     */
    public FiltersMessage(List<String>filters) {
        this.filters = filters;
    }

    /**
     * Returns the filters.
     * @return the filters
     * @since 0.9
     */
    public List<String> getFilters() { return filters; }
}
