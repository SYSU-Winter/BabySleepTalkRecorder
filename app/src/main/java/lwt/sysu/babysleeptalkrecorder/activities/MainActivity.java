package lwt.sysu.babysleeptalkrecorder.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import android.support.v7.app.AlertDialog;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import lwt.sysu.babysleeptalkrecorder.R;
import lwt.sysu.babysleeptalkrecorder.sleeptalkrecording.RecordFragment;
import lwt.sysu.babysleeptalkrecorder.mvpbase.BaseActivity;

import java.util.List;

import javax.inject.Inject;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity
        implements HasSupportFragmentInjector, EasyPermissions.PermissionCallbacks {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQ = 222;

    @Inject
    DispatchingAndroidInjector<Fragment> dispatchingAndroidInjector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_container, RecordFragment.newInstance())
                    .commit();
        }
        // 权限检查与申请
        getPermissions();
    }

    // 对于Android6.0及以上的版本，进行动态权限申请
    // 应用中使用的麦克风和读写存储是危险权限
    @TargetApi(23)
    private void getPermissions() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO};
        if (!EasyPermissions.hasPermissions(MainActivity.this, permissions)) {
            EasyPermissions.requestPermissions(this, getString(R.string.permissions_required),
                    PERMISSION_REQ, permissions);
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    // 当所需的权限有其中一项或多项被用户拒绝时，弹出对话框提示
    // 当用户愿意重新给权限时，跳转到系统设置
    private void showRationale() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Permissions Required")
                .setCancelable(false)
                .setMessage(getString(R.string.permissions_required))
                .setPositiveButton(R.string.dialog_action_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openSettingsPage();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onBackPressed();
                    }
                })
                .show();
    }

    private void openSettingsPage() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, PERMISSION_REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getPermissions();
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return dispatchingAndroidInjector;
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            showRationale();
            return;
        }
        finish();
    }
}
