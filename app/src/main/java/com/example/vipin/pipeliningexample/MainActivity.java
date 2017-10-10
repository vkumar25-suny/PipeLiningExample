package com.example.vipin.pipeliningexample;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.myapache.http.Header;
import org.myapache.http.HttpEntity;
import org.myapache.http.HttpHost;
import org.myapache.http.HttpRequest;
import org.myapache.http.HttpResponse;
import org.myapache.http.client.methods.HttpGet;
import org.myapache.http.concurrent.FutureCallback;
import org.myapache.http.impl.nio.client.CloseableHttpPipeliningClient;
import org.myapache.http.impl.nio.client.HttpAsyncClients;
import org.myapache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.myapache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.myapache.http.nio.reactor.ConnectingIOReactor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "--VIPIN--";

    private Spinner serverSelector, folderNameSelector, pipelininglevelSelector, filecountSelector;
    private Button btnSubmit;
    private TextView textView1, textView2;

    private File myExternalFile = null;

    private String filename = null;

    private ProgressDialog progressDialog;

    private String serverName = null;
    private String serverDirName = null;

    private int pipeliningLevel = 1;
    private int fileCount = 50;
    private int portNumber = 80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // API 23 requires this permission to start download
        String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE"};
        int permsRequestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(perms, permsRequestCode);
        }

        addListenerOnSpinnerItemSelection();

        textView2 = (TextView) findViewById(R.id.edittext1);
        textView2 = (TextView) findViewById(R.id.edittext2);

        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                progressDialog = ProgressDialog.show(MainActivity.this,
                        "", "Loading..");
                new Thread() {
                    public void run() {

                        serverName = String.valueOf(serverSelector.getSelectedItem());
                        serverDirName = String.valueOf(folderNameSelector.getSelectedItem());

                        pipeliningLevel = Integer.valueOf(String.valueOf(pipelininglevelSelector.getSelectedItem()));
                        fileCount = Integer.valueOf(String.valueOf(filecountSelector.getSelectedItem()));

                        Log.d(TAG, "TEST ::: " + serverName + " " + serverDirName + " " + pipeliningLevel + " " + fileCount);

                        myExternalFile = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS), serverDirName);

                        myExternalFile.mkdir();

                        startPipeliningCode();

                        messageHandler.sendEmptyMessage(0);
                    }
                }.start();
            }
        });
    }

    /**
     * A Simple Handler
     */
    private Handler messageHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            progressDialog.dismiss();
        }
    };

    /**
     * To check if external storage is read only
     *
     * @return boolean value
     */
    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    /**
     * To check if external storage is available
     *
     * @return boolean value
     */
    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    /**
     * Add the listeners to all the spinners.
     */
    private void addListenerOnSpinnerItemSelection() {
        serverSelector = (Spinner) findViewById(R.id.servers);
        folderNameSelector = (Spinner) findViewById(R.id.folderName);
        pipelininglevelSelector = (Spinner) findViewById(R.id.pipelininglevel);
        filecountSelector = (Spinner) findViewById(R.id.filecount);

        serverSelector.setPromptId(R.string.server_prompt);
        folderNameSelector.setPromptId(R.string.foldername_prompt);
        pipelininglevelSelector.setPromptId(R.string.pipeline_prompt);
        filecountSelector.setPromptId(R.string.filecount_prompt);

        serverSelector.setOnItemSelectedListener(new CustomOnItemSelectedListener());
        folderNameSelector.setOnItemSelectedListener(new CustomOnItemSelectedListener());
        pipelininglevelSelector.setOnItemSelectedListener(new CustomOnItemSelectedListener());
        filecountSelector.setOnItemSelectedListener(new CustomOnItemSelectedListener());
    }

    /**
     * A custom class for the click listeners
     */
    private class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

            Log.d(TAG, "OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString());
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                boolean writeAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    /**
     * Code when the execution is done one by one.
     */
    private void startPipeliningCode() {


        try {
            final ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();

            PoolingNHttpClientConnectionManager pool = new PoolingNHttpClientConnectionManager(ioReactor);
            pool.setMaxTotal(pipeliningLevel * 2);
            pool.setDefaultMaxPerRoute(pipeliningLevel);

            CloseableHttpPipeliningClient httpclient = HttpAsyncClients.createPipelining(pool);
            long startTime = System.currentTimeMillis();
            Log.d(TAG, "Started : " + startTime);
            httpclient.start();
            HttpHost targetHost = new HttpHost(serverName, portNumber);

            final DateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS");


            int blocks = fileCount / pipeliningLevel;

            for (int i = 0; i < blocks; i++) {
                int startIdx = i * pipeliningLevel;
                HttpGet[] requests = new HttpGet[pipeliningLevel];
                for (int j = 0; j < pipeliningLevel; j++) {
                    int counter = startIdx + j;
                    requests[j] = new HttpGet(serverDirName + "/" + counter);
                }
                final CountDownLatch latch = new CountDownLatch(requests.length);
                for (final HttpGet request : requests) {

                    String currentTime = timeFormatter.format(Calendar.getInstance().getTime());
                    Log.d(TAG, "File : " + request.getRequestLine().getUri() + " Request Sent time : " + currentTime);

                    httpclient.execute(targetHost, request, new FutureCallback<HttpResponse>() {
                        @Override
                        public void completed(HttpResponse response) {
                            latch.countDown();
                            HttpEntity entity = response.getEntity();
                            try {
                                if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
                                    Log.d(TAG, "NO EXTERNAL STORAGE");
                                } else {
                                    Header headers[] = response.getAllHeaders();
                                    String uriName = request.getRequestLine().getUri();

                                    if (uriName.startsWith("/")) {
                                        filename = uriName.substring(1);
                                    } else {
                                        filename = uriName;
                                    }

                                    if (filename.contains("/")) {
                                        filename = filename.substring(filename.indexOf("/") + 1);
                                    }
                                    File tempFile = new File(myExternalFile, filename);

                                    InputStream is = entity.getContent();

                                    int bytesRead;
                                    FileOutputStream out = new FileOutputStream(tempFile);

                                    byte[] buffer = new byte[8 * 1024];

                                    while ((bytesRead = is.read(buffer)) != -1) {
                                        out.write(buffer, 0, bytesRead);
                                    }
                                    is.close();
                                    out.close();

                                    String currentTime = timeFormatter.format(Calendar.getInstance().getTime());

                                    Log.v(TAG, "DOWNLOADING FILENAME : " + filename + " Server Time : "
                                            + headers[0].getValue() + " Local Time : " + currentTime);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void failed(final Exception ex) {
                            latch.countDown();
                            Log.d(TAG, "failed : " + request.getRequestLine() + "->" + ex);
                        }

                        @Override
                        public void cancelled() {
                            latch.countDown();
                            Log.d(TAG, "cancelled : " + request.getRequestLine() + " cancelled");
                        }
                    });
                }
                latch.await();


            }
            Log.d(TAG, "Shutting down");
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            Log.d(TAG, "Time Taken : " + (double) elapsedTime / 1000 + " secs");
            httpclient.close();
            //textView1.setText("Time Taken : " + (double) elapsedTime / 1000 + " secs");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();

        }
    }
}
