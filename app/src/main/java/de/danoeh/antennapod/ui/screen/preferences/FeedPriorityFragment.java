package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.preferences.screen.ReorderDialogAdapter;
import de.danoeh.antennapod.ui.preferences.screen.ReorderDialogItem;
import de.danoeh.antennapod.ui.preferences.screen.ReorderItemTouchCallback;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class FeedPriorityFragment extends Fragment {
    private static final String TAG = "FeedPriorityFragment";
    private final List<ReorderDialogItem> items = new ArrayList<>();
    private final Map<String, Feed> feeds = new HashMap<>();
    private Disposable disposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setId(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL));

        ReorderDialogAdapter adapter = new ReorderDialogAdapter(items);
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ReorderItemTouchCallback() {
            @Override
            protected boolean onItemMove(int fromPosition, int toPosition) {
                if (toPosition == 0) {
                    return false;
                }
                items.add(toPosition, items.remove(fromPosition));
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                savePriorities();
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setDragListener(itemTouchHelper::startDrag);
        recyclerView.setAdapter(adapter);

        disposable = Observable.fromCallable(DBReader::getFeedList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(feedList -> {
                    showFeeds(feedList);
                    adapter.notifyDataSetChanged();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
        return recyclerView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.feed_priorities_label);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void showFeeds(List<Feed> allFeeds) {
        List<Feed> feedList = new ArrayList<>();
        TreeSet<Integer> priorities = new TreeSet<>(Collections.reverseOrder());
        priorities.add(0);
        for (Feed feed : allFeeds) {
            if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                feedList.add(feed);
                priorities.add(feed.getPreferences().getPriority());
            }
        }
        if (priorities.first() < Integer.MAX_VALUE) {
            priorities.add(priorities.first() + 1);
        }
        Collections.sort(feedList, (f1, f2) -> String.CASE_INSENSITIVE_ORDER.compare(
                String.valueOf(f1.getTitle()), String.valueOf(f2.getTitle())));

        items.clear();
        feeds.clear();
        for (int priority : priorities) {
            items.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Header, String.valueOf(priority),
                    getString(R.string.feed_priority_header, priority)));
            for (Feed feed : feedList) {
                if (feed.getPreferences().getPriority() == priority) {
                    feeds.put(String.valueOf(feed.getId()), feed);
                    items.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section,
                            String.valueOf(feed.getId()), feed.getTitle()));
                }
            }
        }
    }

    private void savePriorities() {
        int priority = 0;
        for (ReorderDialogItem item : items) {
            if (item.getViewType() == ReorderDialogItem.ViewType.Header) {
                priority = Integer.parseInt(item.getTag());
                continue;
            }
            Feed feed = feeds.get(item.getTag());
            if (feed.getPreferences().getPriority() != priority) {
                feed.getPreferences().setPriority(priority);
                DBWriter.setFeedPreferences(feed.getPreferences());
            }
        }
    }
}
