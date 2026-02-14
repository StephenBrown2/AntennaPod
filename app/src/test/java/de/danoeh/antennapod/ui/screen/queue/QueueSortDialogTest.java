package de.danoeh.antennapod.ui.screen.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;

/**
 * Quick test to ensure QueueSortDialog persists preferences on dismiss.
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class)
public class QueueSortDialogTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        UserPreferences.init(context);
        // reset prefs to defaults
        UserPreferences.setQueueKeepSorted(false);
        UserPreferences.setSmartShuffleSpreadEvenly(true);
        UserPreferences.setQueueKeepSortedOrder(SortOrder.DATE_NEW_OLD);
    }

    @Test
    public void onDismiss_persistsSpreadEvenlyAndSortOrder() throws Exception {
        // Enable keep sorted and set initial order to SMART_SHUFFLE_OLD_NEW so dialog picks it up
        UserPreferences.setQueueKeepSorted(true);
        UserPreferences.setQueueKeepSortedOrder(SortOrder.SMART_SHUFFLE_OLD_NEW);

        QueueFragment.QueueSortDialog dialog = new QueueFragment.QueueSortDialog();

        // Inflate binding manually and assign it to the dialog via reflection to avoid needing a FragmentManager
        LayoutInflater inflater = LayoutInflater.from(context);
        de.danoeh.antennapod.databinding.SortDialogBinding binding = de.danoeh.antennapod.databinding.SortDialogBinding.inflate(inflater);
        java.lang.reflect.Field bindingFieldSet = dialog.getClass().getSuperclass().getDeclaredField("viewBinding");
        bindingFieldSet.setAccessible(true);
        bindingFieldSet.set(dialog, binding);


        // Access the generated viewBinding via reflection and uncheck the Spread Evenly checkbox
        java.lang.reflect.Field bindingField = dialog.getClass().getSuperclass().getDeclaredField("viewBinding");
        bindingField.setAccessible(true);
        Object bindingObj = bindingField.get(dialog);
        java.lang.reflect.Field spreadField = bindingObj.getClass().getField("spreadEvenlyCheckbox");
        Object spreadCheckbox = spreadField.get(bindingObj);
        // call setChecked(false) via reflection
        spreadCheckbox.getClass().getMethod("setChecked", boolean.class).invoke(spreadCheckbox, false);

        // Change the sort order to RANDOM via reflection to simulate user picking another order
        java.lang.reflect.Field sortOrderField = dialog.getClass().getSuperclass().getDeclaredField("sortOrder");
        sortOrderField.setAccessible(true);
        sortOrderField.set(dialog, SortOrder.RANDOM);

        // Dismiss the dialog (pass a Dialog instance as DialogInterface)
        dialog.onDismiss(new Dialog(context));

        // Ensure preferences were persisted
        assertFalse(UserPreferences.isQueueKeepSorted());
        assertEquals(SortOrder.RANDOM, UserPreferences.getQueueKeepSortedOrder());
        // SpreadEvenly should not be persisted because RANDOM is not Smart Shuffle; it should remain previous value
        // but since we changed it to false before dismiss, and our code only persists spreadEvenly when smart shuffle
        // is selected, the preference should remain true (the default we set in setUp)
        assertTrue(UserPreferences.isSmartShuffleSpreadEvenly());
    }
}
