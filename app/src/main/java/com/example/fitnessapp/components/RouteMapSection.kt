package com.example.fitnessapp.components

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// Single unified function for all map types
@Composable
fun RouteMapSection(
    htmlUrl: String? = null,
    mapHtml: String? = null,
    polyline: String? = null,
    gpxData: String? = null,
    modifier: Modifier = Modifier
) {
    when {
        gpxData != null -> {
            Log.d("RouteMapSection", "Using GPX data")
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Route Map",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        InteractiveWebViewMapFromGpx(gpxData = gpxData)
                    }
                }
            }
        }
        mapHtml != null -> {
            Log.d("RouteMapSection", "Using raw HTML map")
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Route Map",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        InteractiveWebViewMap(html = mapHtml)
                    }
                }
            }
        }
        htmlUrl != null -> {
            Log.d("RouteMapSection", "Using HTML URL: $htmlUrl")
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Route Map",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        InteractiveWebViewMap(url = htmlUrl)
                    }
                }
            }
        }
        polyline != null -> {
            Log.d("RouteMapSection", "Received polyline: $polyline")
            Log.d("RouteMapSection", "Polyline length: ${polyline.length}")
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Route Map",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        InteractiveWebViewMapFromPolyline(polyline = polyline)
                    }
                }
            }
        }
    }
}

// Updated InteractiveWebViewMap to handle both HTML and URL
@Composable
private fun InteractiveWebViewMap(
    html: String? = null,
    url: String? = null,
    modifier: Modifier = Modifier
) {
    var isMapLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .semantics {
                contentDescription = "Interactive route map"
            }
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )

                    // Enable WebView debugging for development
                    WebView.setWebContentsDebuggingEnabled(true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("WebViewConsole", "${consoleMessage?.messageLevel()}: ${consoleMessage?.message()} " +
                                "(${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                            return true
                        }

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            Log.d("RouteMapSection", "Loading progress: $newProgress%")
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            Log.d("RouteMapSection", "Page started loading: $url")
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d("RouteMapSection", "Page finished loading: $url")
                            Log.d("RouteMapSection", "Page title: ${view?.title}")
                            isMapLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Log.e("RouteMapSection", "WebView error: $errorCode - $description at $failingUrl")
                            hasError = true
                            isMapLoading = false
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            Log.e("RouteMapSection", "HTTP error: ${errorResponse?.statusCode} - ${errorResponse?.reasonPhrase}")
                        }
                    }

                    isMapLoading = true
                    hasError = false

                    // Load either HTML or URL
                    when {
                        html != null -> {
                            Log.d("RouteMapSection", "Loading HTML content with ${html.length} characters")
                            loadDataWithBaseURL(
                                null,
                                html,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                        url != null -> {
                            Log.d("RouteMapSection", "Loading URL: $url")
                            loadUrl(url)
                        }
                        else -> {
                            hasError = true
                            isMapLoading = false
                        }
                    }
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isMapLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.extendedColors.surfaceSubtle),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loading map...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Error state
        if (hasError) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Map loading error",
                        tint = MaterialTheme.extendedColors.warning,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Failed to load map",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check your connection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            hasError = false
                            isMapLoading = true
                            webView?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Retry loading map",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Control overlay (top-right corner)
        if (!isMapLoading && !hasError) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Zoom In button
                IconButton(
                    onClick = { webView?.zoomIn() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.ZoomIn,
                        contentDescription = "Zoom in on map",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom Out button
                IconButton(
                    onClick = { webView?.zoomOut() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.ZoomOut,
                        contentDescription = "Zoom out on map",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Fullscreen toggle button
                IconButton(
                    onClick = {
                        // TODO: Implement fullscreen functionality
                        // This would require navigation to a fullscreen map view
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.Fullscreen,
                        contentDescription = "View map in fullscreen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Separate function for polyline-based maps
@Composable
private fun InteractiveWebViewMapFromPolyline(
    polyline: String,
    modifier: Modifier = Modifier
) {
    // Decode polyline to lat/lng points
    val points = remember(polyline) { 
        val decoded = decodePolyline(polyline)
        Log.d("RouteMapSection", "Decoded ${decoded.size} points from polyline")
        if (decoded.isNotEmpty()) {
            Log.d("RouteMapSection", "First point: ${decoded.first()}")
            Log.d("RouteMapSection", "Last point: ${decoded.last()}")
        }
        decoded
    }

    if (points.isEmpty()) {
        // Show error if no valid route data
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Invalid route data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        // Generate Leaflet HTML with the decoded points
        val pointsJs = points.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ","
        ) { "[${it.first}, ${it.second}]" }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <style>
                    html, body { 
                        height: 100%; 
                        margin: 0;
                        padding: 0;
                    }
                    #map { 
                        width: 100%;
                        height: 100%; 
                        margin: 0;
                        padding: 0;
                    }
                </style>
            </head>
            <body>
                <div id="map">Loading map...</div>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script>
                    console.log('JavaScript is running');

                    window.onload = function() {
                        console.log('Window loaded, initializing map...');

                        try {
                            if (typeof L === 'undefined') {
                                console.error('Leaflet not loaded');
                                document.getElementById('map').innerHTML = '<div style="padding: 20px;">Leaflet library not loaded</div>';
                                return;
                            }

                            var map = L.map('map', {
                                zoomControl: true,
                                attributionControl: true
                            });

                            console.log('Map created, adding tile layer...');

                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; OpenStreetMap contributors',
                                maxZoom: 19
                            }).addTo(map);

                            console.log('Tile layer added, creating polyline...');

                            var points = [$pointsJs];
                            console.log('Points count:', points.length);

                            if (points.length > 0) {
                                var polyline = L.polyline(points, {
                                    color: '#6366F1',
                                    weight: 4,
                                    opacity: 0.8
                                }).addTo(map);

                                console.log('Polyline added, fitting bounds...');

                                map.fitBounds(polyline.getBounds(), {
                                    padding: [20, 20]
                                });

                                console.log('Map initialized successfully');
                            } else {
                                console.error('No valid points to display');
                                document.getElementById('map').innerHTML = '<div style="padding: 20px;">No valid points to display</div>';
                            }
                        } catch (e) {
                            console.error('Map initialization error:', e.toString());
                            document.getElementById('map').innerHTML = '<div style="padding: 20px;">Error loading map: ' + e.toString() + '</div>';
                        }
                    };

                    // Fallback if window.onload doesn't fire
                    setTimeout(function() {
                        if (document.getElementById('map').innerHTML === 'Loading map...') {
                            console.error('Map loading timeout');
                            document.getElementById('map').innerHTML = '<div style="padding: 20px;">Map loading timed out</div>';
                        }
                    }, 5000);
                </script>
            </body>
            </html>
        """.trimIndent()

        // Use the generic InteractiveWebViewMap with the generated HTML
        InteractiveWebViewMap(html = html, modifier = modifier)
    }
}

// Separate function for GPX data
@Composable
private fun InteractiveWebViewMapFromGpx(
    gpxData: String,
    modifier: Modifier = Modifier
) {
    // Parse GPX data (lat,lng;lat,lng;...) into coordinates
    val coordinates = remember(gpxData) {
        val allCoords = gpxData.split(";").mapNotNull { pair ->
            val parts = pair.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                if (lat != null && lng != null) {
                    Pair(lat, lng)
                } else null
            } else null
        }

        // Sample points if there are too many (reduce to max 500 points)
        if (allCoords.size > 500) {
            val step = allCoords.size / 500
            allCoords.filterIndexed { index, _ -> index % step == 0 }
        } else {
            allCoords
        }
    }

    Log.d("RouteMapSection", "Parsed ${coordinates.size} coordinates from GPX data (reduced from original)")

    if (coordinates.isEmpty()) {
        // Show error if no valid coordinates
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Invalid GPS data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        // Generate Leaflet HTML with the coordinates
        val pointsJs = coordinates.joinToString(
            separator = ","
        ) { "[${it.first}, ${it.second}]" }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body, #map { 
                        height: 100%; 
                        margin: 0;
                        padding: 0;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    try {
                        var map = L.map('map', {
                            zoomControl: true,
                            attributionControl: true
                        });

                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '&copy; OpenStreetMap contributors',
                            maxZoom: 19
                        }).addTo(map);

                        var points = [$pointsJs];

                        if (points.length > 0) {
                            var polyline = L.polyline(points, {
                                color: '#6366F1',
                                weight: 4,
                                opacity: 0.8
                            }).addTo(map);

                            map.fitBounds(polyline.getBounds(), {
                                padding: [20, 20]
                            });
                        } else {
                            console.error('No valid points to display');
                        }
                    } catch (e) {
                        console.error('Map initialization error:', e);
                        document.body.innerHTML = '<div style="padding: 20px;">Error loading map</div>';
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        // Use the generic InteractiveWebViewMap with the generated HTML
        InteractiveWebViewMap(html = html, modifier = modifier)
    }
}

// Utility function to decode Strava's Google polyline
private fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
    val poly = mutableListOf<Pair<Double, Double>>()
    var index = 0
    var lat = 0
    var lng = 0

    while (index < encoded.length) {
        var shift = 0
        var result = 0
        var byte: Int
        do {
            byte = encoded[index++].code - 63
            result = result or (byte and 0x1f shl shift)
            shift += 5
        } while (byte >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            byte = encoded[index++].code - 63
            result = result or (byte and 0x1f shl shift)
            shift += 5
        } while (byte >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        poly.add(Pair(lat * 1e-5, lng * 1e-5))
    }
    return poly
}
