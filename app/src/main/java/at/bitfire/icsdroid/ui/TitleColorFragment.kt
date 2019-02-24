/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import at.bitfire.icsdroid.BR
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.TitleColorBinding
import kotlinx.android.synthetic.main.title_color.view.*
import yuku.ambilwarna.AmbilWarnaDialog

class TitleColorFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val model = ViewModelProviders.of(requireActivity()).get(TitleColorModel::class.java)
        val binding = DataBindingUtil.inflate<TitleColorBinding>(inflater, R.layout.title_color, container, false)
        binding.lifecycleOwner = this
        binding.setVariable(BR.model, model)

        val v = binding.root
        model.color.value?.let { color ->
            v.color.setOnClickListener {
                AmbilWarnaDialog(activity, color, object: AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(ambilWarnaDialog: AmbilWarnaDialog) {
                    }

                    override fun onOk(ambilWarnaDialog: AmbilWarnaDialog, newColor: Int) {
                        model.color.value = 0xFF000000.toInt() or newColor
                        v.color.setColor(color)
                    }
                }).show()
            }
        }
        return v
    }


    class TitleColorModel: ViewModel() {
        var url = MutableLiveData<String>()

        var originalTitle: String? = null
        val title = MutableLiveData<String>()

        var originalColor: Int? = null
        val color = MutableLiveData<Int>()

        fun dirty() =
                originalTitle != title.value ||
                originalColor != color.value
    }

}
