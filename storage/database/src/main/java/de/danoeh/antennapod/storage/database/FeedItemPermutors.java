package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
        final Permutor<FeedItem> permutor2 = permutor;
        return (queue) -> {
            Collections.sort(queue, (f1, f2) -> Integer.compare(priority(f2), priority(f1)));
            int start = 0;
            while (start < queue.size()) {
                int end = start + 1;
                while (end < queue.size() && priority(queue.get(end)) == priority(queue.get(start))) {
                    end++;
                }
                permutor2.reorder(queue.subList(start, end));
                start = end;
            }
        };
    }

    // Null-safe accessors

    private static int priority(@Nullable FeedItem item) {
        return (item != null && item.getFeed() != null && item.getFeed().getPreferences() != null)
                ? item.getFeed().getPreferences().getPriority() : 0;
    }

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
     * Each episode gets a target position from how far it is through its podcast's episodes:
     * the k-th of n episodes aims for k / (n + 1) of the way down the queue.
     * The queue is sorted by these positions, with ties broken by pubdate,
     * so we end up with: `EEEDEEABCEEDEEE`.
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
        Map<FeedItem, Double> position = new IdentityHashMap<>();
        for (List<FeedItem> feedItems : map.values()) {
            Collections.sort(feedItems, itemComparator);
            for (int i = 0; i < feedItems.size(); i++) {
                position.put(feedItems.get(i), (i + 1.0) / (feedItems.size() + 1));
            }
        }

        Collections.sort(queue, (f1, f2) -> {
            int result = Double.compare(position.get(f1), position.get(f2));
            return result != 0 ? result : itemComparator.compare(f1, f2);
        });
    }
}
