package ru.asartamonov.kmlhandler;

import java.util.Collection;

/**
 * Simple functional converter - interface, method implements this interface takes
 * List of parameters T type and converts it to list of same T type parameters.
 *
 * @param <T> a Collection of kml documents
 */
@FunctionalInterface
interface KmlTransformer<T> {
    Collection<T> transform(Collection<T> collection);
}
