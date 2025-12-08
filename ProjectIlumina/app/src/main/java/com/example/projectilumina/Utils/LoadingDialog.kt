package com.example.projectilumina.Utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.projectilumina.R

class LoadingDialog(private val context: Context) {

    private var dialog: Dialog? = null

    fun show() {
        if (dialog != null && dialog!!.isShowing) return
        val view = LayoutInflater.from(context).inflate(R.layout.loading, null)

        dialog = Dialog(
            context,
            android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen
        ).apply {

            setContentView(view)
            setCancelable(false)

            window?.setBackgroundDrawableResource(android.R.color.transparent)

            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            show()
        }
    }

    fun hide() {
        dialog?.dismiss()
    }
}
