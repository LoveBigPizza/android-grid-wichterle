package eu.inmite.android.gridwichterle.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RemoteViews;
import com.squareup.otto.Subscribe;
import eu.inmite.android.gridwichterle.R;
import eu.inmite.android.gridwichterle.bus.BusProvider;
import eu.inmite.android.gridwichterle.bus.CancelGridBus;
import eu.inmite.android.gridwichterle.bus.SettingsOnOffBus;
import eu.inmite.android.gridwichterle.core.Constants;
import eu.inmite.android.gridwichterle.core.NotificationReceiver;
import eu.inmite.android.gridwichterle.views.GridOverlay;
import eu.inmite.android.gridwichterle.views.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: Michal Matl (michal.matl@inmite.eu)
 * Date: 7/19/13
 * Time: 11:22 AM
 */
public class GridOverlayService extends Service {

	private GridOverlay mGridOverlay;
	private Settings mSettings;

	private static final int NOTIFICATION_ID = 1;

	@Override
	public void onCreate() {
		super.onCreate();

		BusProvider.getInstance().register(this);

	}

	private void showGrid() {
		Log.d(Constants.TAG, "GridOverlayService.showGrid()");
		final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
				PixelFormat.TRANSLUCENT
		);

		final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		mGridOverlay = new GridOverlay(this);

		wm.addView(mGridOverlay, lp);
	}

	private void removeGrid() {
		Log.d(Constants.TAG, "GridOverlayService.removeGrid()");

		try {
			if(mGridOverlay != null && mGridOverlay.getParent() != null) {
				final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
				wm.removeView(mGridOverlay);
			}
		} catch (Exception e) {
			Log.e(Constants.TAG, "Remove grid failed");
		}
	}

	private void removeNotification() {

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID);
	}

	private void restartGrid() {
		Log.d(Constants.TAG, "GridOverlayService.restartGrid()");
		removeGrid();
		showGrid();
	}

	private void showSettings() {
		Log.d(Constants.TAG, "GridOverlayService.showSettings()");
		final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				PixelFormat.TRANSLUCENT
		);


		if (mSettings == null) {
			mSettings = new Settings(this);
		}


		if(mSettings.getParent() == null) {
			final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.addView(mSettings, lp);
		}

	}

	private void showNotification() {
		Log.d(Constants.TAG, "GridOverlayService.showNotification()");

		// custom notification layout
		RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		contentView.setOnClickPendingIntent(R.id.btnSettings, getDeleteIntent());

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.ic_launcher)
				.setOngoing(false)
				.setAutoCancel(true)
				.setContent(contentView)
				.setContentIntent(getSettingIntent());
		Notification notification =  builder.getNotification();
		startForeground(NOTIFICATION_ID, notification);

		/*
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			final Notification.Builder builder = new Notification.Builder(this);
			builder.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle(getString(R.string.notification_title))
					.setContentText(getString(R.string.notification_desc))
					.setContentIntent(getSettingIntent())
					.setStyle(new Notification.BigTextStyle().bigText(getString(R.string.notification_desc)))
					.addAction(R.drawable.ic_remove, getString(R.string.cancel_grid), getDeleteIntent());

			startForeground(NOTIFICATION_ID, builder.build());
		} else {
			// custom notification layout for an older version
			RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
			contentView.setOnClickPendingIntent(R.id.btnSettings, getDeleteIntent());

			final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setSmallIcon(R.drawable.ic_launcher)
					.setOngoing(false)
					.setAutoCancel(true)
					.setContent(contentView)
					.setContentIntent(getSettingIntent());
			Notification notification =  builder.getNotification();
			startForeground(NOTIFICATION_ID, notification);
		}*/
	}

	private void removeSettings() {
		try {
			if(mSettings.getParent() != null) {
				final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
				wm.removeView(mSettings);
			}
		} catch (Exception e) {
			Log.e(Constants.TAG, "GridOverlayService.removeGrid() - failed", e);
		}

	}

	private PendingIntent getDeleteIntent() {
		Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);
		intent.setAction("notification_cancelled");
		return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private PendingIntent getSettingIntent() {
		Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);
		intent.setAction("notification_settings");
		return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private PendingIntent getSettingOffIntent() {
		Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);
		intent.setAction("notification_settings_off");
		return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	@Subscribe
	public void cancelGrid(CancelGridBus cancelGridBus) {
		Log.d(Constants.TAG, "GridOverlayService.CancelGrid()");

		removeGrid();
		removeSettings();
		stopForeground(true);
	}

	@Subscribe
	public void settingsAction(SettingsOnOffBus settingsOnOffBus) {

		if (settingsOnOffBus.getAction() == SettingsOnOffBus.ACTION_SETTINGS_ON) {
			removeGrid();
			showSettings();
		}

		if (settingsOnOffBus.getAction() == SettingsOnOffBus.ACTION_SETTINGS_OFF) {
			removeSettings();
			showGrid();
		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		showGrid();
		showNotification();
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		//we need restart grid when a screen rotates
		if (mGridOverlay.getParent() != null) {
			restartGrid();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		BusProvider.getInstance().unregister(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


}
