package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Provides method for sorting the a list of {@link FeedItem} according to rules.
 */
public class FeedItemPermutors {

    /**
     * Returns a Permutor that sorts a list appropriate to the given sort order.
     *
     * @return Permutor that sorts a list appropriate to the given sort order.
     */
    @NonNull
    public static Permutor<FeedItem> getPermutor(@NonNull SortOrder sortOrder) {

        Comparator<FeedItem> comparator = null;
        Permutor<FeedItem> permutor = null;

        if (SortOrder.GLOBAL_DEFAULT.equals(sortOrder)) {
            sortOrder = UserPreferences.getPrefGlobalSortedOrder();
        }

        switch (sortOrder) {
            case EPISODE_TITLE_A_Z:
                comparator = (f1, f2) -> itemTitle(f1).compareTo(itemTitle(f2));
                break;
            case EPISODE_TITLE_Z_A:
                comparator = (f1, f2) -> itemTitle(f2).compareTo(itemTitle(f1));
                break;
            case DATE_OLD_NEW:
                comparator = (f1, f2) -> pubDate(f1).compareTo(pubDate(f2));
                break;
            case DATE_NEW_OLD:
                comparator = (f1, f2) -> pubDate(f2).compareTo(pubDate(f1));
                break;
            case DURATION_SHORT_LONG:
                comparator = (f1, f2) -> Integer.compare(duration(f1), duration(f2));
                break;
            case DURATION_LONG_SHORT:
                comparator = (f1, f2) -> Integer.compare(duration(f2), duration(f1));
                break;
            case EPISODE_FILENAME_A_Z:
                comparator = (f1, f2) -> itemLink(f1).compareTo(itemLink(f2));
                break;
            case EPISODE_FILENAME_Z_A:
                comparator = (f1, f2) -> itemLink(f2).compareTo(itemLink(f1));
                break;
            case FEED_TITLE_A_Z:
                comparator = (f1, f2) -> feedTitle(f1).compareTo(feedTitle(f2));
                break;
            case FEED_TITLE_Z_A:
                comparator = (f1, f2) -> feedTitle(f2).compareTo(feedTitle(f1));
                break;
            case RANDOM:
                permutor = Collections::shuffle;
                break;
            case SMART_SHUFFLE_OLD_NEW:
                permutor = (queue) -> smartShuffle(queue, true);
                break;
            case SMART_SHUFFLE_NEW_OLD:
                permutor = (queue) -> smartShuffle(queue, false);
                break;
            case SIZE_SMALL_LARGE:
                comparator = (f1, f2) -> Long.compare(size(f1), size(f2));
                break;
            case SIZE_LARGE_SMALL:
                comparator = (f1, f2) -> Long.compare(size(f2), size(f1));
                break;
            case COMPLETION_DATE_NEW_OLD:
                comparator = (f1, f2) -> f2.getMedia().getLastPlayedTimeHistory()
                        .compareTo(f1.getMedia().getLastPlayedTimeHistory());
                break;
            default:
                throw new IllegalArgumentException("Permutor not implemented");
        }

        if (comparator != null) {
            final Comparator<FeedItem> comparator2 = comparator;
            permutor = (queue) -> Collections.sort(queue, comparator2);
        }
        return permutor;
    }

    // Null-safe accessors

    @NonNull
    private static Date pubDate(@Nullable FeedItem item) {
        return (item != null && item.getPubDate() != null) ? item.getPubDate() : new Date(0);
    }

    @NonNull
    private static String itemTitle(@Nullable FeedItem item) {
        return (item != null && item.getTitle() != null) ? item.getTitle().toLowerCase(Locale.getDefault()) : "";
    }

    private static int duration(@Nullable FeedItem item) {
        return (item != null && item.getMedia() != null) ? item.getMedia().getDuration() : 0;
    }

    private static long size(@Nullable FeedItem item) {
        return (item != null && item.getMedia() != null) ? item.getMedia().getSize() : 0;
    }

    @NonNull
    private static String itemLink(@Nullable FeedItem item) {
        return (item != null && item.getLink() != null)
                ? item.getLink().toLowerCase(Locale.getDefault()) : "";
    }

    @NonNull
    private static String feedTitle(@Nullable FeedItem item) {
        return (item != null && item.getFeed() != null && item.getFeed().getTitle() != null)
                ? item.getFeed().getTitle().toLowerCase(Locale.getDefault()) : "";
    }

    /**
     * Implements a reordering by pubdate that avoids consecutive episodes from the same feed in
     * the queue.
     *
     * A listener might want to hear episodes from any given feed in pubdate order, but would
     * prefer a more balanced ordering that avoids having to listen to clusters of consecutive
     * episodes from the same feed. This is what "Smart Shuffle" tries to accomplish.
     *
     * Assume the queue looks like this: `ABCDDEEEEEEEEEE`.
     * This method first starts with a queue of the final size, where each slot is empty (null).
     * It takes the podcast with most episodes (`E`) and places the episodes spread out in the queue: `EE_E_EE_E_EE_EE`.
     * The podcast with the second-most number of episodes (`D`) is then
     * placed spread-out in the *available* slots: `EE_EDEE_EDEE_EE`.
     * This continues, until we end up with: `EEBEDEECEDEEAEE`.
     *
     * Note that episodes aren't strictly ordered in terms of pubdate, but episodes of each feed are.
     *
     * @param queue A (modifiable) list of FeedItem elements to be reordered.
     * @param ascending {@code true} to use ascending pubdate in the reordering;
     *                  {@code false} for descending.
     */
    private static void smartShuffle(List<FeedItem> queue, boolean ascending) {
        // Divide FeedItems into lists by feed
        Map<Long, List<FeedItem>> map = new HashMap<>();
        for (FeedItem item : queue) {
            Long id = item.getFeedId();
            if (!map.containsKey(id)) {
                map.put(id, new ArrayList<>());
            }
            map.get(id).add(item);
        }

        // Sort each individual list by PubDate (ascending/descending)
        Comparator<FeedItem> itemComparator = ascending
                ? (f1, f2) -> f1.getPubDate().compareTo(f2.getPubDate())
                : (f1, f2) -> f2.getPubDate().compareTo(f1.getPubDate());
        List<List<FeedItem>> feeds = new ArrayList<>();
        for (Map.Entry<Long, List<FeedItem>> mapEntry : map.entrySet()) {
            Collections.sort(mapEntry.getValue(), itemComparator);
            feeds.add(mapEntry.getValue());
        }

        // Check user preference for spreading algorithm
        if (UserPreferences.isSmartShuffleSpreadEvenly()) {
            smartShuffleSpreadEvenly(queue, feeds);
        } else {
            smartShuffleLegacy(queue, feeds, ascending);
        }
    }

    /**
     * Spreads episodes from each podcast evenly throughout the queue.
     * This is the newer "Spread Evenly" algorithm.
     *
     * @param queue A (modifiable) list to be filled with reordered items
     * @param feeds List of feed lists sorted by size (largest first)
     */
    private static void smartShuffleSpreadEvenly(List<FeedItem> queue, List<List<FeedItem>> feeds) {
        ArrayList<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            queue.set(i, null);
            emptySlots.add(i);
        }

        // Starting with the largest feed, place items spread out through the empty slots in the queue
        Collections.sort(feeds, (f1, f2) -> Integer.compare(f2.size(), f1.size()));
        for (List<FeedItem> feedItems : feeds) {
            double spread = (double) emptySlots.size() / (feedItems.size() + 1);
            Iterator<Integer> emptySlotIterator = emptySlots.iterator();
            int skipped = 0;
            int placed = 0;
            while (emptySlotIterator.hasNext()) {
                int nextEmptySlot = emptySlotIterator.next();
                skipped++;
                if (skipped >= spread * (placed + 1)) {
                    if (queue.get(nextEmptySlot) != null) {
                        throw new RuntimeException("Slot to be placed in not empty");
                    }
                    queue.set(nextEmptySlot, feedItems.get(placed));
                    emptySlotIterator.remove();
                    placed++;
                    if (placed == feedItems.size()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Legacy smart shuffle algorithm that clusters episodes from podcasts with fewer episodes.
     * Podcasts with fewer than 5 queued episodes are placed first (ordered by the pubdate of
     * their first episode), followed by a spread algorithm for podcasts with more episodes.
     *
     * Given a queue like `ABCDDEEEEEEEEEE`:
     * Low-episode feeds (A) are placed first: `A`
     * The remaining Smart Shuffle algorithm spreads the multi-episode feeds out:
     * - Spread algorithm applied to B(2), C(3), D(2), E(5) results in interleaved episodes
     * - Final queue example: `A, B1, C1, D1, E1, B2, C2, D2, E2, C3, E3, E4, E5`
     *
     * This ensures that low-episode podcasts (typically shows the user wants to keep up with)
     * are always encountered first in the queue.
     *
     * For multi-episode podcasts, the algorithm calculates a spread factor to distribute
     * episodes throughout the remainder of the queue, avoiding long clusters of the same feed.
     *
     * @param queue A (modifiable) list to be filled with reordered items
     * @param feeds List of feed lists (will be sorted by first episode pubdate within this method)
     * @param ascending {@code true} to use ascending pubdate in the reordering; {@code false} for descending
     */
    private static void smartShuffleLegacy(List<FeedItem> queue, List<List<FeedItem>> feeds, boolean ascending) {
        // Separate low-episode feeds from multi-episode feeds
        List<List<FeedItem>> lowEpisodeFeeds = new ArrayList<>();
        List<List<FeedItem>> multiEpisodeFeeds = new ArrayList<>();
        for (List<FeedItem> feedItems : feeds) {
            if (feedItems.size() < 5) {
                lowEpisodeFeeds.add(feedItems);
            } else {
                multiEpisodeFeeds.add(feedItems);
            }
        }

        // Sort low-episode feeds by ascending/descending pubdate
        Collections.sort(lowEpisodeFeeds, ascending
                ? (f1, f2) -> f1.get(0).getPubDate().compareTo(f2.get(0).getPubDate())
                : (f1, f2) -> f2.get(0).getPubDate().compareTo(f1.get(0).getPubDate()));

        // Clear queue and add low-episode feeds first by alternating episodes
        queue.clear();
        int maxFeedSize = 0;
        for (List<FeedItem> feedItems : lowEpisodeFeeds) {
            maxFeedSize = Math.max(maxFeedSize, feedItems.size());
        }
        for (int i = 0; i < maxFeedSize; i++) {
            for (List<FeedItem> feedItems : lowEpisodeFeeds) {
                if (i < feedItems.size()) {
                    queue.add(feedItems.get(i));
                }
            }
        }

        // If there are no multi-episode feeds, we're done
        if (multiEpisodeFeeds.isEmpty()) {
            return;
        }

        // Calculate the spread for multi-episode feeds
        long spread = 0;
        for (List<FeedItem> feedItems : multiEpisodeFeeds) {
            if (spread == 0) {
                spread = feedItems.size();
            } else if (spread % feedItems.size() != 0) {
                spread *= feedItems.size();
            }
        }

        // Sort multi-episode feeds by ascending/descending pubdate of their first episode.
        Collections.sort(multiEpisodeFeeds, ascending
                ? (f1, f2) -> f1.get(0).getPubDate().compareTo(f2.get(0).getPubDate())
                : (f1, f2) -> f2.get(0).getPubDate().compareTo(f1.get(0).getPubDate()));

        // Spread each episode out
        Map<Long, List<FeedItem>> spreadItems = new HashMap<>();
        for (List<FeedItem> feedItems : multiEpisodeFeeds) {
            long thisSpread = spread / feedItems.size();
            if (thisSpread == 0) {
                thisSpread = 1;
            }
            // Starting from 0 ensures we front-load, so the queue starts with one episode from
            // each feed in the queue
            long itemSpread = 0;
            for (FeedItem feedItem : feedItems) {
                if (!spreadItems.containsKey(itemSpread)) {
                    spreadItems.put(itemSpread, new ArrayList<>());
                }
                spreadItems.get(itemSpread).add(feedItem);
                itemSpread += thisSpread;
            }
        }

        // Go through the spread items and add them to the queue (after single-episode feeds)
        List<Long> spreads = new ArrayList<>(spreadItems.keySet());
        Collections.sort(spreads);
        for (long itemSpread : spreads) {
            queue.addAll(spreadItems.get(itemSpread));
        }
    }
}
