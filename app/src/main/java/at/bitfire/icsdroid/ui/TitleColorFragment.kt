/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import at.bitfire.icsdroid.R
import kotlinx.android.synthetic.main.calendar_title_color.view.*
import yuku.ambilwarna.AmbilWarnaDialog

class TitleColorFragment: Fragment(), TextWatcher {

    companion object {
        val ARG_URL = "url"
        val ARG_TITLE = "title"
        val ARG_COLOR = "color"
    }

    private var url: String? = null
    var title: String? = null
    var color = AddCalendarDetailsFragment.DEFAULT_COLOR

    private var listener: OnChangeListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val v = inflater.inflate(R.layout.calendar_title_color, container, false)

        if (inState == null) {
            url = arguments.getString(ARG_URL)
            title = arguments.getString(ARG_TITLE)
            color = arguments.getInt(ARG_COLOR)
        } else {
            url = inState.getString(ARG_URL)
            title = inState.getString(ARG_TITLE)
            color = inState.getInt(ARG_COLOR)
        }

        v.url.text = url

        v.title.setText(title)
        v.title.addTextChangedListener(this)

        v.color.setColor(color)
        v.color.setOnClickListener({ _ ->
            AmbilWarnaDialog(activity, color, object: AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(ambilWarnaDialog: AmbilWarnaDialog) {
                }

                override fun onOk(ambilWarnaDialog: AmbilWarnaDialog, newColor: Int) {
                    color = 0xFF000000.toInt() or newColor
                    v.color.setColor(color)
                    notifyListener()
                }
            }).show()
        })

        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_URL, url)
        outState.putString(ARG_TITLE, title)
        outState.putInt(ARG_COLOR, color)
    }


    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
        title = view!!.title.text.toString()
        notifyListener()
    }


    interface OnChangeListener {
        fun onChangeTitleColor(title: String?, color: Int)
    }

    fun setOnChangeListener(listener: OnChangeListener) {
        this.listener = listener
    }

    private fun notifyListener() {
        listener?.onChangeTitleColor(title, color)
    }
}
