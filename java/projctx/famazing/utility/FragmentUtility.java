package projctx.famazing.utility;

import android.support.v4.app.Fragment;

import projctx.famazing.ui.FamilyFragment;
import projctx.famazing.ui.HistoryFragment;
import projctx.famazing.ui.PositionsFragment;
import projctx.famazing.ui.TasksFragment;

/**
 * Class used by the navigation drawer to choose which fragment to show depending on the item pressed.
 */
public class FragmentUtility {

    /**
     * Return the correct fragment to show in relation to the item pressed in the navigation drawer.
     * @param drawerItemPosition : the index of the item pressed.
     * @return the corresponding fragment.
     */
    public static Fragment getFragment(int drawerItemPosition) {
        Fragment newFragment = null;
        switch (drawerItemPosition) {
            case 0:
                newFragment = new TasksFragment(); break;
            case 1:
                newFragment = new PositionsFragment(); break;
            case 2:
                newFragment = new HistoryFragment(); break;
            case 3:
                newFragment = new FamilyFragment(); break;
            default:
                break;
        }
        return newFragment;
    }
}
