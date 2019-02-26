/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import at.bitfire.icsdroid.BR
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.TitleColorBinding
import at.bitfire.icsdroid.db.LocalCalendar
import kotlinx.android.synthetic.main.title_color.view.*

class TitleColorFragment: Fragment() {

    lateinit var model: TitleColorModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        model = ViewModelProviders.of(requireActivity()).get(TitleColorModel::class.java)
        val binding = DataBindingUtil.inflate<TitleColorBinding>(inflater, R.layout.title_color, container, false)
        binding.lifecycleOwner = this
        binding.setVariable(BR.model, model)

        val v = binding.root
        v.color.setOnClickListener {
            val intent = Intent(requireActivity(), ColorPickerActivity::class.java)
            model.color.value?.let {
                intent.putExtra(ColorPickerActivity.EXTRA_COLOR, it)
            }
            startActivityForResult(intent, 0)

        }
        model.color.observe(this, Observer { color ->
            if (color != null)
                v.color.setColor(color)
        })

        return v
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        result?.let {
            model.color.value = it.getIntExtra(ColorPickerActivity.EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
        }
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
