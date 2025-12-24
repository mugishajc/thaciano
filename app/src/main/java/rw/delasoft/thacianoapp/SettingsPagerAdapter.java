package rw.delasoft.thacianoapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SettingsPagerAdapter extends FragmentStateAdapter {

    public SettingsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new GeneralSettingsFragment();
            case 1:
                return new SecurityConfigFragment();
            default:
                return new GeneralSettingsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Two tabs
    }
}
