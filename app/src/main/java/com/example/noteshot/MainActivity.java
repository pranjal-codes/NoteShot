package com.example.noteshot;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
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

    TextView noDataFoundTextView;
    List<FolderModel> folderName;
    RecyclerView folderRecyclerView;
    FolderAdapter folderRecyclerViewAdapter;
    GridLayoutManager gridLayoutManager;
    static FloatingActionButton fab;
    File parentFolder;
    File[] files;
    File folderParentPath;
    boolean isSortByName = false;
    static SearchView folderSearchView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Initializations for views and buttons
        noDataFoundTextView = findViewById(R.id.empty_recyclerView_message);
        fab = findViewById(R.id.fab);
        folderRecyclerView = (RecyclerView) findViewById(R.id.folder_recyclerview);
        folderParentPath = MainActivity.this.getFilesDir();
        parentFolder = new File(String.valueOf(MainActivity.this.getFilesDir()));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fetchExistingData();
        setRecyclerView();

//        Create Folder Dialog Appears by Clicking the Fab Button
        fab.setOnClickListener(view -> {
            createFolder();
            gridLayoutManager.scrollToPositionWithOffset(0, 0);
        });

        hideFabOnScroll();

    }


    public void setRecyclerView() {
        folderRecyclerViewAdapter = new FolderAdapter(MainActivity.this, folderName, noDataFoundTextView, folderParentPath);
        folderRecyclerView.setAdapter(folderRecyclerViewAdapter);

        int columns = getColumns();
        gridLayoutManager = new GridLayoutManager(MainActivity.this, columns + 1, RecyclerView.VERTICAL, false);
        folderRecyclerView.setLayoutManager(gridLayoutManager);
    }

    public int getColumns() {
//        It finds the appropriate no. of columns for the recycler View
        Display display = MainActivity.this.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;
        float dpWidth = outMetrics.widthPixels / density;
        return Math.round(dpWidth / 300);
    }

    public void fetchExistingData() {

        folderName = new ArrayList<>();
        files = parentFolder.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    folderName.add(new FolderModel(inFile.getName(), inFile.lastModified()));
                }
            }
        }
        Collections.sort(folderName, (o1, o2) -> o1.getFolderName().compareToIgnoreCase(o2.getFolderName()));
    }

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


    private void createFolder() {

//        My custom alert Dialog , nothing to worry about this
        CustomAlertDialogue alertDialogue = new CustomAlertDialogue(this);
        AlertDialog alertToShow = alertDialogue.getFolderCreateAlert();
        EditText input = alertDialogue.getAlertEditText();

        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

//            Getting the name of the folder to be created
            String value = input.getText().toString();
            File dir = new File(MainActivity.this.getFilesDir(), value.trim());
            if (!dir.exists()) {
                if (dir.mkdir()) {
                    alertToShow.dismiss();
                    folderName.add(new FolderModel(dir.getName(), dir.lastModified()));
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
                if (value.length() == 0) {
                    Toast.makeText(MainActivity.this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Folder with that name already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//         Inflate the menu; this adds items to the action bar if it is present.
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
//            setRecyclerView();
            folderRecyclerViewAdapter.notifyDataSetChanged();
            folderSearchView.onActionViewCollapsed();
            return true;
        });
        folderSearchView.setOnSearchClickListener(v -> {
//            setRecyclerView();
            folderRecyclerViewAdapter.notifyDataSetChanged();
            fab.hide();
        });


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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
        Collections.sort(folderName, (o1, o2) -> o2.getTimestamp().compareToIgnoreCase(o1.getTimestamp()));
        gridLayoutManager.scrollToPositionWithOffset(0, 0);
        isSortByName = false;
    }

    private void sortByName() {
        Collections.sort(folderName, (o1, o2) -> o1.getFolderName().compareToIgnoreCase(o2.getFolderName()));
        gridLayoutManager.scrollToPositionWithOffset(0, 0);
        isSortByName = true;
    }

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