package com.example.anku.sunshine;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.anku.sunshine.data.WeatherContract.LocationEntry;
import com.example.anku.sunshine.data.WeatherContract.WeatherEntry;

public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();

    static final String DETAIL_URI = "URI";
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    private ShareActionProvider mShareActionProvider;
    private String mForecast;
    private Uri mUri;

    private static final int DETAIL_LOADER = 0;

    private static final String[] DETAIL_COLUMNS = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_LOCATION_SETTING
    };

    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_WEATHER_HUMIDITY = 5;
    public static final int COL_WEATHER_PRESSURE = 6;
    public static final int COL_WEATHER_WIND_SPEED = 7;
    public static final int COL_WEATHER_DEGREES = 8;
    public static final int COL_WEATHER_CONDITION_ID = 9;

    private ImageView mIconView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailActivityFragment.DETAIL_URI);
        }
        View rootView = inflater.inflate(R.layout.fragment_detail_start, container, false);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        return rootView;
    }

    private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
        menuItem.setIntent(createShareForecastIntent());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (getActivity() instanceof DetailActivity) {
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);
            finishCreatingMenu(menu);
        }
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged(String newLocation) {
        Uri uri = mUri;
        if (null != uri) {
            long date = WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (null != mUri) {
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        getView().setVisibility(View.INVISIBLE);
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            getView().setVisibility(View.VISIBLE);
            int weatherId = cursor.getInt(COL_WEATHER_CONDITION_ID);

            if (Utility.usingLocalGraphics(getActivity())) {
                mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
            } else {
                Glide.with(this)
                        .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                        .error(Utility.getArtResourceForWeatherCondition(weatherId))
                        .crossFade()
                        .into(mIconView);
            }
            long date = cursor.getLong(COL_WEATHER_DATE);
            String dateText = Utility.getFullFriendlyDayString(getActivity(), date);
            mDateView.setText(dateText);

            String description = cursor.getString(COL_WEATHER_DESC);
            mDescriptionView.setText(description);
            mIconView.setContentDescription(description);
            boolean isMetric = Utility.isMetric(getActivity());
            double high = cursor.getDouble(COL_WEATHER_MAX_TEMP);
            String highString = Utility.formatTemperature(getActivity(), high);
            mHighTempView.setText(highString);
            double low = cursor.getDouble(COL_WEATHER_MIN_TEMP);
            String lowString = Utility.formatTemperature(getActivity(), low);
            mLowTempView.setText(lowString);

            float humidity = cursor.getFloat(COL_WEATHER_HUMIDITY);
            mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));
            float windSpeedStr = cursor.getFloat(COL_WEATHER_WIND_SPEED);
            float windDirStr = cursor.getFloat(COL_WEATHER_DEGREES);
            mWindView.setText(Utility.getFormattedWind(getActivity(), windSpeedStr, windDirStr));
            float pressure = cursor.getFloat(COL_WEATHER_PRESSURE);
            mPressureView.setText(getActivity().getString(R.string.format_pressure, pressure));

            mForecast = String.format("%s - %s - %s/%s", dateText, description, high, low);
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);
        // We need to start the enter transition after the data has loaded
        if (activity instanceof DetailActivity) {
            activity.supportStartPostponedEnterTransition();
            if (null != toolbarView) {
                activity.setSupportActionBar(toolbarView);
                activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            if (null != toolbarView) {
                Menu menu = toolbarView.getMenu();
                if (null != menu) menu.clear();
                toolbarView.inflateMenu(R.menu.detailfragment);
                finishCreatingMenu(toolbarView.getMenu());
            }
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
