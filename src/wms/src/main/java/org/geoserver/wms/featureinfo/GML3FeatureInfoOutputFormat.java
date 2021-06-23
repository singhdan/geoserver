/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import javax.xml.namespace.QName;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geotools.feature.FeatureCollection;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.opengis.feature.type.Name;

/**
 * A GetFeatureInfo response handler specialized in producing GML 3 data for a GetFeatureInfo
 * request.
 *
 * <p>This class does not deals directly with GML encoding. Instead, it works by taking the
 * FeatureResults produced in <code>execute()</code> and constructs a <code>GetFeaturesResult</code>
 * wich is passed to a <code>GML2FeatureResponseDelegate</code>, as if it where the result of a
 * GetFeature WFS request.
 *
 * @author Niels Charlier
 */
public class GML3FeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {
    /**
     * The MIME type of the format this response produces: <code>"application/vnd.ogc.gml"</code>
     */
    public static final String FORMAT = "application/vnd.ogc.gml/3.1.1";

    private GML3OutputFormat outputFormat;

    /** Default constructor, sets up the supported output format string. */
    public GML3FeatureInfoOutputFormat(final GML3OutputFormat outputFormat) {
        this(outputFormat, FORMAT);
    }

    protected GML3FeatureInfoOutputFormat(GML3OutputFormat outputFormat, String format) {
        super(format);
        this.outputFormat = outputFormat;
    }

    /**
     * Takes the <code>FeatureResult</code>s generated by the <code>execute</code> method in the
     * superclass and constructs a <code>GetFeaturesResult</code> wich is passed to a <code>
     * GML2FeatureResponseDelegate</code>.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest fInfoReq, OutputStream out)
            throws ServiceException, IOException {

        // the 'response' object we'll pass to our OutputFormat
        FeatureCollectionType features = WfsFactory.eINSTANCE.createFeatureCollectionType();

        // the 'request' object we'll pass to our OutputFormat
        GetFeatureType gfreq = WfsFactory.eINSTANCE.createGetFeatureType();
        gfreq.setBaseUrl(fInfoReq.getBaseUrl());

        for (int i = 0; i < results.getFeature().size(); i++) {

            FeatureCollection fc = (FeatureCollection) results.getFeature().get(i);
            Name name = FeatureCollectionDecorator.getName(fc);
            QName qname = new QName(name.getNamespaceURI(), name.getLocalPart());

            features.getFeature().add(fc);

            QueryType qt = WfsFactory.eINSTANCE.createQueryType();

            qt.setTypeName(Collections.singletonList(qname));

            String crs = GML2EncodingUtils.epsgCode(fc.getSchema().getCoordinateReferenceSystem());
            if (crs != null) {
                final String srsName = "EPSG:" + crs;
                try {
                    qt.setSrsName(new URI(srsName));

                } catch (URISyntaxException e) {
                    throw new ServiceException(
                            "Unable to determite coordinate system for featureType "
                                    + fc.getSchema().getName().getLocalPart()
                                    + ".  Schema told us '"
                                    + srsName
                                    + "'",
                            e);
                }
            }
            gfreq.getQuery().add(qt);
        }

        // this is a dummy wrapper around our 'request' object so that the new Dispatcher will
        // accept it.
        Service serviceDesc = new Service("wms", null, null, Collections.emptyList());
        Operation opDescriptor = new Operation("", serviceDesc, null, new Object[] {gfreq});
        outputFormat.write(features, out, opDescriptor);
    }
}
