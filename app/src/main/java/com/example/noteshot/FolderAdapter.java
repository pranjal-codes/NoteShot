package com.example.noteshot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> implements Filterable {

    //Data fields
    List<FolderModel> mFolderNames;
    List<FolderModel> wholeSearchList;
    List<FolderModel> currentSearchListItems;
    List<FolderModel> selectedListItems;
    Activity activity;
    TextView noDataTextView;
    MainViewModel mainViewModel;
    boolean multiSelect = false;
    boolean isSelectAll = false;
    File folderParentPath;
    Menu menuBar;

    /**
     * @param activity         context of ImageActivity
     * @param folderName       List of folders
     * @param noDataTextView   referring to xml file (shown on empty view)
     * @param folderParentPath path of parent directory
     */
    public FolderAdapter(Activity activity, List<FolderModel> folderName, TextView noDataTextView, File folderParentPath) {

        // Initializations
        this.activity = activity;
        this.noDataTextView = noDataTextView;
        this.folderParentPath = folderParentPath;
        mFolderNames = folderName;
        wholeSearchList = new ArrayList<>(folderName);
        currentSearchListItems = folderName;
        selectedListItems = new ArrayList<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ImageView imageView;
        public ImageView checkedItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.folder_icon);
            textView = itemView.findViewById(R.id.folder_name);
            checkedItem = itemView.findViewById(R.id.checked_item);
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View folderView = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_ui, parent, false);
        ViewHolder viewHolder = new ViewHolder(folderView);
        mainViewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(MainViewModel.class);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderModel folderModel = mFolderNames.get(position);
        TextView textView = holder.textView;
        ImageView imageView = holder.imageView;
        ImageView checkedItem = holder.checkedItem;

        textView.setText(folderModel.getFolderName());
        imageView.setImageResource(R.drawable.ic_folder);
        checkedItem.setImageResource(R.drawable.ic_check);


        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelect) {
                MainActivity.fab.hide();
                ActionMode.Callback callback = new ActionMode.Callback() {

                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(R.menu.menu_context, menu);
                        MainActivity.folderSearchView.clearFocus();
                        menuBar = menu;
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        multiSelect = true;
                        clickItem(holder);
                        mainViewModel.getText().observe((LifecycleOwner) activity,
                                s -> mode.setTitle(String.format("%s Selected", s)));
                        return true;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        int id = item.getItemId();
                        switch (id) {
                            case (int) R.id.delete_folder:
                                deleteDirectory(holder, mode);
                                break;

                            case (int) R.id.select_all:
                                selectAll(holder);
                                break;

                            case (int) R.id.rename_folder:
                                renameFolder(mode);
                                break;

                        }
                        if (mFolderNames.size() == 0) {
                            noDataTextView.setVisibility(View.VISIBLE);
                        }
                        return true;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        isSelectAll = false;
                        multiSelect = false;
                        mFolderNames.clear();
                        mFolderNames.addAll(wholeSearchList);
                        hideHighlight(holder);
                        selectedListItems.clear();
                        MainActivity.fab.show();
                        notifyDataSetChanged();
                        MainActivity.folderSearchView.onActionViewCollapsed();
                    }
                };
                ((AppCompatActivity) v.getContext()).startActionMode(callback);
            } else {
                clickItem(holder);
            }

            return true;
        });

        holder.itemView.setOnClickListener(v -> {

            // If multiSelect mode is enabled, we'll keep on selecting the items
            // else we open that folder
            if (multiSelect) {
                clickItem(holder);
            } else {
                Intent intent = new Intent(activity, ImageActivity.class);
                intent.putExtra("title", holder.textView.getText());
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
     * Check if all item are selected or not
     * and handles the Select All button accordingly
     *
     * @param holder referring to current ViewHolder
     */
    private void selectAll(ViewHolder holder) {
        if (selectedListItems.size() == mFolderNames.size()) {
            isSelectAll = false;
            hideHighlight(holder);
            selectedListItems.clear();
        } else {
            isSelectAll = true;
            selectedListItems.clear();
            selectedListItems.addAll(mFolderNames);
            showHighlight(holder);
            notifyDataSetChanged();
        }
        checkSelectList();
        mainViewModel.setText(String.valueOf(selectedListItems.size()));
        notifyDataSetChanged();
    }

    /**
     * Show Highlight on background of items on long click and selection
     */
    private void showHighlight(ViewHolder holder) {
        holder.checkedItem.setVisibility(View.VISIBLE);
        holder.itemView.setBackgroundColor(Color.parseColor("#394456"));
    }

    /**
     * Hide Highlight on background of items on long click and selection
     */
    private void hideHighlight(ViewHolder holder) {
        holder.checkedItem.setVisibility(View.GONE);
        holder.itemView.setBackgroundColor(Color.parseColor("#121212"));
    }

    /**
     * Deletes the selected folders
     *
     * @param holder current ViewHolder
     * @param mode   current Action mode
     */
    private void deleteDirectory(ViewHolder holder, ActionMode mode) {
        // Items to be deleted is now in tempDelete
        List<FolderModel> tempDelete = new ArrayList<>(selectedListItems);

        CustomAlertDialogue alertDialogue = new CustomAlertDialogue(activity);
        AlertDialog alertToShow = alertDialogue.getDeleteAlert();
        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            alertToShow.dismiss();
            selectedListItems.clear();
            selectedListItems.addAll(tempDelete);

            for (FolderModel s : selectedListItems) {
                File folder = new File(folderParentPath.getPath() + "/" + s.getFolderName());
                if (folder.exists()) {
                    String[] files = folder.list();
                    if (files != null) {
                        for (String item : files) {
                            File currentFile = new File(folder.getPath(), item);
                            currentFile.delete();
                        }
                    }
                    if (folder.delete()) {
                        Toast.makeText(activity, "Folder deleted", Toast.LENGTH_SHORT).show();
                    }
                    wholeSearchList.remove(s);
                }
            }
            selectedListItems.clear();
            mFolderNames.clear();
            mFolderNames.addAll(wholeSearchList);
            currentSearchListItems.clear();
            currentSearchListItems.addAll(wholeSearchList);
            notifyDataSetChanged();

            hideHighlight(holder);
            if (mFolderNames.size() == 0) {
                noDataTextView.setVisibility(View.VISIBLE);
            }
            //collapsing the searchBar after delete
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
        FolderModel selected = mFolderNames.get(holder.getAdapterPosition());
        if (holder.checkedItem.getVisibility() == View.GONE) {
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
        if (selectedListItems.size() != 1) {
            menuBar.findItem(R.id.rename_folder).setEnabled(false);
            menuBar.findItem(R.id.rename_folder).setIcon(R.drawable.faded_create);
        } else {
            menuBar.findItem(R.id.rename_folder).setEnabled(true);
            menuBar.findItem(R.id.rename_folder).setIcon(R.drawable.ic_baseline_create_24);
        }

        if (selectedListItems.size() == 0) {
            menuBar.findItem(R.id.delete_folder).setEnabled(false);
            menuBar.findItem(R.id.delete_folder).setIcon(R.drawable.faded_delete);
        } else {
            menuBar.findItem(R.id.delete_folder).setEnabled(true);
            menuBar.findItem(R.id.delete_folder).setIcon(R.drawable.ic_baseline_delete_24);
        }
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(MainActivity.folderSearchView.getWindowToken(), 0);
    }

    @Override
    public int getItemCount() {
        if (mFolderNames.size() == 0) {
            noDataTextView.setVisibility(View.VISIBLE);
        } else
            noDataTextView.setVisibility(View.GONE);
        return mFolderNames.size();
    }

    @Override
    public Filter getFilter() {
        return (Filter) filter;
    }

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<FolderModel> filteredSearchList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredSearchList.addAll(wholeSearchList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (FolderModel folder :
                        wholeSearchList) {
                    if (folder.getFolderName().toLowerCase().contains(filterPattern)) {
                        filteredSearchList.add(folder);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredSearchList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            currentSearchListItems.clear();
            currentSearchListItems.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    /**
     * Renames the folder
     *
     * @param mode current Action mode
     */
    private void renameFolder(ActionMode mode) {
        CustomAlertDialogue alertDialogue = new CustomAlertDialogue(activity);
        AlertDialog alertToShow = alertDialogue.getFolderRenameAlert(selectedListItems);
        EditText input = alertDialogue.getAlertEditText();

        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString();
            FolderModel toDelete = selectedListItems.get(0);
            String prevFolderName = selectedListItems.get(0).getFolderName();
            File prevFolder = new File(activity.getExternalFilesDir(null), prevFolderName);
            File renamedFolder = new File(activity.getExternalFilesDir(null), value);
            if (value.length() == 0) {
                Toast.makeText(activity, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (renamedFolder.exists()) {
                Toast.makeText(activity, "Folder with that name already exists", Toast.LENGTH_SHORT).show();
            } else {
                prevFolder.renameTo(renamedFolder);
                wholeSearchList.remove(toDelete);
                wholeSearchList.add(new FolderModel(renamedFolder.getName(), renamedFolder.lastModified()));
                currentSearchListItems.clear();
                currentSearchListItems.addAll(wholeSearchList);
                mFolderNames.clear();
                mFolderNames.addAll(wholeSearchList);
                notifyDataSetChanged();
                alertToShow.dismiss();
                mode.finish();
            }
        });
        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            mode.finish();
            alertToShow.dismiss();
        });

    }


}




