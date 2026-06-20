# Truncate All Cache

The `truncateAll` operation provides a way to completely clear all cached tiles from GeoWebCache in a single request. This is useful when you need to remove the entire cache across all layers, gridsets, formats, and parameter filter combinations.

## Overview

`truncateAll` is a mass truncation operation that clears **all tiles in the entire GeoWebCache**. Unlike per-layer truncation (`truncateLayer`), this operation targets the entire cache system.

!!! warning
    This operation will delete **all** cached tiles across **all layers**. This action cannot be undone. Use with caution.

## When to Use truncateAll

The `truncateAll` operation is appropriate for the following scenarios:

- **Cache corruption**: If you suspect the entire cache has been corrupted or contains stale data, you can clear it completely
- **Data consistency**: When you need to ensure all cached tiles are regenerated from fresh source data
- **Storage reclamation**: To free up disk space by removing all cached tiles at once
- **Cache reset**: When performing major GeoServer configuration changes that affect all layers
- **Maintenance windows**: During scheduled maintenance when tile caching is not critical

!!! note
    For more targeted cache clearing, consider using per-layer truncation (`truncateLayer`) instead, which only affects a specific layer.

## Web Administration Interface

### Using the Tile Layers Page

The easiest way to execute `truncateAll` is through the GeoServer web administration interface:

1. Navigate to **Tile Layers** under **GeoWebCache** in the web administration interface
2. Click the **"Empty all"** link at the top of the Tile Layers list

![](img/gwc_confirm.png)

*Confirmation dialog for truncateAll operation*

3. A confirmation dialog will appear asking you to confirm the operation
4. Click **OK** to proceed with clearing all cached tiles

!!! warning
    This will display a confirmation message before proceeding. Review the confirmation carefully as this action is irreversible.

Once completed, a message will appear showing the names of all layers that were truncated:

![](img/gwc_clean.png)

*Confirmation message after truncateAll completes*

## REST API

### Using the Mass Truncate Endpoint

The `truncateAll` operation is available via the GeoWebCache REST API at the `/masstruncate` endpoint.

#### URL

```
POST /geoserver/gwc/rest/masstruncate
```

#### Request Parameters

| Parameter | Required | Type | Description |
|-----------|----------|------|-------------|
| `requestType` | Yes | String | Must be set to `truncateAll` |

#### Example cURL Request

```bash
curl -v -u admin:geoserver -X POST \
  "http://localhost:8080/geoserver/gwc/rest/masstruncate?requestType=truncateAll"
```

Replace `admin:geoserver` with your actual GeoServer credentials.

#### XML Request Example

If you prefer to use an XML request body:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<truncateAllRequest>
  <requestType>truncateAll</requestType>
</truncateAllRequest>
```

Send it with:

```bash
curl -v -u admin:geoserver -X POST \
  -H "Content-type: text/xml" \
  -d @truncateall-request.xml \
  "http://localhost:8080/geoserver/gwc/rest/masstruncate"
```

#### Successful Response

A successful `truncateAll` request will return HTTP status code `200 OK` with a response containing the list of layers that were truncated:

```xml
<truncateAllResponse>
  <truncatedLayers>workspace1:layer1,workspace1:layer2,workspace2:layer3</truncatedLayers>
</truncateAllResponse>
```

The response includes a comma-separated list of all layers from which tiles were removed.

#### Error Response

If the request fails (e.g., due to authentication issues or server errors), an appropriate HTTP error code will be returned:

- `401 Unauthorized` - Invalid or missing credentials
- `403 Forbidden` - User lacks permission to perform this operation
- `500 Internal Server Error` - Server-side error during truncation

## REST API - Query Parameter Method

You can also use the simpler query parameter approach:

```bash
curl -v -u admin:geoserver -X POST \
  "http://localhost:8080/geoserver/gwc/rest/masstruncate?requestType=truncateAll"
```

This method does not require the `layer` parameter, which is only applicable for `truncateLayer` operations.

## Other Truncation Request Types

While this document focuses on `truncateAll`, the REST API supports additional request types for more granular cache control:

| Request Type | Purpose | Parameters |
|--------------|---------|------------|
| `truncateAll` | Clear entire cache | None (no parameters needed) |
| `truncateLayer` | Clear specific layer cache | `layer` - layer name |
| `truncateParameters` | Clear cache for specific parameters | `layer`, parameter filters |
| `truncateExtent` | Clear cache within spatial bounds | `layer`, bounds |
| `truncateOrphans` | Clean up orphaned tiles | None |

See the [Mass Truncate API documentation](index.md) for details on other request types.

## Checking Available Request Types

To see all available truncation request types supported by your GeoServer instance, make a GET request:

```bash
curl -v -u admin:geoserver \
  "http://localhost:8080/geoserver/gwc/rest/masstruncate"
```

Response:

```xml
<massTruncateRequests href="http://localhost:8080/geoserver/gwc/rest/masstruncate.xml">
  <requestType>truncateLayer</requestType>
  <requestType>truncateParameters</requestType>
  <requestType>truncateOrphans</requestType>
  <requestType>truncateExtent</requestType>
  <requestType>truncateAll</requestType>
</massTruncateRequests>
```

## Security Considerations

`truncateAll` is a privileged operation that should only be accessible to administrators:

- **Authentication required**: You must provide valid GeoServer admin credentials
- **Authorization**: User must have administrative privileges to GeoWebCache
- **Audit logging**: The operation will be logged in GeoServer's audit logs
- **Role-based access**: Can be restricted using GeoServer's security system

For more information on GeoServer security, see the [Security section](../../security/index.md).

## Performance Implications

Executing `truncateAll` has the following performance implications:

- **Blocking operation**: The truncation process will block while deleting tiles from disk
- **I/O intensive**: Large caches may take several minutes to clear
- **No seeding impact**: Truncation does not affect the seeding thread pool
- **Cache re-population**: After truncation, tiles will need to be regenerated on-demand or via seeding

!!! tip
    For large caches, consider executing `truncateAll` during a maintenance window to minimize impact on users.

## Monitoring Truncation Progress

When using the web interface, progress is shown in the confirmation dialog. When using the REST API:

- The request will block until truncation is complete
- Monitor GeoServer logs for progress messages
- Check disk usage to verify tiles are being deleted

Example log messages:

```
INFO [geowebcache.service.Rest] Mass Truncate Completed
INFO [geowebcache.service.Rest] Truncated Layers : workspace1:layer1,workspace1:layer2,workspace2:layer3
```

## Alternative: Per-Layer Truncation

If you only need to clear the cache for specific layers, use the `truncateLayer` request type instead:

```bash
curl -v -u admin:geoserver -X POST \
  "http://localhost:8080/geoserver/gwc/rest/masstruncate?requestType=truncateLayer&layer=workspace:layername"
```

See the [Seeding and Truncating](seed.md) documentation for more information on per-layer operations.

## Troubleshooting

### Truncation Appears to Hang

If the truncation seems to be taking longer than expected:

1. Check disk I/O performance
2. Monitor GeoServer logs for errors
3. Verify disk space is available
4. Consider stopping GeoServer and manually cleaning the cache directory if necessary

### Permission Denied Errors

If you receive a permission error:

1. Verify your GeoServer credentials
2. Ensure your user has administrative privileges
3. Check GeoServer security settings
4. Consult the [Security](../../security/index.md) documentation

### Cache Not Actually Cleared

If tiles appear to still be present after truncation:

1. Clear your web browser cache
2. Verify the REST API returned HTTP 200
3. Check that you're looking at the correct cache directory
4. Ensure no other GeoServer instances are accessing the cache

## See Also

- [Tile Layers](../webadmin/layers.md) - Web UI for cache management
- [Seeding and Truncating](seed.md) - Per-layer seeding and truncation via REST API
- [GeoWebCache REST API](index.md) - Full REST API reference
- [Mass Truncate API Specification](../../../api/1.0.0/gwcmasstruncate.yaml) - OpenAPI specification
