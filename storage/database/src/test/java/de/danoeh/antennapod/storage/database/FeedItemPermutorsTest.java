package de.danoeh.antennapod.storage.database;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.model.feed.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for FeedItemPermutors.
 */
@RunWith(RobolectricTestRunner.class)
public class FeedItemPermutorsTest {

    @Test
    public void testEnsureNonNullPermutors() {
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);
        for (SortOrder sortOrder : SortOrder.values()) {
            assertNotNull("The permutor for SortOrder " + sortOrder + " is unexpectedly null",
                    FeedItemPermutors.getPermutor(sortOrder));
        }
    }

    @Test
    public void testPermutorForRule_EPISODE_TITLE_ASC() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.EPISODE_TITLE_A_Z);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_EPISODE_TITLE_ASC_NullTitle() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.EPISODE_TITLE_A_Z);

        List<FeedItem> itemList = getTestList();
        itemList.get(2) // itemId 2
                .setTitle(null);
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 2, 1, 3)); // after sorting
    }


    @Test
    public void testPermutorForRule_EPISODE_TITLE_DESC() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.EPISODE_TITLE_Z_A);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    @Test
    public void testPermutorForRule_DATE_ASC() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.DATE_OLD_NEW);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_DATE_ASC_NulPubDatel() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.DATE_OLD_NEW);

        List<FeedItem> itemList = getTestList();
        itemList.get(2) // itemId 2
                .setPubDate(null);
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 2, 1, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_DATE_DESC() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.DATE_NEW_OLD);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    @Test
    public void testPermutorForRule_DURATION_ASC() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.DURATION_SHORT_LONG);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_DURATION_DESC() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.DURATION_LONG_SHORT);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    @Test
    public void testPermutorForRule_size_asc() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.SIZE_SMALL_LARGE);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_size_desc() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.SIZE_LARGE_SMALL);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    @Test
    public void testPermutorForRule_DURATION_DESC_NullMedia() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.DURATION_LONG_SHORT);

        List<FeedItem> itemList = getTestList();
        itemList.get(1) // itemId 3
                .setMedia(null);
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 2, 1, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_FEED_TITLE_ASC() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.FEED_TITLE_A_Z);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_FEED_TITLE_DESC() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.FEED_TITLE_Z_A);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    @Test
    public void testPermutorForRule_FEED_TITLE_DESC_NullTitle() {
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.FEED_TITLE_Z_A);

        List<FeedItem> itemList = getTestList();
        itemList.get(1) // itemId 3
            .getFeed().setTitle(null);
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 2, 1, 3)); // after sorting
    }

    /**
     * Generates a list with test data.
     */
    private List<FeedItem> getTestList() {
        List<FeedItem> itemList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2019, 0, 1);  // January 1st
        Feed feed1 = new Feed(null, null, "Feed title 1");
        FeedItem feedItem1 = new FeedItem(1, "Title 1", null, null, calendar.getTime(), 0, feed1);
        FeedMedia feedMedia1 = new FeedMedia(0, feedItem1, 1000, 0, 100, null, null, null,
                System.currentTimeMillis(), null, 0, 0);
        feedItem1.setMedia(feedMedia1);
        itemList.add(feedItem1);

        calendar.set(2019, 2, 1);  // March 1st
        Feed feed2 = new Feed(null, null, "Feed title 3");
        FeedItem feedItem2 = new FeedItem(3, "Title 3", null, null, calendar.getTime(), 0, feed2);
        FeedMedia feedMedia2 = new FeedMedia(0, feedItem2, 3000, 0, 300, null, null, null,
                System.currentTimeMillis(), null, 0, 0);
        feedItem2.setMedia(feedMedia2);
        itemList.add(feedItem2);

        calendar.set(2019, 1, 1);  // February 1st
        Feed feed3 = new Feed(null, null, "Feed title 2");
        FeedItem feedItem3 = new FeedItem(2, "Title 2", null, null, calendar.getTime(), 0, feed3);
        FeedMedia feedMedia3 = new FeedMedia(0, feedItem3, 2000, 0, 200, null, null, null,
                System.currentTimeMillis(), null, 0, 0);
        feedItem3.setMedia(feedMedia3);
        itemList.add(feedItem3);

        return itemList;
    }

    /**
     * Checks if both lists have the same size and the same ID order.
     *
     * @param itemList Item list.
     * @param ids      List of IDs.
     * @return <code>true</code> if both lists have the same size and the same ID order.
     */
    private boolean checkIdOrder(List<FeedItem> itemList, long... ids) {
        if (itemList.size() != ids.length) {
            return false;
        }

        for (int i = 0; i < ids.length; i++) {
            if (itemList.get(i).getId() != ids[i]) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testSmartShuffleSpreadEvenlyDefault() {
        // Test that spread evenly is the default
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);
        assertTrue("Spread evenly should be enabled by default",
                UserPreferences.isSmartShuffleSpreadEvenly());
    }

    @Test
    public void testSmartShuffleSpreadEvenly() {
        // Test the "spread evenly" algorithm with multiple feeds
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);
        UserPreferences.setSmartShuffleSpreadEvenly(true);

        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.SMART_SHUFFLE_OLD_NEW);
        List<FeedItem> itemList = getSmartShuffleTestList();

        // Expected order: episodes should be spread out evenly
        // Feed A (3 eps): A1, A2, A3
        // Feed B (2 eps): B1, B2
        // Feed C (1 ep): C1
        // Spread evenly places largest first, spreading across slots
        permutor.reorder(itemList);

        // Verify no null items and size is correct
        for (FeedItem item : itemList) {
            assertNotNull("Item should not be null", item);
        }
        assertTrue("List should have 6 items", itemList.size() == 6);

        // Verify episodes from same feed maintain order
        int lastA = -1, lastB = -1, lastC = -1;
        for (FeedItem item : itemList) {
            if (item.getTitle().startsWith("A")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed A episodes should be in order", episodeNum > lastA);
                lastA = episodeNum;
            } else if (item.getTitle().startsWith("B")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed B episodes should be in order", episodeNum > lastB);
                lastB = episodeNum;
            } else if (item.getTitle().startsWith("C")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed C episodes should be in order", episodeNum > lastC);
                lastC = episodeNum;
            }
        }
    }

    @Test
    public void testSmartShuffleLegacy() {
        // Test the legacy smart shuffle algorithm for low-episode feeds sorted by pubdate
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);
        // Force set to false to ensure legacy algorithm is used
        UserPreferences.setSmartShuffleSpreadEvenly(false);

        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.SMART_SHUFFLE_OLD_NEW);
        List<FeedItem> itemList = getSmartShuffleTestList();

        permutor.reorder(itemList);

        // Verify no null items and size is correct
        for (FeedItem item : itemList) {
            assertNotNull("Item should not be null", item);
        }
        assertTrue("List should have 6 items", itemList.size() == 6);

        // Verify episodes from same feed maintain order
        int lastA = -1, lastB = -1, lastC = -1;
        for (FeedItem item : itemList) {
            if (item.getTitle().startsWith("A")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed A episodes should be in order", episodeNum > lastA);
                lastA = episodeNum;
            } else if (item.getTitle().startsWith("B")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed B episodes should be in order", episodeNum > lastB);
                lastB = episodeNum;
            } else if (item.getTitle().startsWith("C")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed C episodes should be in order", episodeNum > lastC);
                lastC = episodeNum;
            }
        }

        // For SMART_SHUFFLE_OLD_NEW, low-episode feeds are ordered by oldest first.
        assertTrue("First item should be from the oldest feed (A)", itemList.get(0).getTitle().startsWith("A"));
    }

    @Test
    public void testSmartShuffleToggleBetweenAlgorithms() {
        // Test that toggling between algorithms works correctly
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);

        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.SMART_SHUFFLE_OLD_NEW);

        // Get spread evenly result
        UserPreferences.setSmartShuffleSpreadEvenly(true);
        List<FeedItem> spreadEvenlyList = getSmartShuffleTestList();
        permutor.reorder(spreadEvenlyList);

        // Verify result is valid
        for (FeedItem item : spreadEvenlyList) {
            assertNotNull("Item should not be null", item);
        }
        assertTrue("Spread evenly list should have 6 items", spreadEvenlyList.size() == 6);

        // Get legacy result
        UserPreferences.setSmartShuffleSpreadEvenly(false);
        List<FeedItem> legacyList = getSmartShuffleTestList();
        permutor.reorder(legacyList);

        // Verify result is valid
        for (FeedItem item : legacyList) {
            assertNotNull("Item should not be null", item);
        }
        assertTrue("Legacy list should have 6 items", legacyList.size() == 6);

        // Verify episodes from same feed maintain order in legacy result
        int lastA = -1, lastB = -1, lastC = -1;
        for (FeedItem item : legacyList) {
            if (item.getTitle().startsWith("A")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed A episodes should be in order in legacy", episodeNum > lastA);
                lastA = episodeNum;
            } else if (item.getTitle().startsWith("B")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed B episodes should be in order in legacy", episodeNum > lastB);
                lastB = episodeNum;
            } else if (item.getTitle().startsWith("C")) {
                int episodeNum = Integer.parseInt(item.getTitle().substring(1));
                assertTrue("Feed C episodes should be in order in legacy", episodeNum > lastC);
                lastC = episodeNum;
            }
        }
    }

        @Test
        public void testSmartShuffleLegacyAscendingOrderMatchesForLowAndMultiEpisodeFeeds() {
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);
        UserPreferences.setSmartShuffleSpreadEvenly(false);

        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.SMART_SHUFFLE_OLD_NEW);
        List<FeedItem> itemList = getSmartShuffleLegacyMixedFeedList();
        permutor.reorder(itemList);

        assertTrue("Low-episode feeds should be ordered oldest to newest",
            indexOfTitle(itemList, "L11") < indexOfTitle(itemList, "L21"));
        assertTrue("Multi-episode feeds should be ordered oldest to newest",
            indexOfTitle(itemList, "M11") < indexOfTitle(itemList, "M21"));
        }

        @Test
        public void testSmartShuffleLegacyDescendingOrderMatchesForLowAndMultiEpisodeFeeds() {
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);
        UserPreferences.setSmartShuffleSpreadEvenly(false);

        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(SortOrder.SMART_SHUFFLE_NEW_OLD);
        List<FeedItem> itemList = getSmartShuffleLegacyMixedFeedList();
        permutor.reorder(itemList);

        assertTrue("Low-episode feeds should be ordered newest to oldest",
            indexOfTitle(itemList, "L21") < indexOfTitle(itemList, "L11"));
        assertTrue("Multi-episode feeds should be ordered newest to oldest",
            indexOfTitle(itemList, "M21") < indexOfTitle(itemList, "M11"));
        }

    /**
     * Generates a list with test data for smart shuffle testing.
     * Creates 6 items from 3 feeds (Feed A: 3 items, Feed B: 2 items, Feed C: 1 item)
     */
    private List<FeedItem> getSmartShuffleTestList() {
        List<FeedItem> itemList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();

        // Feed A - 3 episodes
        Feed feedA = new Feed(null, null, "Feed A");
        feedA.setId(1L);
        for (int i = 1; i <= 3; i++) {
            calendar.set(2019, 0, i);
            FeedItem item = new FeedItem(100 + i, "A" + i, null, null, calendar.getTime(), 0, feedA);
            item.setFeedId(1L);  // Set the feedId explicitly
            FeedMedia media = new FeedMedia(0, item, 1000, 0, 100, null, null, null,
                    System.currentTimeMillis(), null, 0, 0);
            item.setMedia(media);
            itemList.add(item);
        }

        // Feed B - 2 episodes
        Feed feedB = new Feed(null, null, "Feed B");
        feedB.setId(2L);
        for (int i = 1; i <= 2; i++) {
            calendar.set(2019, 1, i);
            FeedItem item = new FeedItem(200 + i, "B" + i, null, null, calendar.getTime(), 0, feedB);
            item.setFeedId(2L);  // Set the feedId explicitly
            FeedMedia media = new FeedMedia(0, item, 2000, 0, 200, null, null, null,
                    System.currentTimeMillis(), null, 0, 0);
            item.setMedia(media);
            itemList.add(item);
        }

        // Feed C - 1 episode
        Feed feedC = new Feed(null, null, "Feed C");
        feedC.setId(3L);
        calendar.set(2019, 2, 1);
        FeedItem item = new FeedItem(300, "C1", null, null, calendar.getTime(), 0, feedC);
        item.setFeedId(3L);  // Set the feedId explicitly
        FeedMedia media = new FeedMedia(0, item, 3000, 0, 300, null, null, null,
                System.currentTimeMillis(), null, 0, 0);
        item.setMedia(media);
        itemList.add(item);

        return itemList;
    }

    /**
     * Generates a list with both low-episode (<=5) and multi-episode (>5) feeds for legacy shuffle testing.
     */
    private List<FeedItem> getSmartShuffleLegacyMixedFeedList() {
        List<FeedItem> itemList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        Feed low1 = new Feed(null, null, "Low 1");
        low1.setId(11L);
        for (int i = 1; i <= 2; i++) {
            calendar.set(2019, 0, i);
            FeedItem item = new FeedItem(1100 + i, "L1" + i, null, null, calendar.getTime(), 0, low1);
            item.setFeedId(11L);
            item.setMedia(new FeedMedia(0, item, 1000, 0, 100, null, null, null,
                    System.currentTimeMillis(), null, 0, 0));
            itemList.add(item);
        }

        Feed low2 = new Feed(null, null, "Low 2");
        low2.setId(12L);
        calendar.set(2019, 1, 1);
        FeedItem low2Item = new FeedItem(1201, "L21", null, null, calendar.getTime(), 0, low2);
        low2Item.setFeedId(12L);
        low2Item.setMedia(new FeedMedia(0, low2Item, 1000, 0, 100, null, null, null,
                System.currentTimeMillis(), null, 0, 0));
        itemList.add(low2Item);

        Feed multi1 = new Feed(null, null, "Multi 1");
        multi1.setId(21L);
        for (int i = 1; i <= 6; i++) {
            calendar.set(2019, 2, i);
            FeedItem item = new FeedItem(2100 + i, "M1" + i, null, null, calendar.getTime(), 0, multi1);
            item.setFeedId(21L);
            item.setMedia(new FeedMedia(0, item, 1000, 0, 100, null, null, null,
                    System.currentTimeMillis(), null, 0, 0));
            itemList.add(item);
        }

        Feed multi2 = new Feed(null, null, "Multi 2");
        multi2.setId(22L);
        for (int i = 1; i <= 6; i++) {
            calendar.set(2019, 3, i);
            FeedItem item = new FeedItem(2200 + i, "M2" + i, null, null, calendar.getTime(), 0, multi2);
            item.setFeedId(22L);
            item.setMedia(new FeedMedia(0, item, 1000, 0, 100, null, null, null,
                    System.currentTimeMillis(), null, 0, 0));
            itemList.add(item);
        }

        return itemList;
    }

    private int indexOfTitle(List<FeedItem> items, String title) {
        for (int i = 0; i < items.size(); i++) {
            if (title.equals(items.get(i).getTitle())) {
                return i;
            }
        }
        return -1;
    }
}
