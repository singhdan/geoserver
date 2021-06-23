/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

public class ItemsTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/items-LANDSAT8.json");
        copyTemplate("/items-SAS1.json");
        copyTemplate("/box.json");
        copyTemplate("/parentLink.json");
    }

    @Test
    public void testSentinelItemsJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/collections/SENTINEL2/items?limit=50", 200);

        // global properties
        assertEquals("FeatureCollection", json.read("type"));
        assertEquals(Integer.valueOf(19), json.read("numberMatched"));
        assertEquals(Integer.valueOf(19), json.read("numberReturned"));
        assertEquals(STACService.STAC_VERSION, json.read("stac_version"));

        // global links (only one expected for this one, there is no paging)
        DocumentContext link = readSingleContext(json, "links");
        assertEquals("self", link.read("rel"));
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, link.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?limit=50",
                link.read("href"));

        // check it's only sentinel2 data
        List<String> collections = json.read("features[*].collection");
        assertThat(new HashSet<>(collections), containsInAnyOrder("SENTINEL2"));
        assertEquals(Integer.valueOf(19), json.read("features.length()", Integer.class));

        // read single reference feature
        DocumentContext s2Sample =
                readSingleContext(
                        json,
                        "features[?(@.id == 'S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02"
                                + ".04')]");
        checkSentinel2Sample(s2Sample);

        // a sample image that does have an actual time range
        DocumentContext s2Range =
                readSingleContext(
                        json,
                        "features[?(@.id == 'S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04')]");
        assertEquals("2017-02-26T10:20:21.026+00:00", s2Range.read("properties.start_datetime"));
        assertEquals("2017-02-26T10:30:00.031+00:00", s2Range.read("properties.end_datetime"));
    }

    @Test
    public void testSentinelItemJSON() throws Exception {
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/collections/SENTINEL2/items/S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        200);

        assertEquals("Feature", json.read("type"));
        checkSentinel2Sample(json);
    }

    @Test
    public void testDisabledItem() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/stac/collections/LANDSAT8/items/LS8_TEST.DISABLED", 404);

        assertEquals("InvalidParameterValue", json.read("code"));
        assertEquals("Could not locate item LS8_TEST.DISABLED", json.read("description"));
    }

    @Test
    public void testLandsat8ItemsJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/collections/LANDSAT8/items", 200);

        assertEquals("FeatureCollection", json.read("type"));

        // check it's only landsat8 data
        List<String> collections = json.read("features[*].collection");
        assertThat(new HashSet<>(collections), containsInAnyOrder("LANDSAT8"));
        assertEquals(Integer.valueOf(1), json.read("features.length()", Integer.class));

        // read single reference feature, will test only peculiarities of this
        DocumentContext l8Sample = readSingleContext(json, "features[?(@.id == 'LS8_TEST.02')]");
        checkLandsat8_02(l8Sample);
    }

    @Test
    public void testLandsat8ItemJSON() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/stac/collections/LANDSAT8/items/LS8_TEST.02", 200);

        assertEquals("Feature", json.read("type"));

        // it's the expected feature
        assertEquals("LS8_TEST.02", json.read("id"));
        checkLandsat8_02(json);
    }

    @Test
    public void testSentinelItemsHTML() throws Exception {
        Document doc = getAsJSoup("ogc/stac/collections/SENTINEL2/items?f=html");
        assertEquals("SENTINEL2 items", doc.select("#title").text());

        // the item identifiers
        Set<String> titles =
                doc.select("div.card-header h2")
                        .stream()
                        .map(e -> e.text())
                        .collect(Collectors.toSet());
        assertThat(titles, Matchers.everyItem(Matchers.startsWith("S2A_OPER_MSI")));

        // test the Sentinel2 entry
        Elements s2Body =
                doc.select(
                        "div.card-header:has(a:contains(S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04)) ~ div.card-body");
        testSentinel2SampleHTML(s2Body);
    }

    private void testSentinel2SampleHTML(Elements elements) {
        assertTextContains(elements, "[data-tid='gbounds']", "-119,174, 33,333, -117,969, 34,338.");
        assertTextContains(elements, "[data-tid='ccover']", "7");
    }

    @Test
    public void testPagingLinksFirstPage() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/collections/SENTINEL2/items?limit=5", 200);

        // two links expected, self and next
        assertEquals(Integer.valueOf(2), json.read("links.length()"));

        // self link
        DocumentContext self = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?limit=5",
                self.read("href"));

        // next link (order should be stable, linked hash maps all around)
        DocumentContext next = readSingleContext(json, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?limit=5&startIndex=5",
                next.read("href"));
    }

    @Test
    public void testPagingLinksSecondPage() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/stac/collections/SENTINEL2/items?startIndex=5&limit=5", 200);

        // three links expected, prev, self and next
        assertEquals(Integer.valueOf(3), json.read("links.length()"));

        // prev link
        DocumentContext prev = readSingleContext(json, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?startIndex=0&limit=5",
                prev.read("href"));

        // self link
        DocumentContext self = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?startIndex=5&limit=5",
                self.read("href"));

        // next link
        DocumentContext next = readSingleContext(json, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?startIndex=10&limit=5",
                next.read("href"));
    }

    @Test
    public void testPagingLinksLastPage() throws Exception {
        // assuming 19 items
        DocumentContext json =
                getAsJSONPath("ogc/stac/collections/SENTINEL2/items?startIndex=15&limit=5", 200);

        // two links expected, prev and self
        assertEquals(Integer.valueOf(2), json.read("links.length()"));

        // prev link
        DocumentContext prev = readSingleContext(json, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?startIndex=10&limit=5",
                prev.read("href"));

        // self link
        DocumentContext self = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?startIndex=15&limit=5",
                self.read("href"));
    }

    @Test
    public void testPagingLinksLastPageFull() throws Exception {
        // assuming 19 items, and asking a page of 19. There should not be another page
        DocumentContext json = getAsJSONPath("ogc/stac/collections/SENTINEL2/items?limit=19", 200);

        // Only self link expected
        assertEquals(Integer.valueOf(1), json.read("links.length()"));

        // self link
        DocumentContext self = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/items?limit=19",
                self.read("href"));
    }

    @Test
    public void testTimeFilterInstant() throws Exception {
        // only one feature intersects this point in time
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/collections/SENTINEL2/items?limit=19&datetime=2017-02-26T10:25:00Z",
                        200);

        assertEquals(Integer.valueOf(1), json.read("numberMatched"));
        assertEquals(Integer.valueOf(1), json.read("numberReturned"));
        String expectedId = "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04";
        assertThat(json.read("features[*].id"), contains(expectedId));
    }

    @Test
    public void testTimeFilterRange() throws Exception {
        // only two sentinel features available in 2017 feb/march
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/collections/SENTINEL2/items?limit=19&datetime=2017-02-25/2017-03-31",
                        200);

        assertEquals(Integer.valueOf(2), json.read("numberMatched"));
        assertEquals(Integer.valueOf(2), json.read("numberReturned"));
        assertThat(
                json.read("features[*].id"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04"));
    }

    @Test
    public void testSpaceFilter() throws Exception {
        // only two features matching
        DocumentContext json =
                getAsJSONPath("ogc/stac/collections/SENTINEL2/items?bbox=16,42,17,43", 200);

        assertEquals(Integer.valueOf(2), json.read("numberMatched"));
        assertEquals(Integer.valueOf(2), json.read("numberReturned"));
        assertThat(
                json.read("features[*].id"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWG_N02.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01"));
    }

    @Test
    public void testCloudCoverFilter() throws Exception {
        // only one feature matching.
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/collections/SENTINEL2/items?filter=eo:cloud_cover > 60&filter-lang=cql-text",
                        200);

        assertEquals(Integer.valueOf(1), json.read("numberMatched"));
        assertEquals(Integer.valueOf(1), json.read("numberReturned"));
        assertThat(
                json.read("features[*].id"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TUH_N02.01"));
    }

    @Test
    public void testSpaceCQLFilter() throws Exception {
        // only one feature matching.
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/collections/SENTINEL2/items?filter=eo:cloud_cover = 1&filter-lang=cql-text&bbox=16,42,17,43",
                        200);

        assertEquals(Integer.valueOf(1), json.read("numberMatched"));
        assertEquals(Integer.valueOf(1), json.read("numberReturned"));
        assertThat(
                json.read("features[*].id"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01"));
    }

    @Test
    public void testSAS1ItemsJSON() throws Exception {
        // this one has a custom template with relative imports for the bbox and a link
        DocumentContext json = getAsJSONPath("ogc/stac/collections/SAS1/items", 200);

        assertEquals("FeatureCollection", json.read("type"));

        // check it's only landsat8 data
        List<String> collections = json.read("features[*].collection");
        assertThat(new HashSet<>(collections), containsInAnyOrder("SAS1"));
        assertEquals(Integer.valueOf(2), json.read("features.length()", Integer.class));

        // read single reference feature, will test only peculiarities of this
        DocumentContext item =
                readSingleContext(json, "features[?(@.id == 'SAS1_20180227102021.02')]");

        // the bbox is in an included template
        assertEquals(Integer.valueOf(-180), item.read("bbox[0]", Integer.class));
        assertEquals(Integer.valueOf(-90), item.read("bbox[1]", Integer.class));
        assertEquals(Integer.valueOf(180), item.read("bbox[2]", Integer.class));
        assertEquals(Integer.valueOf(90), item.read("bbox[3]", Integer.class));

        // the parent link is included as well
        DocumentContext link = readSingleContext(item, "links[?(@.rel == 'collection')]");
        assertEquals(
                "http://localhost:8080/geoserver/ogcapi/stac/collections/SAS1", link.read("href"));
    }
}
