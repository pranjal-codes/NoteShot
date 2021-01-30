package com.example.noteshot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {


    List<ImageModel> mImageList;
    List<ImageModel> wholeSearchList;
    List<ImageModel> selectedListItems;
    Activity activity;
    TextView noDataFoundTextView;
    MainViewModel mainViewModel;
    boolean multiSelect = false;
    boolean isSelectAll = false;
    File folderParentPath;
    Menu menuBar;

    public ImageAdapter(Activity activity, List<ImageModel> imageNameList, TextView noDataFoundTextView, File folderParentPath) {
        this.activity = activity;
        this.noDataFoundTextView = noDataFoundTextView;
        this.folderParentPath = folderParentPath;
        mImageList = imageNameList;
        wholeSearchList = new ArrayList<>(imageNameList);
        selectedListItems = new ArrayList<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView imageName;
        public TextView imageData;
        public ImageView checked;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_thumbnail);
            imageName = itemView.findViewById(R.id.image_name);
            imageData = itemView.findViewById(R.id.image_data);
            checked = itemView.findViewById(R.id.checked_image);
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View imageListView = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_ui, parent, false);
        ViewHolder viewHolder = new ViewHolder(imageListView);
        mainViewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(MainViewModel.class);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageModel imageModel = mImageList.get(position);
        TextView imageData = holder.imageData;
        TextView imageName = holder.imageName;
        ImageView imageView = holder.imageView;
        imageData.setText(imageModel.getImageDatetime());
        imageName.setText(imageModel.getImageName());
        Glide.with(activity).load("file://" + imageModel.getImageUri()).circleCrop().into(imageView);
        ImageView checkedItem = holder.checked;
        checkedItem.setImageResource(R.drawable.ic_check);

        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelect) {
                ImageActivity.fab.hide();
                ActionMode.Callback callback = new ActionMode.Callback() {

                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(R.menu.image_menu_context, menu);
                        menuBar = menu;
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        multiSelect = true;
                        clickItem(holder);
                        mainViewModel.getText().observe((LifecycleOwner) activity, s -> mode.setTitle(String.format("%s Selected", s)));
                        return true;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        int id = item.getItemId();
                        switch (id) {

                            case (int) R.id.delete_image:
                                deleteImages(holder, mode);
                                break;

                            case (int) R.id.select_all_image:
                                selectAll(holder);
                                break;

                            case (int) R.id.share_image:
                                shareSelectedImages();
                                break;
                        }
                        if (mImageList.size() == 0) {
                            noDataFoundTextView.setVisibility(View.VISIBLE);
                        }
                        return true;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        isSelectAll = false;
                        multiSelect = false;
                        mImageList.clear();
                        mImageList.addAll(wholeSearchList);
                        hideHighlight(holder);
                        selectedListItems.clear();
                        ImageActivity.fab.show();
                        notifyDataSetChanged();
                    }
                };
                ((AppCompatActivity) v.getContext()).startActionMode(callback);
            } else {
                clickItem(holder);
            }

            return true;
        });
        holder.itemView.setOnClickListener(v -> {
            if (multiSelect) {
                clickItem(holder);
            } else {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + imageModel.getImageUri()), "image/*");
                activity.startActivity(intent);
            }
        });

        if (isSelectAll) {
            showHighlight(holder);

        } else {
            hideHighlight(holder);
        }

    }

    /**
     * Broadcast to share selected images through intent
     */
    private void shareSelectedImages() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.");
        intent.setType("image/jpeg"); /* This example is sharing jpeg images. */
        ArrayList<Uri> files = new ArrayList<>();

        for (ImageModel path : selectedListItems /* List of the files you want to send */) {
            File file = new File(path.getImageUri());
            Uri uri = Uri.fromFile(file);
            files.add(uri);
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        activity.startActivity(intent);

    }

    /**
     * Select/Deselect all the images
     *
     * @param holder current holder
     */
    private void selectAll(ViewHolder holder) {
        if (selectedListItems.size() == mImageList.size()) {
            isSelectAll = false;
            hideHighlight(holder);
            selectedListItems.clear();
        } else {
            isSelectAll = true;
            selectedListItems.clear();
            selectedListItems.addAll(mImageList);
            showHighlight(holder);
            notifyDataSetChanged();
        }
        checkSelectList();
        mainViewModel.setText(String.valueOf(selectedListItems.size()));
        notifyDataSetChanged();
    }

    /**
     * Show Highlight on background of items on long click and selection
     *
     * @param holder current holder
     */
    private void showHighlight(ViewHolder holder) {
        holder.checked.setVisibility(View.VISIBLE);
        holder.itemView.setBackgroundColor(Color.parseColor("#394456"));
    }

    /**
     * Hide Highlight on background of items on long click and selection
     *
     * @param holder current holder
     */
    private void hideHighlight(ViewHolder holder) {
        holder.checked.setVisibility(View.GONE);
        holder.itemView.setBackgroundColor(Color.parseColor("#121212"));
    }

    /**
     * Deletes the selected images
     *
     * @param holder current ViewHolder
     * @param mode   current Action mode
     */
    private void deleteImages(ViewHolder holder, ActionMode mode) {
//        Items to be deleted is now in tempDelete
        List<ImageModel> tempDelete = new ArrayList<>(selectedListItems);

        CustomAlertDialogue alertDialogue = new CustomAlertDialogue(activity);
        AlertDialog alertToShow = alertDialogue.getDeleteAlert();
        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            alertToShow.dismiss();
            selectedListItems.clear();
            selectedListItems.addAll(tempDelete);
            for (ImageModel s : selectedListItems) {

                File image = new File(s.getImageUri());
                if (image.exists()) {
                    if (image.delete()) {
                        Toast.makeText(activity, "Image deleted", Toast.LENGTH_SHORT).show();
                    }
                    wholeSearchList.remove(s);
                }
            }
            selectedListItems.clear();
            mImageList.clear();
            mImageList.addAll(wholeSearchList);
            notifyDataSetChanged();

            hideHighlight(holder);
            if (mImageList.size() == 0) {
                noDataFoundTextView.setVisibility(View.VISIBLE);
            }
            mode.finish();
        });
        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            mode.finish();
            alertToShow.dismiss();
        });
    }

    /**
     * Behavior of items on click ( Highlighted/Not Highlighted and adding/removing from selectedListItems )
     *
     * @param holder current ViewHolder
     */
    private void clickItem(ViewHolder holder) {
        ImageModel selected = mImageList.get(holder.getAdapterPosition());
        if (holder.checked.getVisibility() == View.GONE) {
            showHighlight(holder);
            selectedListItems.add(selected);
        } else {
            hideHighlight(holder);
            selectedListItems.remove(selected);
        }
        checkSelectList();
        mainViewModel.setText(String.valueOf(selectedListItems.size()));
    }

    /**
     * Checks item in selectList ( for disabling/enabling the delete and rename buttons)
     */
    private void checkSelectList() {

        if (selectedListItems.size() == 0) {
            menuBar.findItem(R.id.share_image).setEnabled(false);
            menuBar.findItem(R.id.share_image).setIcon(R.drawable.faded_share);
            menuBar.findItem(R.id.delete_image).setEnabled(false);
            menuBar.findItem(R.id.delete_image).setIcon(R.drawable.faded_delete);
        } else {
            menuBar.findItem(R.id.share_image).setEnabled(true);
            menuBar.findItem(R.id.share_image).setIcon(R.drawable.ic_baseline_share_24);
            menuBar.findItem(R.id.delete_image).setEnabled(true);
            menuBar.findItem(R.id.delete_image).setIcon(R.drawable.ic_baseline_delete_24);
        }

    }

    @Override
    public int getItemCount() {
        if (mImageList.size() == 0) {
            noDataFoundTextView.setVisibility(View.VISIBLE);
        } else
            noDataFoundTextView.setVisibility(View.GONE);
        return mImageList.size();
    }


}




