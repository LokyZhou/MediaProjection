package com.example.zzr.mediaprojection.ftp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.zzr.mediaprojection.MainActivity;
import com.example.zzr.mediaprojection.R;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by MAGIC-L on 2016/4/8.
 */
public class Setting extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private String TAG = "ftp";
    private EditTextPreference password = null;
    private static String Password = null;
    private EditTextPreference ftp_port;
    private CheckBoxPreference ftp_status = null;
    private EditTextPreference rtsp_port;
    private CheckBoxPreference rtsp_status = null;
    private EditTextPreference http_port;
    private CheckBoxPreference http_status = null;
    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "created");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting);
        EditTextPreference username = (EditTextPreference)findPreference("username");
        username.setOnPreferenceChangeListener((preference, newValue) -> {
            String newUsername = (String) newValue;
            if (!newUsername.matches("[a-zA-Z0-9]+")) {
                Toast.makeText(MainActivity.mcontext,
                        R.string.username_validation_error, Toast.LENGTH_LONG).show();
                return false;
            }
            username.setSummary(newUsername);
            return true;
        });
        password = (EditTextPreference)findPreference("password");
        password.setOnPreferenceChangeListener((preference, newValue) -> {
            String newpassword = (String) newValue;
            if (!newpassword.matches("[a-zA-Z0-9]+")) {
                Toast.makeText(MainActivity.mcontext,
                        R.string.password_validation_error, Toast.LENGTH_LONG).show();
                return false;
            }
//            password.setSummary(newpassword);
            password.setSummary(transformPassword(newpassword, true));
            return true;
        });
        Preference chroot_pref = (Preference)findPreference("chrootDir");
        chroot_pref.setSummary(getChrootDirAsString());
        chroot_pref.setOnPreferenceClickListener(preference -> {
            AlertDialog folderPicker = new FolderPickerDialogBuilder(this, getChrootDir())
                    .setSelectedButton(R.string.select, path -> {
                        if (preference.getSummary().equals(path))
                            return;
                        if (!setChrootDir(path))
                            return;
                        // TODO: this is a hotfix, create correct resources, improve UI/UX
                        final File root = new File(path);
                        if (!root.canRead()) {
                            Toast.makeText(this,
                                    "Notice that we can't read/write in this folder.",
                                    Toast.LENGTH_LONG).show();
                        } else if (!root.canWrite()) {
                            Toast.makeText(this,
                                    "Notice that we can't write in this folder, reading will work. Writing in subfolders might work.",
                                    Toast.LENGTH_LONG).show();
                        }

                        preference.setSummary(path);

                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            folderPicker.show();
            return true;
        });
        CheckBoxPreference showpassword = (CheckBoxPreference)findPreference("show_password");
        showpassword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newpassword = password.getText();
                password.setSummary(transformPassword(newpassword, false));
                return true;
            }
        });
        Preference register = (Preference)findPreference("register");
        register.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences.Editor editor = MainActivity.mcontext.getSharedPreferences("FTP", MODE_PRIVATE).edit();
                Set<String> set = new HashSet<String>();
                set.add(Password);
                set.add((String)chroot_pref.getSummary());
                editor.putStringSet(username.getText(), set);
                editor.commit();
                Log.d(Util.TAG,"Username is "+username.getText()+"\n"+set);
                onDestroy();
                return false;
            }
        });
        ftp_port = (EditTextPreference)findPreference("ftpport");
        ftp_port.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int ftp_port = Integer.valueOf(newValue.toString());
                if(ftp_port<=65535&&ftp_port>=1){
                    Util.port = ftp_port;
                    sendBroadcast(new Intent(FsService.ACTION_STOP_FTPSERVER));
                    try {
                        Thread.sleep(1000);  //进行Ftp服务关闭处理
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
                    return  true;
                }else{
                    Toast.makeText(MainActivity.mcontext,"端口号输入错误",Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
        ftp_status = (CheckBoxPreference)findPreference("start_ftp");
        ftp_status.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.mcontext);
                Boolean status = true;
                status = sp.getBoolean("start_ftp",status);
                if(status){
                    sendBroadcast(new Intent(FsService.ACTION_STOP_FTPSERVER));
                    Toast.makeText(MainActivity.mcontext,"Ftp服务器关闭",Toast.LENGTH_LONG).show();
                }else{
                    sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
                    Toast.makeText(MainActivity.mcontext,"Ftp服务器开启",Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
        rtsp_port = (EditTextPreference)findPreference("RTSP_port");
        rtsp_port.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int rtsp_port = Integer.valueOf(newValue.toString());
                if(rtsp_port<=65535&&rtsp_port>=1) {
                    MainActivity.RTSP_PORT = rtsp_port;
                    return  true;
                }else{
                    Toast.makeText(MainActivity.mcontext,"端口号输入错误",Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
        rtsp_status = (CheckBoxPreference)findPreference("RTSP");
        rtsp_status.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.mcontext);
                Boolean status = Boolean.TRUE;
                status = sp.getBoolean("RTSP",status);
                if(status){
                    sendBroadcast(new Intent(MainActivity.ACTION_RTSP_STOP));
                    Toast.makeText(MainActivity.mcontext,"RTSP服务器关闭",Toast.LENGTH_LONG);
                }else{
                    sendBroadcast(new Intent(MainActivity.ACTION_RTSP_START));
                    Toast.makeText(MainActivity.mcontext,"RTSP服务器打开",Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });



    }
    public static boolean setChrootDir(String dir) {
        File chrootTest = new File(dir);
        if (!chrootTest.isDirectory() || !chrootTest.canRead())
            return false;
        final SharedPreferences sp = getSharedPreferences();
        sp.edit().putString("chrootDir", dir).apply();
        return true;
    }
    private static String getChrootDirAsString() {
        File dirFile = getChrootDir();
        return dirFile != null ? dirFile.getAbsolutePath() : "";
    }

    private static File getChrootDir() {
        final SharedPreferences sp = getSharedPreferences();
        String dirName = sp.getString("chrootDir", "");
        File chrootDir = new File(dirName);
        if (dirName.equals("")) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                chrootDir = Environment.getExternalStorageDirectory();
            } else {
                chrootDir = new File("/");
            }
        }
        if (!chrootDir.isDirectory()) {
            Log.e(Util.TAG, "getChrootDir: not a directory");
            return null;
        }
        return chrootDir;
    }
    private static SharedPreferences getSharedPreferences() {
        final Context context = MainActivity.mcontext;
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        updateLoginInfo();
    }

    private void updateLoginInfo() {
        String newpassword = password.getText();
        password.setSummary(transformPassword(newpassword,true));
    }


    static private String transformPassword(String password,Boolean trans) {
        Password = password;
        Context context = FsApp.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = context.getResources();
        String showPasswordString = res.getString(R.string.show_password_default);
        boolean showPassword = showPasswordString.equals("true");
        showPassword = sp.getBoolean("show_password", showPassword);
        if(trans){
        if (showPassword)
            return password;
        else {
            StringBuilder sb = new StringBuilder(password.length());
            for (int i = 0; i < password.length(); ++i)
                sb.append('*');
            return sb.toString();
        }}else {
            if (!showPassword)
                return password;
            else {
                StringBuilder sb = new StringBuilder(password.length());
                for (int i = 0; i < password.length(); ++i)
                    sb.append('*');
                return sb.toString();
            }
        }

    }

}
