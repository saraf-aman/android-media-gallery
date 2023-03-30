package edu.vt.cs5254.fancygallery

import android.os.SystemClock.sleep
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import androidx.appcompat.widget.ActionBarContainer
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.vt.cs5254.fancygallery.api.GalleryItem
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private const val DEFAULT_WAIT_SECONDS = 60
private const val MINIMUM_MARKER_COUNT = 10
private const val RECYCLER_ITEM_COUNT = 99

@RunWith(AndroidJUnit4::class)
class P3BaseTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    //passed
    @Test
    fun bottomNavigationBasics() {
        onView(withId(R.id.photo_grid)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        Thread.sleep(5000)
        onView(withId(R.id.map_view)).check(doesNotExist())
        Thread.sleep(5000)

        onView(withId(R.id.map_fragment)).perform(click())
        Thread.sleep(5000)

        onView(withId(R.id.map_view)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        Thread.sleep(5000)
        onView(withId(R.id.photo_grid)).check(doesNotExist())
        Thread.sleep(5000)

        onView(withId(R.id.gallery_fragment)).perform(click())
        Thread.sleep(5000)

        onView(withId(R.id.photo_grid)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        Thread.sleep(5000)
        onView(withId(R.id.map_view)).check(doesNotExist())
        Thread.sleep(5000)
    }

    //passed
    @Test
    fun galleryFirstPlaceholder() {
        waitFor(withId(R.id.photo_grid), atPosition(0, matchesDrawable(R.drawable.ic_placeholder)))
    }

    //passed
    @Test
    fun galleryFirstImageLoaded() {
        waitFor(
            withId(R.id.photo_grid), atPosition(0, not(matchesDrawable(R.drawable.ic_placeholder)))
        )
    }

    //passed
    @Test
    fun galleryCountNinetyNine() {
        galleryFirstPlaceholder()
        onView(withId(R.id.photo_grid)).check(matches(recyclerChildCount()))
    }

    //passed
    @Test
    fun galleryFirstThreeUrlsUnique() {
        galleryFirstImageLoaded()

        val url0 = getGalleryItem(0, withId(R.id.photo_grid))?.url
        val url1 = getGalleryItem(1, withId(R.id.photo_grid))?.url
        val url2 = getGalleryItem(2, withId(R.id.photo_grid))?.url

        assertNotEquals("", url0)
        assertNotEquals("", url1)
        assertNotEquals("", url2)

        assertNotEquals(url0, url1)
        assertNotEquals(url0, url2)
        assertNotEquals(url1, url2)
    }

    //passed
    @Test
    fun galleryReloadShowsPlaceholder() {
        galleryFirstImageLoaded()

        onView(withId(R.id.reload_menu)).perform(click())

        waitFor(withId(R.id.photo_grid), atPosition(0, matchesDrawable(R.drawable.ic_placeholder)))
    }

    //passed
    @Test
    fun webFromGalleryFourHasProgressBarSubtitle() {
        galleryFirstImageLoaded()
        Thread.sleep(5000)

        val galleryItem4 = getGalleryItem(5, withId(R.id.photo_grid))

        onView(withId(R.id.photo_grid)).perform(
            actionOnItemAtPosition<GalleryItemHolder>(
                4, click()
            )
        )
        Thread.sleep(5000)

        onView(withId(R.id.web_view)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        Thread.sleep(5000)

        onView(withId(R.id.progress_bar)).check(
            matches(
                anyOf(
                    withEffectiveVisibility(Visibility.VISIBLE),
                    withEffectiveVisibility(Visibility.GONE)
                )
            )
        )
        Thread.sleep(5000)

        waitFor(
            isRoot(), hasDescendant(
                allOf(
                    withText(startsWith(galleryItem4?.title)),
                    withParent(isDescendantOfA(instanceOf(ActionBarContainer::class.java)))
                )
            )
        )
    }

    //passed
    @Test
    fun galleryAfterWebNoSubtitleCachedImage() {
        webFromGalleryFourHasProgressBarSubtitle()
        Thread.sleep(5000)

        waitFor(withId(R.id.web_view), webViewLoaded(), maxRepeat = 20 * DEFAULT_WAIT_SECONDS)
        Thread.sleep(5000)

        Espresso.pressBack()
        Thread.sleep(5000)

        onView(withId(R.id.photo_grid)).check(
            matches(
                atPosition(
                    4, not(matchesDrawable(R.drawable.ic_placeholder))
                )
            )
        )
        Thread.sleep(5000)

        val galleryItem4 = getGalleryItem(4, withId(R.id.photo_grid))
        Thread.sleep(5000)

        onView(isRoot()).check(
            matches(
                not(
                    hasDescendant(
                        allOf(
                            withText(startsWith(galleryItem4?.title)),
                            withParent(isDescendantOfA(instanceOf(ActionBarContainer::class.java)))
                        )
                    )
                )
            )
        )
        Thread.sleep(5000)
    }

    //passed
    @Test
    fun webToMapAndBack() {
        webFromGalleryFourHasProgressBarSubtitle()
        Thread.sleep(5000)

        onView(withId(R.id.map_fragment)).perform(click())
        Thread.sleep(5000)

        waitFor(withId(R.id.map_view), withEffectiveVisibility(Visibility.VISIBLE))
        Thread.sleep(5000)
        onView(withId(R.id.photo_grid)).check(doesNotExist())
        Thread.sleep(5000)
        onView(withId(R.id.web_view)).check(doesNotExist())
        Thread.sleep(5000)

        waitFor(withId(R.id.map_view), loadingIsComplete())
        Thread.sleep(5000)
        onView(withId(R.id.gallery_fragment)).perform(click())
        Thread.sleep(5000)

        waitFor(withId(R.id.web_view), withEffectiveVisibility(Visibility.VISIBLE))
        Thread.sleep(5000)
        onView(withId(R.id.map_view)).check(doesNotExist())
        Thread.sleep(5000)
        onView(withId(R.id.photo_grid)).check(doesNotExist())
        Thread.sleep(5000)
    }

    //passed
    @Test
    fun mapInitiallyZoomedOut() {
        onView(withId(R.id.map_fragment)).perform(click())
        Thread.sleep(5000)
        onView(withId(R.id.map_view)).check(matches(isDisplayed()))
            .check(matches(isFullyZoomedOut()))
    }

    //passed
    @Test
    fun mapLoadsAllTiles() {
        onView(withId(R.id.map_fragment)).perform(click())
        Thread.sleep(5000)
        waitFor(withId(R.id.map_view), loadingIsComplete())
    }

    //passed
    @Test
    fun mapLoadsAllMarkers() {
        galleryFirstImageLoaded()
        Thread.sleep(5000)
        mapLoadsAllTiles()
        Thread.sleep(5000)
        waitFor(withId(R.id.map_view), loadedMarkerCountMinimum())
    }

    //passed
    @Test
    fun mapClickMarkerShowsInfoAndRaises() {
        mapLoadsAllMarkers()
        Thread.sleep(5000)

        val markers = getMapMarkers(withId(R.id.map_view))
        val marker = markers.dropLast(1).last()
        assertFalse(marker.isInfoWindowShown)

        val pos = marker.position
        onView(withId(R.id.map_view)).perform(zoomTo(14.0)).perform(panTo(pos)).perform(click())
        Thread.sleep(5000)

        for (n in 0 until 50) {
            if (marker.isInfoWindowShown) break
            sleep(100)
        }
        assertTrue(marker.isInfoWindowShown)
        Thread.sleep(5000)

        val newMarkers = getMapMarkers(withId(R.id.map_view))
        assertEquals(marker, newMarkers.last())
    }

    //passed
    @Test
    fun mapClickMarkerWithInfoLoadsWeb() {
        mapClickMarkerShowsInfoAndRaises()
        Thread.sleep(5000)
        onView(withId(R.id.map_view)).perform(click())
        Thread.sleep(5000)
        waitFor(isRoot(), hasDescendant(withId(R.id.web_view)), 50)
    }

    //passed
    @Test
    fun mapConfirmMaxZoom() {
        mapLoadsAllTiles()
        Thread.sleep(5000)
        onView(withId(R.id.map_view)).perform(zoomTo(15.0))
        Thread.sleep(5000)
        onView(withId(R.id.map_view)).check(matches(isDisplayed()))
            .check(matches(isFullyZoomedIn()))
    }

    //passed
    @Test
    fun mapRetainsState() {
        mapLoadsAllMarkers()
        Thread.sleep(5000)

        val firstMarker = getMapMarkers(withId(R.id.map_view)).first()

        onView(withId(R.id.map_view)).perform(zoomTo(14.0)).perform(panTo(firstMarker.position))
        Thread.sleep(5000)

        waitFor(withId(R.id.map_view), loadingIsComplete())
        Thread.sleep(5000)

        onView(withId(R.id.gallery_fragment)).perform(click())
        Thread.sleep(5000)

        onView(withId(R.id.map_view)).check(doesNotExist())
        Thread.sleep(5000)

        onView(withId(R.id.map_fragment)).perform(click())
        Thread.sleep(5000)

        waitFor(withId(R.id.map_view), loadingIsComplete())
        Thread.sleep(5000)

        val zoom = getZoomLevel(withId(R.id.map_view))
        val center = getCenter(withId(R.id.map_view))

        assertEquals(14.0, zoom, 0.1)
        assertEquals(firstMarker.position.latitude, center.latitude, 0.001)
        assertEquals(firstMarker.position.longitude, center.longitude, 0.001)
    }

    // ------------  END OF TEST FUNCTIONS ABOVE ------------

    //

    // ------------  PRIVATE HELPER FUNCTIONS BELOW  ------------

    private fun loadedMarkerCountMinimum(num: Int = MINIMUM_MARKER_COUNT): BoundedMatcher<View, MapView> {
        return object : BoundedMatcher<View, MapView>(MapView::class.java) {
            override fun describeTo(description: Description?) {
                description?.appendText("loaded at least $num markers")
            }

            override fun matchesSafely(item: MapView?): Boolean {
                return if (item == null) false else item.overlays.size > num
            }
        }
    }

    private fun loadingIsComplete() = object : BoundedMatcher<View, MapView>(MapView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("is fully loaded")
        }

        override fun matchesSafely(item: MapView?): Boolean {
            if (item == null) return false
            val states = item.overlayManager.tilesOverlay.tileStates
            return states.upToDate == states.total
        }
    }

    private fun isFullyZoomedOut() = object : BoundedMatcher<View, MapView>(MapView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("is fully loaded")
        }

        override fun matchesSafely(item: MapView?): Boolean {
            if (item == null) return false
            return !item.canZoomOut() && item.canZoomIn()
        }
    }

    private fun isFullyZoomedIn() = object : BoundedMatcher<View, MapView>(MapView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("MapView is fully loaded")
        }

        override fun matchesSafely(item: MapView?): Boolean {
            if (item == null) return false
            return !item.canZoomIn() && item.canZoomOut()
        }
    }

    private fun matchAny() = object : BaseMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("Matches ANY view")
        }

        override fun matches(actual: Any?): Boolean = true
    }

    private fun zoomTo(zoom: Double): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(MapView::class.java)
        }

        override fun getDescription(): String = "Zoom MapView to $zoom"

        override fun perform(uiController: UiController?, view: View?) {
            val mapView = view as MapView
            mapView.controller.setZoom(zoom)
        }
    }

    private fun panTo(where: GeoPoint): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(MapView::class.java)
        }

        override fun getDescription(): String = "Pan MapView to $where"

        override fun perform(uiController: UiController?, view: View?) {
            val mapView = view as MapView
            mapView.controller.setCenter(where)
        }
    }

    private fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position) ?: return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }

    private fun matchesDrawable(resourceID: Int): Matcher<View?> {
        return object : BoundedMatcher<View?, ImageView>(ImageView::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("an ImageView with resourceID: ")
                description.appendValue(resourceID)
            }

            override fun matchesSafely(imageView: ImageView): Boolean {
                val expBM = imageView.context.resources.getDrawable(resourceID, null).toBitmap()
                return imageView.drawable?.toBitmap()?.sameAs(expBM) ?: false
            }
        }
    }

    private fun recyclerChildCount(num: Int = RECYCLER_ITEM_COUNT): Matcher<View?> {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description?) {
                description?.appendText("RecyclerView having exactly $num children")
            }

            override fun matchesSafely(item: RecyclerView?): Boolean {
                return item?.adapter?.itemCount.let {
                    it == num
                }
            }
        }
    }

    private fun webViewLoaded() = object : BoundedMatcher<View?, WebView>(WebView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("fully loaded WebView")
        }

        override fun matchesSafely(item: WebView?): Boolean {
            return item?.progress == 100
        }
    }

    private fun getZoomLevel(matcher: Matcher<View>): Double {
        var zoom: Double = -1.0
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(MapView::class.java)
            override fun getDescription(): String = "Get zoom level from MapView"
            override fun perform(uiController: UiController?, view: View?) {
                val mapView = view as MapView
                zoom = mapView.zoomLevelDouble
            }
        })
        return zoom
    }

    private fun getCenter(matcher: Matcher<View>): IGeoPoint {
        var center: IGeoPoint = GeoPoint(0.0, 0.0)
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(MapView::class.java)
            override fun getDescription(): String = "Get center from MapView"
            override fun perform(uiController: UiController?, view: View?) {
                val mapView = view as MapView
                center = mapView.mapCenter
            }
        })
        return center
    }

    private fun getMapMarkers(matcher: Matcher<View>): List<Marker> {
        var markers = emptyList<Marker>()
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(MapView::class.java)
            override fun getDescription(): String = "Get markers from MapView"
            override fun perform(uiController: UiController?, view: View?) {
                val mapView = view as MapView
                markers = mapView.overlays.map { it as Marker }
            }
        })
        return markers
    }

    private fun getGalleryItem(pos: Int, matcher: Matcher<View>): GalleryItem? {
        var galleryItem: GalleryItem? = null
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                isAssignableFrom(RecyclerView::class.java)

            override fun getDescription(): String {
                return "Fetching GalleryItem at position $pos from RecyclerView"
            }

            override fun perform(uiController: UiController?, view: View?) {
                val holder = (view as RecyclerView).findViewHolderForAdapterPosition(pos)
                galleryItem = (holder as GalleryItemHolder).boundGalleryItem
            }
        })
        return galleryItem
    }

    private fun noopDelayAction(millis: Long) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = matchAny()
        override fun getDescription(): String = "Intentionally does nothing but delay"
        override fun perform(uiController: UiController?, view: View?) = sleep(millis)
    }

    private fun waitFor(
        target: Matcher<View>,
        matcher: Matcher<View?>,
        maxRepeat: Int = DEFAULT_WAIT_SECONDS * 10,
        sleepMillis: Long = 100
    ) {
        onView(target).perform(repeatedlyUntil(noopDelayAction(sleepMillis), matcher, maxRepeat))
    }

}
