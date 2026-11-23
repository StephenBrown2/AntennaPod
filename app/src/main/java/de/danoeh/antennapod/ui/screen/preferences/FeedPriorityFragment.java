package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.FeedPriorityHeaderBinding;
import de.danoeh.antennapod.databinding.FeedPriorityItemBinding;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.common.ImagePlaceholder;
import de.danoeh.antennapod.ui.common.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.ui.preferences.screen.ReorderItemTouchCallback;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class FeedPriorityFragment extends Fragment {
    private static final String TAG = "FeedPriorityFragment";
    private final List<Feed> feedList = new ArrayList<>();
    private final List<Object> items = new ArrayList<>();
    private final Map<Integer, Integer> counts = new HashMap<>();
    private final Set<Integer> collapsed = new HashSet<>();
    private final Set<Long> selected = new HashSet<>();
    private final PriorityAdapter adapter = new PriorityAdapter();
    private ItemTouchHelper itemTouchHelper;
    private ActionMode actionMode;
    private RecyclerView.ViewHolder draggedHolder;
    private Disposable disposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setId(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL));

        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        itemTouchHelper = new ItemTouchHelper(new ReorderItemTouchCallback() {
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
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                draggedHolder = actionState == ItemTouchHelper.ACTION_STATE_DRAG ? viewHolder : null;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (draggedHolder != null && draggedHolder != viewHolder) {
                    return;
                }
                savePriorities();
                recyclerView.post(() -> {
                    if (draggedHolder == null) {
                        buildItems();
                    }
                });
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.feed_priority, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.multi_select) {
                    startSelectMode();
                } else if (menuItem.getItemId() == R.id.expand_all) {
                    collapsed.clear();
                    buildItems();
                } else if (menuItem.getItemId() == R.id.collapse_all) {
                    collapsed.addAll(counts.keySet());
                    buildItems();
                } else {
                    return false;
                }
                return true;
            }
        }, getViewLifecycleOwner());

        disposable = Observable.fromCallable(DBReader::getFeedList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showFeeds, error -> Log.e(TAG, Log.getStackTraceString(error)));
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
        if (actionMode != null) {
            actionMode.finish();
        }
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void showFeeds(List<Feed> allFeeds) {
        feedList.clear();
        for (Feed feed : allFeeds) {
            if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                feedList.add(feed);
            }
        }
        Collections.sort(feedList, (f1, f2) -> String.CASE_INSENSITIVE_ORDER.compare(
                String.valueOf(f1.getTitle()), String.valueOf(f2.getTitle())));
        buildItems();
    }

    private void buildItems() {
        TreeSet<Integer> priorities = new TreeSet<>(Collections.reverseOrder());
        priorities.add(0);
        counts.clear();
        for (Feed feed : feedList) {
            int priority = feed.getPreferences().getPriority();
            priorities.add(priority);
            counts.put(priority, counts.containsKey(priority) ? counts.get(priority) + 1 : 1);
        }
        if (priorities.first() < Integer.MAX_VALUE) {
            priorities.add(priorities.first() + 1);
        }

        items.clear();
        for (int priority : priorities) {
            items.add(priority);
            if (collapsed.contains(priority)) {
                continue;
            }
            for (Feed feed : feedList) {
                if (feed.getPreferences().getPriority() == priority) {
                    items.add(feed);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void savePriorities() {
        int priority = 0;
        for (Object item : items) {
            if (item instanceof Integer) {
                priority = (Integer) item;
            } else {
                setPriority((Feed) item, priority);
            }
        }
    }

    private void setPriority(Feed feed, int priority) {
        if (feed.getPreferences().getPriority() != priority) {
            feed.getPreferences().setPriority(priority);
            DBWriter.setFeedPreferences(feed.getPreferences());
        }
    }

    private void showPriorityDialog(int initial, Consumer<Integer> onSet) {
        EditTextDialogBinding binding = EditTextDialogBinding.inflate(getLayoutInflater());
        binding.textInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        binding.textInput.setText(String.valueOf(initial));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_feed_priority)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int priority;
                    try {
                        priority = Integer.parseInt(binding.textInput.getText().toString());
                    } catch (NumberFormatException e) {
                        return;
                    }
                    onSet.accept(priority);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void editGroup(int from) {
        showPriorityDialog(from, to -> {
            if (to == from) {
                return;
            }
            Runnable move = () -> {
                for (Feed feed : feedList) {
                    if (feed.getPreferences().getPriority() == from) {
                        setPriority(feed, to);
                    }
                }
                collapsed.remove(from);
                buildItems();
            };
            if (!counts.containsKey(to)) {
                move.run();
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.feed_priority_edit)
                    .setMessage(getString(R.string.feed_priority_merge_confirm, to, from))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> move.run())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private void editFeeds(List<Feed> feeds) {
        showPriorityDialog(feeds.get(0).getPreferences().getPriority(), to -> {
            for (Feed feed : feeds) {
                setPriority(feed, to);
            }
            if (actionMode != null) {
                actionMode.finish();
            }
            buildItems();
        });
    }

    private void startSelectMode() {
        if (actionMode != null) {
            return;
        }
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (actionMode != null) {
                    actionMode.finish();
                }
            }
        };
        actionMode = requireActivity().startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.feed_priority_select, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() != R.id.edit_priority) {
                    return false;
                }
                List<Feed> feeds = new ArrayList<>();
                for (Feed feed : feedList) {
                    if (selected.contains(feed.getId())) {
                        feeds.add(feed);
                    }
                }
                if (!feeds.isEmpty()) {
                    editFeeds(feeds);
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                selected.clear();
                backPressedCallback.remove();
                adapter.notifyDataSetChanged();
            }
        });
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
        updateSelectTitle();
        adapter.notifyDataSetChanged();
    }

    private void updateSelectTitle() {
        if (actionMode != null) {
            actionMode.setTitle(getResources().getQuantityString(R.plurals.num_selected_label,
                    selected.size(), selected.size(), feedList.size()));
        }
    }

    private class PriorityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int HEADER_VIEW = 0;
        private static final int ITEM_VIEW = 1;

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof Integer ? HEADER_VIEW : ITEM_VIEW;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == HEADER_VIEW) {
                return new HeaderHolder(FeedPriorityHeaderBinding.inflate(inflater, parent, false));
            }
            return new ItemHolder(FeedPriorityItemBinding.inflate(inflater, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderHolder) {
                bindHeader(((HeaderHolder) holder).binding, (Integer) items.get(position));
            } else {
                bindItem((ItemHolder) holder, (Feed) items.get(position));
            }
        }

        private void bindHeader(FeedPriorityHeaderBinding binding, int priority) {
            boolean used = counts.containsKey(priority);
            binding.headerLabel.setText(getString(R.string.feed_priority_header, priority));
            binding.countLabel.setText(used
                    ? getResources().getQuantityString(R.plurals.num_podcasts, counts.get(priority),
                            counts.get(priority))
                    : getString(R.string.feed_priority_unused));
            binding.getRoot().setAlpha(used ? 1f : 0.5f);
            binding.editButton.setVisibility(used ? View.VISIBLE : View.GONE);
            binding.chevron.setVisibility(used ? View.VISIBLE : View.GONE);
            binding.chevron.setRotation(collapsed.contains(priority) ? 0 : 180);
            binding.editButton.setOnClickListener(v -> editGroup(priority));
            binding.getRoot().setOnClickListener(v -> {
                if (!collapsed.remove(priority)) {
                    collapsed.add(priority);
                }
                buildItems();
            });
            binding.getRoot().setClickable(used);
        }

        private void bindItem(ItemHolder holder, Feed feed) {
            FeedPriorityItemBinding binding = holder.binding;
            binding.titleLabel.setText(feed.getTitle());
            binding.authorLabel.setText(feed.getAuthor());
            binding.authorLabel.setVisibility(TextUtils.isEmpty(feed.getAuthor()) ? View.GONE : View.VISIBLE);
            float radius = 4 * getResources().getDisplayMetrics().density;
            Glide.with(FeedPriorityFragment.this)
                    .load(feed.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(ImagePlaceholder.getDrawable(requireContext(), radius))
                            .error(ImagePlaceholder.getDrawable(requireContext(), radius))
                            .transform(new FitCenter(), new RoundedCorners((int) radius))
                            .dontAnimate())
                    .into(binding.coverImage);

            boolean selectMode = actionMode != null;
            binding.dragHandle.setVisibility(selectMode ? View.GONE : View.VISIBLE);
            binding.checkBox.setVisibility(selectMode ? View.VISIBLE : View.GONE);
            binding.checkBox.setChecked(selected.contains(feed.getId()));
            binding.dragHandle.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder);
                }
                return true;
            });
            binding.getRoot().setOnClickListener(v -> {
                if (actionMode == null) {
                    editFeeds(Collections.singletonList(feed));
                    return;
                }
                if (!selected.remove(feed.getId())) {
                    selected.add(feed.getId());
                }
                updateSelectTitle();
                notifyItemChanged(holder.getBindingAdapterPosition());
            });
            binding.getRoot().setOnLongClickListener(v -> {
                if (actionMode == null) {
                    selected.add(feed.getId());
                    startSelectMode();
                }
                return true;
            });
        }
    }

    private static class HeaderHolder extends RecyclerView.ViewHolder {
        final FeedPriorityHeaderBinding binding;

        HeaderHolder(FeedPriorityHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class ItemHolder extends RecyclerView.ViewHolder {
        final FeedPriorityItemBinding binding;

        ItemHolder(FeedPriorityItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
