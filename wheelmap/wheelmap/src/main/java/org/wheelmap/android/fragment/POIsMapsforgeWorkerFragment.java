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
package org.wheelmap.android.fragment;

import org.wheelmap.android.app.WheelmapApp;
import org.wheelmap.android.model.Extra;
import org.wheelmap.android.model.Extra.What;
import org.wheelmap.android.model.UserQueryHelper;
import org.wheelmap.android.model.UserQueryUpdateEvent;
import org.wheelmap.android.model.Wheelmap.POIs;
import org.wheelmap.android.service.SyncService;
import org.wheelmap.android.service.SyncServiceException;
import org.wheelmap.android.service.SyncServiceHelper;
import org.wheelmap.android.utils.DetachableResultReceiver;
import org.wheelmap.android.utils.DetachableResultReceiver.Receiver;

import android.app.Activity;
import android.app.SearchManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import de.akquinet.android.androlog.Log;

public class POIsMapsforgeWorkerFragment extends LocationFragment implements
		WorkerFragment, Receiver, LoaderCallbacks<Cursor> {
	public final static String TAG = POIsMapsforgeWorkerFragment.class
			.getSimpleName();
	private final static int LOADER_ID = 0;

	private DisplayFragment mDisplayFragment;
	private WorkerFragmentListener mListener;
	private DetachableResultReceiver mReceiver;

	private Cursor mCursor;

	boolean isSearchMode;
	private boolean mRefreshStatus;
	private Bus mBus;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof WorkerFragmentListener)
			mListener = (WorkerFragmentListener) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		mBus = WheelmapApp.getBus();
		mBus.register(this);

		mReceiver = new DetachableResultReceiver(new Handler());
		mReceiver.setReceiver(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mReceiver.clearReceiver();
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	/** {@inheritDoc} */
	public void onReceiveResult(int resultCode, Bundle resultData) {
		Log.d(TAG, "onReceiveResult resultCode = " + resultCode);
		switch (resultCode) {
		case SyncService.STATUS_RUNNING: {
			setRefreshStatus(true);
			break;
		}
		case SyncService.STATUS_FINISHED: {
			setRefreshStatus(false);
			break;
		}
		case SyncService.STATUS_ERROR: {
			setRefreshStatus(false);
			SyncServiceException e = resultData.getParcelable(Extra.EXCEPTION);
			if (mListener != null)
				mListener.onError(e);
			break;
		}
		}
	}

	protected void updateLocation() {
		if (mDisplayFragment != null)
			mDisplayFragment.setCurrentLocation(getLocation());
	}

	private void setRefreshStatus(boolean refreshState) {
		mRefreshStatus = refreshState;
		update();
	}

	private void update() {
		if (mDisplayFragment != null)
			mDisplayFragment.onUpdate(this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Log.d(TAG, "onCreateLoader");

		Uri uri = POIs.CONTENT_URI_RETRIEVED;
		return new CursorLoader(getActivity(), uri, POIs.PROJECTION,
				UserQueryHelper.getUserQuery(), null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.d(TAG,
				"cursorloader - switching cursors in adapter - cursor size = "
						+ cursor.getCount());
		mCursor = cursor;
		update();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		Log.d(TAG, "onLoaderReset - why is that?");
	}

	@Override
	public void requestSearch(Bundle bundle) {
		if (!bundle.containsKey(SearchManager.QUERY)
				&& !bundle.containsKey(Extra.CATEGORY)
				&& !bundle.containsKey(Extra.NODETYPE)
				&& !bundle.containsKey(Extra.WHEELCHAIR_STATE))
			return;

		if (bundle.getInt(Extra.CATEGORY) == Extra.UNKNOWN)
			bundle.remove(Extra.CATEGORY);

		if (!bundle.containsKey(Extra.WHAT)) {
			int what;
			if (bundle.containsKey(Extra.CATEGORY)
					|| bundle.containsKey(Extra.NODETYPE))
				what = What.RETRIEVE_NODES;
			else
				what = What.SEARCH_NODES_IN_BOX;

			bundle.putInt(Extra.WHAT, what);
		}

		bundle.putParcelable(Extra.STATUS_RECEIVER, mReceiver);
		SyncServiceHelper.executeRequest(getActivity(), bundle);
		setSearchModeInt(true);
	}

	@Override
	public void requestUpdate(Bundle bundle) {
		if (isSearchMode)
			return;

		bundle.putInt(Extra.WHAT, What.RETRIEVE_NODES);
		bundle.putParcelable(Extra.STATUS_RECEIVER, mReceiver);
		SyncServiceHelper.executeRequest(getActivity(), bundle);
	}

	@Override
	public void registerDisplayFragment(DisplayFragment fragment) {
		mDisplayFragment = fragment;
	}

	@Override
	public void unregisterDisplayFragment(DisplayFragment fragment) {
		mDisplayFragment = null;
	}

	@Override
	public Cursor getCursor(int id) {
		return mCursor;
	}

	@Override
	public boolean isRefreshing() {
		return mRefreshStatus;
	}

	@Override
	public boolean isSearchMode() {
		return isSearchMode;
	}

	private void setSearchModeInt(boolean searchMode) {
		Log.d(TAG, "setSearchMode: " + searchMode);
		isSearchMode = searchMode;
		if (mListener != null)
			mListener.onSearchModeChange(isSearchMode);
	}

	@Override
	public void setSearchMode(boolean searchMode) {
		Log.d(TAG, "setSearchMode: " + isSearchMode);
		isSearchMode = searchMode;
	}

	@Subscribe
	public void onUserQueryChange(UserQueryUpdateEvent e) {
		Log.d(TAG, "onUserQueryChanged: received event");
		getLoaderManager().restartLoader(LOADER_ID, null, this);
	}
}
