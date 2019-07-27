package com.tyrata.tyrata.ui.services;

import android.widget.Button;

public class ReadingLabeling implements Runnable {
    private Button var;

    public ReadingLabeling(Button var) {
        this.var = var;
    }

    public void run() {
        var.setEnabled(false);
    }
}
