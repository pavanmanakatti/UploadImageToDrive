package com.example.pavan.drive;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements ConnectionCallbacks,
        OnConnectionFailedListener {


    private static final String TAG = "drive-quickstart";
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private GoogleApiClient mGoogleApiClient;
    private Bitmap mBitmapToSave;
    private ImageView mCapturedImageView;
    Button b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        b = (Button) findViewById(R.id.button);
        mCapturedImageView = (ImageView) findViewById(R.id.capturePhotoImageView);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");
        new createFolder().execute();
        if(mBitmapToSave != null)
        saveFileToDrive();
    }


    private class createFolder extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            Query query = new Query.Builder()
                    .addFilter(Filters.and(Filters.eq(
                                    SearchableField.TITLE, "DigitalDiary"),
                            Filters.eq(SearchableField.TRASHED, false)))
                    .build();
            Drive.DriveApi.query(getGoogleApiClient(), query)
                    .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                        @Override
                        public void onResult(DriveApi.MetadataBufferResult result) {
                            if (!result.getStatus().isSuccess()) {
                                showMessage("Cannot create folder in the root.");
                            } else {
                                boolean isFound = false;
                                for (Metadata m : result.getMetadataBuffer()) {
                                    if (m.getTitle().equals("DigitalDiary")) {
                                        showMessage("Folder exists");
                                        isFound = true;
                                        break;
                                    }
                                }
                                if (!isFound) {
                                    showMessage("Folder not found; creating it.");
                                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                            .setTitle("DigitalDiary")
                                            .build();
                                    Drive.DriveApi.getRootFolder(getGoogleApiClient())
                                            .createFolder(getGoogleApiClient(), changeSet)
                                            .setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
                                                @Override
                                                public void onResult(DriveFolder.DriveFolderResult result) {
                                                    if (!result.getStatus().isSuccess()) {
                                                        showMessage("Error while trying to create the folder");
                                                    } else {
                                                        showMessage("Created a folder");
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    });
            return null;
        }
    }


        @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CAPTURE_IMAGE:
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    // Store the image data as a bitmap for writing later.
                    mBitmapToSave = (Bitmap) data.getExtras().get("data");
                    mCapturedImageView.setImageBitmap(mBitmapToSave);
                }
                break;
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.");
                    mBitmapToSave = null;
                    // Just start the camera again for another photo.
                    //startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),REQUEST_CODE_CAPTURE_IMAGE);
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }


    public void onBtnClicked(View view){
//        if (mBitmapToSave == null) {
            // This activity has no UI of its own. Just start the camera.
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    REQUEST_CODE_CAPTURE_IMAGE);
            return;
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient.connect();

    }

    /**
     * Create a new file and save it to Drive.
     */
    private void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = mBitmapToSave;
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveContentsResult>() {

                    @Override
                    public void onResult(DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        // Write the bitmap data from it.
                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                        try {
                            outputStream.write(bitmapStream.toByteArray());
                        } catch (IOException e1) {
                            Log.i(TAG, "Unable to write file contents.");
                        }
                        // Create the initial metadata - MIME type and title.
                        // Note that the user will be able to change the title later.
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("image/jpeg").setTitle("Android Photo.png").build();
                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    final ResultCallback<DriveFolderResult> callback = new ResultCallback<DriveFolderResult>() {
        @Override
        public void onResult(DriveFolderResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Error while trying to create the folder");
                return;
            }
            showMessage("Created a folder: " + result.getDriveFolder().getDriveId());
        }
    };


    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }




}
