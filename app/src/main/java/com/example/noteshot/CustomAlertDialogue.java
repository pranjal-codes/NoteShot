package com.example.noteshot;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

public class CustomAlertDialogue {
    Context context;
    EditText folderCreateEditText;

    CustomAlertDialogue(Context context) {
        this.context = context;
    }


    public AlertDialog getFolderCreateAlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("New Folder");
        final EditText input = new EditText(context);
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 16, 16, 16);
        input.setHighlightColor(Color.parseColor("#09a9ee"));
        input.setSingleLine();
        input.setLayoutParams(params);
        input.setText(R.string.new_folder);
        input.selectAll();
        input.requestFocus();
        folderCreateEditText = input;
        container.addView(input);
        alert.setView(container).setPositiveButton("OK", null).setNegativeButton("CANCEL", null);
        AlertDialog alertToShow = alert.create();
        alertToShow.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertToShow.show();
        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#03DAC6"));
        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#03DAC6"));
        alertToShow.setCanceledOnTouchOutside(false);
        return alertToShow;
    }

    public EditText getFolderCreateEditText() {
        return folderCreateEditText;
    }

    public AlertDialog getDeleteAlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Delete");
        alert.setMessage("Delete selected item?").setPositiveButton("OK", null).setNegativeButton("CANCEL", null);
        AlertDialog alertToShow = alert.create();
        alertToShow.show();
        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#03DAC6"));
        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#03DAC6"));
        alertToShow.setCanceledOnTouchOutside(false);
        return alertToShow;
    }
}
