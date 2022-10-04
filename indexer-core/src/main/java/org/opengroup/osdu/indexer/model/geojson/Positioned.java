package org.opengroup.osdu.indexer.model.geojson;

/**
 * @author Andrei_Dalhikh
 */
public interface Positioned {
    Object getCoordinates();

    String getType();
}
