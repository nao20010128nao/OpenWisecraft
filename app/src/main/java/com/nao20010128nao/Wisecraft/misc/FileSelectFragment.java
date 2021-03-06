package com.nao20010128nao.Wisecraft.misc;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.support.v7.app.*;
import android.view.*;
import android.widget.*;
import com.ipaulpro.afilechooser.*;
import com.nao20010128nao.Wisecraft.R;
import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.activity.*;

import java.io.*;
import java.security.*;
import java.util.*;

public class FileSelectFragment extends BaseFragment<AppCompatActivity> {
    Map<Integer, ServerListActivity.ChooserResult> results = new HashMap<>();
    SecureRandom sr = new SecureRandom();
    Object lastResult = null;

    Button select;
    ImageButton fileLocal, fileProvided;
    EditText path;
    LinearLayout pathForm, modeForm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.server_list_imp_exp, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        select = getView().findViewById(R.id.selectFile);
        fileLocal = getView().findViewById(R.id.openLocalChooser);
        fileProvided = getView().findViewById(R.id.openProvidedChooser);
        path = getView().findViewById(R.id.filePath);
        pathForm = getView().findViewById(R.id.pathForm);
        modeForm = getView().findViewById(R.id.modeForm);

        select.setOnClickListener(v -> {
            pathForm.setVisibility(View.GONE);
            modeForm.setVisibility(View.VISIBLE);
        });
        fileLocal.setOnClickListener(v -> {
            modeForm.setVisibility(View.GONE);
            pathForm.setVisibility(View.VISIBLE);

            File f = new File(path.getText().toString());
            if ((!f.exists()) | f.isFile()) f = f.getParentFile();
            startChooseFileForSelect(f, new ServerListActivity.FileChooserHandler() {
                public void onSelected(File f) {
                    path.setText(f.toString());
                    path.setEnabled(true);
                }

                public void onSelectCancelled() {/*No-op*/}
            });
        });
        fileProvided.setOnClickListener(v -> {
            modeForm.setVisibility(View.GONE);
            pathForm.setVisibility(View.VISIBLE);
            startExtChooseFile(new ServerListActivity.UriFileChooserResult() {
                public void onSelected(Uri f) {
                    path.setText("");
                    path.setEnabled(false);
                }

                public void onSelectCancelled() {/*No-op*/}
            });
        });
        path.setOnTouchListener((v, ev) -> {
            if (ev.getAction() != MotionEvent.ACTION_UP) return false;
            if (!v.isEnabled()) {
                v.setEnabled(true);
                path.setText(getArguments().getString("default"));
                lastResult = null;
            }
            return false;
        });
        Bundle args = getArguments();
        String s = Environment.getExternalStorageDirectory().toString();
        if (args != null) {
            if (args.containsKey("default")) {
                s = args.getString("default");
            }
        }
        path.setText(s);
        fileLocal.setImageDrawable(TheApplication.getTintedDrawable(R.drawable.ic_file, Color.WHITE, getActivity()));
        fileProvided.setImageDrawable(TheApplication.getTintedDrawable(R.drawable.ic_launch_black_36dp, Color.WHITE, getActivity()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (results.containsKey(requestCode)) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (results.get(requestCode) instanceof ServerListActivity.FileChooserHandler) {
                        results.get(requestCode)
                            .onSelected((File) (lastResult = new File(data.getStringExtra("path"))));
                    } else if (results.get(requestCode) instanceof ServerListActivity.UriFileChooserResult) {
                        results.get(requestCode)
                            .onSelected((Uri) (lastResult = data.getData()));
                    }
                    break;
                case Activity.RESULT_CANCELED:
                    results.get(requestCode).onSelectCancelled();
                    break;
            }
            results.remove(requestCode);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Object last = getResult();
        if (last instanceof File)
            outState.putSerializable("file", (File) last);
        else if (last instanceof Uri)
            outState.putParcelable("uri", (Uri) last);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) return;
        if (savedInstanceState.containsKey("file")) {
            lastResult = savedInstanceState.getSerializable("file");
            path.setText(lastResult.toString());
            path.setEnabled(true);
        } else if (savedInstanceState.containsKey("uri")) {
            lastResult = savedInstanceState.<Uri>getParcelable("uri");
            path.setText("");
            path.setEnabled(false);
        }
    }


    public void startChooseFileForOpen(File startDir, ServerListActivity.FileChooserHandler result) {
        int call = nextCallId();
        Intent intent = new Intent(getContext(), FileOpenChooserActivity.class);
        if (startDir != null) {
            intent.putExtra("path", startDir.toString());
        }
        results.put(call, Utils.requireNonNull(result));
        startActivityForResult(intent, call);
    }

    public void startChooseFileForSelect(File startDir, ServerListActivity.FileChooserHandler result) {
        int call = nextCallId();
        Intent intent = new Intent(getContext(), FileChooserActivity.class);
        if (startDir != null) {
            intent.putExtra("path", startDir.toString());
        }
        results.put(call, Utils.requireNonNull(result));
        startActivityForResult(intent, call);
    }

    public void startChooseDirectory(File startDir, ServerListActivity.FileChooserHandler result) {
        int call = nextCallId();
        Intent intent = new Intent(getContext(), DirectoryChooserActivity.class);
        if (startDir != null) {
            intent.putExtra("path", startDir.toString());
        }
        results.put(call, Utils.requireNonNull(result));
        startActivityForResult(intent, call);
    }

    public void startExtChooseFile(ServerListActivity.UriFileChooserResult result) {
        int call = nextCallId();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, call);
    }

    private int nextCallId() {
        int call = Math.abs(sr.nextInt()) & 0xff;
        while (results.containsKey(call)) {
            call = Math.abs(sr.nextInt()) & 0xff;
        }
        return call;
    }

    public Object getLastResult() {
        return lastResult;
    }

    public Object getResult() {
        if (lastResult == null) {
            //no choose, so file
            return new File(path.getText().toString());
        } else if (lastResult instanceof File) {
            //file choosen, so file
            return new File(path.getText().toString());
        } else {
            //uri retrived, return last result
            return lastResult;
        }
    }
}
