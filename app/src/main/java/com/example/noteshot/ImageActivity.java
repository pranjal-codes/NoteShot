package com.example.noteshot;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.example.noteshot.MainActivity.folderParentPath;

public class ImageActivity extends AppCompatActivity {

    public static final int CAMERA_REQUEST_CODE = 100;
    public static final int CAMERA_PERMISSION_CODE = 200;
    TextView noDataFoundTextView;
    List<ImageModel> imageList;
    RecyclerView imageRecyclerView;
    ImageAdapter imageRecyclerViewAdapter;
    LinearLayoutManager linearLayoutManager;
    static FloatingActionButton fab;
    File[] files;
    static File imagePath;
    static SearchView imageSearchView;
    File photoFile = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        Toolbar toolbar = findViewById(R.id.toolbar_image_ui);
        String message = getIntent().getStringExtra("title"); // Now, message has Drawer title
        toolbar.setTitle(message);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        noDataFoundTextView = findViewById(R.id.no_image_recyclerView_message);
        fab = findViewById(R.id.fab_take_photo);
        imageRecyclerView = findViewById(R.id.image_recyclerview);
        imagePath = new File(folderParentPath.getPath() + "/" + message);
        fetchExistingData();
        setRecyclerView();

//        Create Folder Dialog Appears by Clicking the Fab Button
        fab.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            }
        });

        hideFabOnScroll();
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            Timestamp timestamp;
            DateFormat fileNameFormat;
            photoFile = null;
            Date date = new Date();
            timestamp = new Timestamp(date.getTime());
            fileNameFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
            String fileName = "IMG_" + fileNameFormat.format(timestamp);
            try {
                photoFile = File.createTempFile(fileName, ".jpg", imagePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
            linearLayoutManager.scrollToPositionWithOffset(0, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                long ts = photoFile.lastModified();
                Timestamp tsp = new Timestamp(ts);
                imageList.add(0, new ImageModel(photoFile.getPath(), tsp));
                imageRecyclerViewAdapter.notifyDataSetChanged();
                takePhoto();
            }
        } else {
            photoFile.delete();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (permissions.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            }
        }
    }

    public void fetchExistingData() {
        imageList = new ArrayList<>();
        files = imagePath.listFiles();
        if (files != null) {
            for (File inFile : files) {
                DateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
                // you can change format of date
                String name = inFile.getName();
                String timeString = name.replaceAll("[^0-9]", "").substring(0, 14);
                Date date = null;
                try {
                    date = formatter.parse(timeString);
                    Timestamp timeStampDate = new Timestamp(date.getTime());
                    imageList.add(new ImageModel(inFile.getPath(), timeStampDate));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
        }
        Collections.sort(imageList, new Comparator<ImageModel>() {
            @Override
            public int compare(ImageModel o1, ImageModel o2) {
                return o1.getImageName().compareToIgnoreCase(o2.getImageName());
            }
        });
        Collections.reverse(imageList);
    }

    public void setRecyclerView() {
        imageRecyclerViewAdapter = new ImageAdapter(this, imageList, noDataFoundTextView, imagePath);
        imageRecyclerView.setAdapter(imageRecyclerViewAdapter);
        linearLayoutManager = new LinearLayoutManager(this);
        imageRecyclerView.setLayoutManager(linearLayoutManager);
    }

    private void hideFabOnScroll() {
        imageRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//         Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_image);
        imageSearchView = (SearchView) menuItem.getActionView();
        imageSearchView.setMaxWidth(Integer.MAX_VALUE);
        imageSearchView.setQueryHint("Search Images");
        imageSearchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        imageSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                imageRecyclerViewAdapter.getFilter().filter(newText);
                return true;
            }
        });

        imageSearchView.setOnCloseListener(() -> {
            fab.show();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(imageSearchView.getWindowToken(), 0);
            imageSearchView.onActionViewCollapsed();
            imageRecyclerViewAdapter.notifyDataSetChanged();
            return true;
        });
        imageSearchView.setOnSearchClickListener(v -> {
            imageRecyclerViewAdapter.notifyDataSetChanged();
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
        if (id == android.R.id.home) {
            onUpButtonPressed();
            return true;
        }
        imageRecyclerViewAdapter.notifyDataSetChanged();
        fab.show();
        return super.onOptionsItemSelected(item);
    }

    private void onUpButtonPressed() {
        if (!imageSearchView.isIconified()) {
            fab.show();
            imageRecyclerViewAdapter.notifyDataSetChanged();
            setRecyclerView();
            imageSearchView.onActionViewCollapsed();
        } else {
            finish();
        }
    }


    @Override
    public void onBackPressed() {
        if (!imageSearchView.isIconified()) {
            fab.show();
            imageRecyclerViewAdapter.notifyDataSetChanged();
            setRecyclerView();
            imageSearchView.onActionViewCollapsed();
        } else {
            super.onBackPressed();
        }
    }
}
