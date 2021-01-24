package com.example.noteshot;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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


    private final List<FolderModel> mFolderNames = MainActivity.folderName;
    List<FolderModel> wholeSearchList = new ArrayList<>(MainActivity.folderName);
    List<FolderModel> currentSearchListItems = MainActivity.folderName;
    List<FolderModel> selectedListItems = new ArrayList<>();
    Activity activity;
    TextView noDataFoundTextView;
    MainViewModel mainViewModel;
    boolean multiSelect = false;
    boolean isSelectAll = false;
    File folderParentPath;
    Menu menuBar;

    // Pass in the contact array into the constructor
    public FolderAdapter(Activity activity, TextView noDataFoundTextView, File folderParentPath) {
        this.activity = activity;
        this.noDataFoundTextView = noDataFoundTextView;
        this.folderParentPath = folderParentPath;
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
        textView.setText(folderModel.getFolderName());

        ImageView imageView = holder.imageView;
        imageView.setImageResource(R.drawable.ic_folder);

        ImageView checkedItem = holder.checkedItem;
        checkedItem.setImageResource(R.drawable.ic_check);


        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelect) {
                MainActivity.fab.hide();
                ActionMode.Callback callback = new ActionMode.Callback() {


                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(R.menu.menu_context, menu);
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
                                break;

                            case (int) R.id.rename_folder:
//                                to be implemented soon
                                break;

                        }
                        if (mFolderNames.size() == 0) {
                            noDataFoundTextView.setVisibility(View.VISIBLE);
                        }
                        return true;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        isSelectAll = false;
                        multiSelect = false;
                        hideHighlight(holder);
                        selectedListItems.clear();
                        MainActivity.fab.show();
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
                Toast.makeText(activity
                        , mFolderNames.get(holder.getAdapterPosition()).getFolderName()
                        , Toast.LENGTH_SHORT).show();
            }
        });

        if (isSelectAll) {
            showHighlight(holder);

        } else {
            hideHighlight(holder);
        }


    }

    //  Show/Hide Highlight on background of items on long click and selection
    private void showHighlight(ViewHolder holder) {
        holder.checkedItem.setVisibility(View.VISIBLE);
        holder.itemView.setBackgroundColor(Color.parseColor("#394456"));
    }

    private void hideHighlight(ViewHolder holder) {
        holder.checkedItem.setVisibility(View.GONE);
        holder.itemView.setBackgroundColor(Color.parseColor("#121212"));
    }

    private void deleteDirectory(ViewHolder holder, ActionMode mode) {
//        Items to be deleted is now in tempDelete
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
                    mFolderNames.remove(s);
                }
            }
            selectedListItems.clear();

            hideHighlight(holder);
            if (mFolderNames.size() == 0) {
                noDataFoundTextView.setVisibility(View.VISIBLE);
            }
            notifyDataSetChanged();
            //collapsing the searchBar after delete
            MainActivity.folderSearchView.onActionViewCollapsed();
            mode.finish();
        });
    }

    //    Behavior of items on click ( Highlighted/Not Highlighted and adding/removing from selectedListItems )
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

    //    Checks item in selectList ( for disabling/enabling the delete and rename buttons)
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
    }

    @Override
    public int getItemCount() {
        if (mFolderNames.size() == 0) {
            noDataFoundTextView.setVisibility(View.VISIBLE);
        } else
            noDataFoundTextView.setVisibility(View.GONE);
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

}
