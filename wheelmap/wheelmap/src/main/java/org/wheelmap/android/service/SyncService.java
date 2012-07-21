/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.service;

import org.wheelmap.android.model.Extra;
import org.wheelmap.android.model.Extra.What;
import org.wheelmap.android.model.POIsProvider;
import org.wheelmap.android.net.ApiKeyExecutor;
import org.wheelmap.android.net.CategoriesExecutor;
import org.wheelmap.android.net.IExecutor;
import org.wheelmap.android.net.LocalesExecutor;
import org.wheelmap.android.net.NodeExecutor;
import org.wheelmap.android.net.NodeTypesExecutor;
import org.wheelmap.android.net.NodeUpdateOrNewExecutor;
import org.wheelmap.android.net.NodesExecutor;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * Background {@link Service} that synchronizes data living in
 * {@link POIsProvider}. Reads data from remote source
 */
public class SyncService extends IntentService {
	private static final String TAG = SyncService.class.getSimpleName();

	public static final int STATUS_RUNNING = 0x1;
	public static final int STATUS_ERROR = 0x2;
	public static final int STATUS_FINISHED = 0x3;

	public SyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG,
				"onHandleIntent(intent="
						+ intent.getIntExtra(Extra.WHAT, Extra.UNKNOWN) + ")");

		final ResultReceiver receiver = intent
				.getParcelableExtra(Extra.STATUS_RECEIVER);
		if (receiver != null)
			receiver.send(STATUS_RUNNING, Bundle.EMPTY);

		final Bundle arguments = intent.getExtras();

		int what = arguments.getInt(Extra.WHAT);
		IExecutor executor = null;
		switch (what) {
		case What.RETRIEVE_NODE:
			executor = new NodeExecutor(getContentResolver(), arguments);
			break;
		case What.RETRIEVE_NODES:
		case What.SEARCH_NODES:
		case What.SEARCH_NODES_IN_BOX:
			executor = new NodesExecutor(getContentResolver(), arguments);
			break;
		case What.RETRIEVE_LOCALES:
			executor = new LocalesExecutor(getContentResolver(), arguments);
			break;
		case What.RETRIEVE_CATEGORIES:
			executor = new CategoriesExecutor(getContentResolver(), arguments);
			break;
		case What.RETRIEVE_NODETYPES:
			executor = new NodeTypesExecutor(getContentResolver(), arguments);
			break;
		case What.UPDATE_SERVER:
			executor = new NodeUpdateOrNewExecutor(getApplicationContext(),
					getContentResolver());
			break;
		case What.RETRIEVE_APIKEY:
			executor = new ApiKeyExecutor(getApplicationContext(),
					getContentResolver(), arguments);
			break;
		default:
			return; // noop no instruction, no operation;
		}

		executor.prepareContent();
		try {
			executor.execute();
			executor.prepareDatabase();
		} catch (SyncServiceException e) {

			Log.e(TAG, "Problem while executing", e);
			if (receiver != null) {
				// Pass back error to surface listener
				final Bundle bundle = new Bundle();
				bundle.putParcelable(Extra.EXCEPTION, e);
				bundle.putInt(Extra.WHAT, what);
				receiver.send(STATUS_ERROR, bundle);
				return;
			}
		}

		// Log.d(TAG, "sync finished");
		if (receiver != null) {
			Log.d(TAG, "sending STATUS_FINISHED");
			final Bundle bundle = new Bundle();
			bundle.putInt(Extra.WHAT, what);
			receiver.send(STATUS_FINISHED, bundle);
		}
	}
}
