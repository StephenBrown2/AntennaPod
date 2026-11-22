package de.danoeh.antennapod.ui.share;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ShareUtilsTest {

    @Test
    public void testPodlinkUrls() {
        Feed feed = new Feed("https://feeds.example.com/show.xml", null, "Show");
        FeedItem item = new FeedItem(0, "Episode", "guid-123?", null, null, FeedItem.PLAYED, feed);
        assertEquals("https://pod.link/aHR0cHM6Ly9mZWVkcy5leGFtcGxlLmNvbS9zaG93LnhtbA",
                ShareUtils.getPodlinkFeedUrl(feed));
        assertEquals("https://pod.link/aHR0cHM6Ly9mZWVkcy5leGFtcGxlLmNvbS9zaG93LnhtbA/episode/Z3VpZC0xMjM_",
                ShareUtils.getPodlinkEpisodeUrl(item));
    }

    @Test
    public void testNoPodlinkForPrivateFeeds() {
        Feed feed = new Feed("https://feeds.example.com/show.xml", null, "Show");
        FeedItem item = new FeedItem(0, "Episode", "guid", null, null, FeedItem.PLAYED, feed);
        assertTrue(ShareUtils.hasPodlinkToShare(item));
        feed.setPreferences(new FeedPreferences(0, FeedPreferences.AutoDownloadSetting.GLOBAL,
                FeedPreferences.AutoDeleteAction.GLOBAL, null, null, "user", "pass"));
        assertFalse(ShareUtils.hasPodlinkToShare(item));
        feed.setPreferences(null);
        feed.setDownloadUrl("https://feeds.example.com/show.xml?token=secret");
        assertFalse(ShareUtils.hasPodlinkToShare(item));
        feed.setDownloadUrl("https://user:pass@feeds.example.com/show.xml");
        assertFalse(ShareUtils.hasPodlinkToShare(item));
    }
}
