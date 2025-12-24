package rw.delasoft.thacianoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import rw.delasoft.thacianoapp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open Settings Activity
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_process_all) {
            // Open Processing Activity
            Intent intent = new Intent(MainActivity.this, ProcessingActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            // Open Settings Activity
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete_all) {
            // Show delete all confirmation dialog
            showDeleteAllConfirmationDialog();
            return true;
        } else if (id == R.id.action_support) {
            // Show support dialog
            showSupportDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteAllConfirmationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete All Phone Numbers");
        builder.setMessage("Are you sure you want to delete all phone numbers from the database?\n\nThis action cannot be undone!");
        builder.setPositiveButton("Yes, Delete All", (dialog, which) -> {
            // Show progress dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Deleting all phone numbers from database...");
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Perform deletion in background thread
            new Thread(() -> {
                DatabaseHelper databaseHelper = new DatabaseHelper(MainActivity.this);
                boolean success = databaseHelper.deleteAllPhoneNumbers();

                // Update UI on main thread
                runOnUiThread(() -> {
                    progressDialog.dismiss();

                    if (success) {
                        Toast.makeText(MainActivity.this, "All phone numbers deleted successfully", Toast.LENGTH_SHORT).show();
                        // Refresh the fragment by recreating the activity
                        recreate();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to delete phone numbers", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
        builder.setNegativeButton("Cancel", null);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }

    private void showSupportDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Call for Support");
        builder.setMessage("Need help? Send us an email at:\n\nrwandadevelopmentteam@gmail.com\n\nWe'll get back to you as soon as possible!");
        builder.setPositiveButton("Send Email", (dialog, which) -> {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rwandadevelopmentteam@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - Thaciano App");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe your issue here...");
            try {
                startActivity(Intent.createChooser(emailIntent, "Send email using..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(MainActivity.this, "No email clients installed.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}