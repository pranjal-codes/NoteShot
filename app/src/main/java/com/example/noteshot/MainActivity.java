package com.example.noteshot;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    //Data fields
    TextView noDataTextView;
    List<FolderModel> folderNameList;
    RecyclerView folderRecyclerView;
    FolderAdapter folderRecyclerViewAdapter;
    GridLayoutManager gridLayoutManager;
    static FloatingActionButton fab;
    File[] files;
    static File folderParentPath;
    boolean isSortByName = false;
    static SearchView folderSearchView;

    /**
     * First method that gets called when the app is launched. All instantiations and inflation here.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noDataTextView = findViewById(R.id.empty_recyclerView_message);
        fab = findViewById(R.id.fab);
        folderRecyclerView = findViewById(R.id.folder_recyclerview);
        folderParentPath = this.getExternalFilesDir(null);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        fetchExistingData();
        setRecyclerView();

        fab.setOnClickListener(view -> {
            createFolder();
            gridLayoutManager.scrollToPositionWithOffset(0, 0);
        });

        hideFabOnScroll();
    }

    /**
     * Setting up the recycler View
     */
    public void setRecyclerView() {
        folderRecyclerViewAdapter = new FolderAdapter(MainActivity.this, folderNameList, noDataTextView, folderParentPath);
        folderRecyclerView.setAdapter(folderRecyclerViewAdapter);

        // Getting the suitable number
        // of columns for the grid layout
        int columns = getColumns();
        gridLayoutManager = new GridLayoutManager(MainActivity.this, columns + 1, RecyclerView.VERTICAL, false);
        folderRecyclerView.setLayoutManager(gridLayoutManager);
    }

    /**
     * Calculates the suitable number of columns
     * for the grid layout
     *
     * @return number of columns
     */
    public int getColumns() {
        Display display = MainActivity.this.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;
        float dpWidth = outMetrics.widthPixels / density;
        return Math.round(dpWidth / 300);
    }

    /**
     * Loads the activity on start with stored folders
     */
    public void fetchExistingData() {

        // Fetches existing folders in the current directory
        // and adding them to the folderName List
        folderNameList = new ArrayList<>();
        files = folderParentPath.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    folderNameList.add(new FolderModel(inFile.getName(), inFile.lastModified()));
                }
            }
        }
        Collections.sort(folderNameList, (o1, o2) -> o1.getFolderName().compareToIgnoreCase(o2.getFolderName()));
    }

    /**
     * Hides the Floating Action Button on
     * scroll for better visibility
     */
    private void hideFabOnScroll() {
        folderRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && fab.getVisibility() == View.VISIBLE) {
                    fab.hide();
                } else if (dy < 0 && fab.getVisibility() != View.VISIBLE) {
                    fab.show();
                }
            }
        });
    }

    /**
     * Creates a new folder
     */
    private void createFolder() {
        // Custom Dialog box for the action
        CustomAlertDialogue alertDialogue = new CustomAlertDialogue(this);
        AlertDialog alertToShow = alertDialogue.getFolderCreateAlert();
        EditText input = alertDialogue.getAlertEditText();

        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            // Getting the folder's name
            String value = input.getText().toString();

            //Checks for illegal text input
            if (value.contains("\\") || value.contains(":") || value.contains("*") || value.contains("\"") ||
                    value.contains("<") || value.contains(">") || value.contains("|") || value.contains("/") || value.contains("?")) {

                Toast.makeText(MainActivity.this, "File name contains illegal characters.\n(\\:*?\"<>|/)", Toast.LENGTH_SHORT).show();

            } else {

                //Creating a new folder
                File dir = new File(MainActivity.this.getExternalFilesDir(null), value.trim());

                // Check if the folder exists
                if (!dir.exists()) {
                    if (dir.mkdir()) {
                        alertToShow.dismiss();
                        folderNameList.add(new FolderModel(dir.getName(), dir.lastModified()));
                        if (isSortByName) {
                            sortByName();
                        } else {
                            sortByLastModified();
                        }
                        folderRecyclerViewAdapter.notifyDataSetChanged();
                        setRecyclerView();
                        Toast.makeText(MainActivity.this, "New Folder Created", Toast.LENGTH_SHORT).show();
                    }
                } else {

                    // Handling edges cases
                    if (value.length() == 0) {
                        Toast.makeText(MainActivity.this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Folder with that name already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * Inflate the AppBar menu
     *
     * @param menu object
     * @return boolean value indicating success
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.search_folders);
        folderSearchView = (SearchView) menuItem.getActionView();
        folderSearchView.setMaxWidth(Integer.MAX_VALUE);
        folderSearchView.setQueryHint("Search Folders");
        folderSearchView.setImeOptions(EditorInfo.IME_ACTION_DONE);


        folderSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                folderRecyclerViewAdapter.getFilter().filter(newText);
                return true;
            }
        });

        folderSearchView.setOnCloseListener(() -> {
            fab.show();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(folderSearchView.getWindowToken(), 0);
            folderRecyclerViewAdapter.notifyDataSetChanged();
            folderSearchView.onActionViewCollapsed();
            return true;
        });
        folderSearchView.setOnSearchClickListener(v -> {
            folderRecyclerViewAdapter.notifyDataSetChanged();
            fab.hide();
        });


        return true;
    }

    /**
     * Handles AppBar menu option clicks
     *
     * @param item the clicked menu entry
     * @return status indicating success
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case (int) R.id.sort_folders_by_last_modified:
                sortByLastModified();
                break;
            case (int) R.id.sort_folders_by_name:
                sortByName();
                break;
        }

        folderRecyclerViewAdapter.notifyDataSetChanged();
        fab.show();
        return super.onOptionsItemSelected(item);
    }

    private void sortByLastModified() {
        Collections.sort(folderNameList, (o1, o2) -> o2.getTimestamp().compareToIgnoreCase(o1.getTimestamp()));
        gridLayoutManager.scrollToPositionWithOffset(0, 0);
        isSortByName = false;
    }

    private void sortByName() {
        Collections.sort(folderNameList, (o1, o2) -> o1.getFolderName().compareToIgnoreCase(o2.getFolderName()));
        gridLayoutManager.scrollToPositionWithOffset(0, 0);
        isSortByName = true;
    }

    /**
     * On back press, checks if search bar is open,
     * if yes, closes it
     */
    @Override
    public void onBackPressed() {
        if (!folderSearchView.isIconified()) {
            fab.show();
            folderRecyclerViewAdapter.notifyDataSetChanged();
            folderSearchView.onActionViewCollapsed();
        } else {
            super.onBackPressed();
        }
    }

}