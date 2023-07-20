package com.ozgurbaykal.hostmobile.view.customdialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.ozgurbaykal.hostmobile.R


class CustomDialogManager constructor(private val context: Context, private val dialogType: CustomDialogTypes, private val dialogTitle: String, private val dialogParagraph: String, private val dialogImage: Int, val optionalParameter: String? = null){

    private lateinit var dialog : Dialog

    //CUSTOM PARAMETERS
    private var simpleDialogButtonText: String? = null
    private var yesNoDialogYesButtonText: String? = null
    private var yesNoDialogNoButtonText: String? = null


    fun setSimpleDialogButtonText(title: String) {
        this.simpleDialogButtonText = title
    }

    fun setYesNoDialogYesButtonText(title: String) {
        this.yesNoDialogYesButtonText = title
    }

    fun setYesNoDialogNoButtonText(title: String) {
        this.yesNoDialogNoButtonText = title
    }

    //showDialog with cancel and button click functions
    fun showCustomDialog(onDialogCancel: () -> Unit, onSimpleDialogButtonClick: (() -> Unit)? = null, onYesButtonClick: (() -> Unit)? = null, onNoButtonClick: (() -> Unit)? = null){
        val builder = AlertDialog.Builder(context)

        val inflater = LayoutInflater.from(context)
        val view = when (dialogType) {
            CustomDialogTypes.SIMPLE_DIALOG -> inflater.inflate(R.layout.custom_dialog_simple, null)
            CustomDialogTypes.YES_OR_NO_DIALOG -> inflater.inflate(R.layout.custom_dialog_yes_no, null)
            else -> inflater.inflate(R.layout.custom_dialog_simple, null)
        }

        if(dialogType == CustomDialogTypes.SIMPLE_DIALOG){
            val dialogTitleTextView = view.findViewById<TextView>(R.id.simpleDialogTitle)
            val dialogParagraphTextView = view.findViewById<TextView>(R.id.simpleDialogParagraph)
            val dialogImageView = view.findViewById<ImageView>(R.id.simpleDialogImage)

            dialogTitleTextView.text = dialogTitle
            dialogParagraphTextView.text = dialogParagraph
            dialogImageView.setImageResource(dialogImage)

            val button = view.findViewById<Button>(R.id.simpleDialogClickButton)
            if(simpleDialogButtonText != null)
                button.text = simpleDialogButtonText

            button.setOnClickListener {
                if (onSimpleDialogButtonClick != null) {
                    onSimpleDialogButtonClick()
                }else{
                    cancelCustomDialog()
                }
            }
        }else if(dialogType == CustomDialogTypes.YES_OR_NO_DIALOG){
            val dialogTitleTextView = view.findViewById<TextView>(R.id.yesNoDialogTitle)
            val dialogParagraphTextView = view.findViewById<TextView>(R.id.yesNoDialogParagraph)
            val dialogImageView = view.findViewById<ImageView>(R.id.yesNoDialogImage)

            dialogTitleTextView.text = dialogTitle
            dialogParagraphTextView.text = dialogParagraph
            dialogImageView.setImageResource(dialogImage)

            val yesButton = view.findViewById<Button>(R.id.yesNoDialogYesClickButton)
            if(yesNoDialogYesButtonText != null)
                yesButton.text = yesNoDialogYesButtonText

            yesButton.setOnClickListener {
                if (onYesButtonClick != null) {
                    onYesButtonClick()
                }else{
                    cancelCustomDialog()
                }
            }

            val noButton = view.findViewById<Button>(R.id.yesNoDialogNoClickButton)
            if(yesNoDialogNoButtonText != null)
                noButton.text = yesNoDialogNoButtonText

            noButton.setOnClickListener {
                if (onNoButtonClick != null) {
                    onNoButtonClick()
                }else{
                    cancelCustomDialog()
                }
            }
        }


        builder.setView(view)
        dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setOnCancelListener {
            onDialogCancel()
        }


        dialog.show()
    }

    fun cancelCustomDialog(){
        if(dialog != null)
            dialog.cancel()
    }

    //SHOW DIALOGS WITHOUT LISTENERS
    fun showCustomDialog(){

        val builder = AlertDialog.Builder(context)

        val inflater = LayoutInflater.from(context)
        val view = when (dialogType) {
            CustomDialogTypes.SIMPLE_DIALOG -> inflater.inflate(R.layout.custom_dialog_simple, null)
            CustomDialogTypes.YES_OR_NO_DIALOG -> inflater.inflate(R.layout.custom_dialog_yes_no, null)
            else -> inflater.inflate(R.layout.custom_dialog_simple, null)
        }

        if(dialogType == CustomDialogTypes.SIMPLE_DIALOG){
            val dialogTitleTextView = view.findViewById<TextView>(R.id.simpleDialogTitle)
            val dialogParagraphTextView = view.findViewById<TextView>(R.id.simpleDialogParagraph)
            val dialogImageView = view.findViewById<ImageView>(R.id.simpleDialogImage)

            dialogTitleTextView.text = dialogTitle
            dialogParagraphTextView.text = dialogParagraph
            dialogImageView.setImageResource(dialogImage)

            val button = view.findViewById<Button>(R.id.simpleDialogClickButton)
            if(simpleDialogButtonText != null)
                button.text = simpleDialogButtonText

            button.setOnClickListener {
                cancelCustomDialog()
            }
        }else if(dialogType == CustomDialogTypes.YES_OR_NO_DIALOG){
            val dialogTitleTextView = view.findViewById<TextView>(R.id.yesNoDialogTitle)
            val dialogParagraphTextView = view.findViewById<TextView>(R.id.yesNoDialogParagraph)
            val dialogImageView = view.findViewById<ImageView>(R.id.yesNoDialogImage)

            dialogTitleTextView.text = dialogTitle
            dialogParagraphTextView.text = dialogParagraph
            dialogImageView.setImageResource(dialogImage)

            val yesButton = view.findViewById<Button>(R.id.yesNoDialogYesClickButton)
            if(yesNoDialogYesButtonText != null)
                yesButton.text = yesNoDialogYesButtonText

            yesButton.setOnClickListener {
                cancelCustomDialog()
            }

            val noButton = view.findViewById<Button>(R.id.yesNoDialogNoClickButton)
            if(yesNoDialogNoButtonText != null)
                noButton.text = yesNoDialogNoButtonText

            noButton.setOnClickListener {
                cancelCustomDialog()
            }
        }


        builder.setView(view)
        dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))



        dialog.show()
    }

}