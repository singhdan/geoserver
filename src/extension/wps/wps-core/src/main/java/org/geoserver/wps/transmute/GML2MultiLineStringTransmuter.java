/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.transmute;

/**
 * Transmuter for GML2 MultiLineString geometries
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class GML2MultiLineStringTransmuter extends GML2ComplexTransmuter {
    /** @see ComplexTransmuter#getSchema(String) */
    @Override
    public String getSchema(String urlBase) {
        return urlBase + "ows?service=WPS&request=GetSchema&Identifier=gml2multilinestring.xsd";
    }
}
