/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid;

import android.app.Service
import android.content.Intent

class SyncAdapterService: Service() {

    private val syncAdapter: SyncAdapter by lazy { SyncAdapter(this) }

    override fun onBind(intent: Intent?) = syncAdapter.syncAdapterBinder!!

}
