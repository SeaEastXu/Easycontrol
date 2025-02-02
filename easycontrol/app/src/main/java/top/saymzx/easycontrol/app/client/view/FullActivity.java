package top.saymzx.easycontrol.app.client.view;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;

import java.nio.ByteBuffer;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.ClientController;
import top.saymzx.easycontrol.app.client.ControlPacket;
import top.saymzx.easycontrol.app.databinding.ActivityFullBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;
import top.saymzx.easycontrol.app.helper.ViewTools;

public class FullActivity extends Activity implements SensorEventListener {
  private boolean isClose = false;
  private Device device;
  private ActivityFullBinding activityFullBinding;
  private boolean autoRotate;
  private boolean light = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ViewTools.setFullScreen(this);
    activityFullBinding = ActivityFullBinding.inflate(this.getLayoutInflater());
    setContentView(activityFullBinding.getRoot());
    device = ClientController.getDevice(getIntent().getStringExtra("uuid"));
    if (device == null) return;
    ClientController.setFullView(device.uuid, this);
    // 初始化
    activityFullBinding.barView.setVisibility(View.GONE);
    setNavBarHide(device.showNavBarOnConnect);
    autoRotate = AppData.setting.getAutoRotate();
    activityFullBinding.buttonAutoRotate.setImageResource(autoRotate ? R.drawable.un_rotate : R.drawable.rotate);
    // 按键监听
    setButtonListener();
    setKeyEvent();
    // 更新textureView
    activityFullBinding.textureViewLayout.addView(ClientController.getTextureView(device.uuid), 0);
    activityFullBinding.textureViewLayout.post(this::updateMaxSize);
    // 页面自动旋转
    AppData.sensorManager.registerListener(this, AppData.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onPause() {
    AppData.sensorManager.unregisterListener(this);
    if (isChangingConfigurations()) activityFullBinding.textureViewLayout.removeView(ClientController.getTextureView(device.uuid));
    else if (!isClose) ClientController.handleControll(device.uuid, device.fullToMiniOnRunning ? "changeToMini" : "changeToSmall", ByteBuffer.wrap("changeToFull".getBytes()));
    super.onPause();
  }

  @Override
  public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
    activityFullBinding.textureViewLayout.post(this::updateMaxSize);
    super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
  }

  @Override
  public void onBackPressed() {
  }

  private void updateMaxSize() {
    int width = activityFullBinding.textureViewLayout.getMeasuredWidth();
    int height = activityFullBinding.textureViewLayout.getMeasuredHeight();
    ByteBuffer byteBuffer = ByteBuffer.allocate(8);
    byteBuffer.putInt(width);
    byteBuffer.putInt(height);
    byteBuffer.flip();
    ClientController.handleControll(device.uuid, "updateMaxSize", byteBuffer);
    if (!device.customResolutionOnConnect && device.changeResolutionOnRunning) ClientController.handleControll(device.uuid, "writeByteBuffer", ControlPacket.createChangeResolutionEvent((float) width / height));
  }

  public void hide() {
    try {
      isClose = true;
      activityFullBinding.textureViewLayout.removeView(ClientController.getTextureView(device.uuid));
      finish();
    } catch (Exception ignored) {
    }
  }

  // 设置按钮监听
  private void setButtonListener() {
    activityFullBinding.buttonRotate.setOnClickListener(v -> ClientController.handleControll(device.uuid, "buttonRotate", null));
    activityFullBinding.buttonBack.setOnClickListener(v -> ClientController.handleControll(device.uuid, "buttonBack", null));
    activityFullBinding.buttonHome.setOnClickListener(v -> ClientController.handleControll(device.uuid, "buttonHome", null));
    activityFullBinding.buttonSwitch.setOnClickListener(v -> ClientController.handleControll(device.uuid, "buttonSwitch", null));
    activityFullBinding.buttonNavBar.setOnClickListener(v -> {
      setNavBarHide(activityFullBinding.navBar.getVisibility() == View.GONE);
      changeBarView();
    });
    activityFullBinding.buttonMini.setOnClickListener(v -> ClientController.handleControll(device.uuid, "changeToMini", null));
    activityFullBinding.buttonSmall.setOnClickListener(v -> ClientController.handleControll(device.uuid, "changeToSmall", null));
    activityFullBinding.buttonClose.setOnClickListener(v -> ClientController.handleControll(device.uuid, "close", null));
    activityFullBinding.buttonLight.setOnClickListener(v -> {
      light = !light;
      activityFullBinding.buttonLight.setImageResource(light ? R.drawable.lightbulb_off : R.drawable.lightbulb);
      ClientController.handleControll(device.uuid, light ? "buttonLight" : "buttonLightOff", null);
      changeBarView();
    });
    activityFullBinding.buttonPower.setOnClickListener(v -> {
      ClientController.handleControll(device.uuid, "buttonPower", null);
      changeBarView();
    });
    activityFullBinding.buttonMore.setOnClickListener(v -> changeBarView());
    activityFullBinding.buttonAutoRotate.setOnClickListener(v -> {
      autoRotate = !autoRotate;
      AppData.setting.setAutoRotate(autoRotate);
      activityFullBinding.buttonAutoRotate.setImageResource(autoRotate ? R.drawable.un_rotate : R.drawable.rotate);
      changeBarView();
    });
  }

  // 导航栏隐藏
  private void setNavBarHide(boolean isShow) {
    activityFullBinding.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    activityFullBinding.buttonNavBar.setImageResource(isShow ? R.drawable.not_equal : R.drawable.equals);
    activityFullBinding.textureViewLayout.post(this::updateMaxSize);
  }

  private void changeBarView() {
    boolean toShowView = activityFullBinding.barView.getVisibility() == View.GONE;
    boolean isLandscape = lastOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || lastOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    ViewTools.viewAnim(activityFullBinding.barView, toShowView, 0, PublicTools.dp2px(40f) * (isLandscape ? -1 : 1), (isStart -> {
      if (isStart && toShowView) activityFullBinding.barView.setVisibility(View.VISIBLE);
      else if (!isStart && !toShowView) activityFullBinding.barView.setVisibility(View.GONE);
    }));
  }

  private int lastOrientation = -1;

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (!autoRotate || Sensor.TYPE_ACCELEROMETER != sensorEvent.sensor.getType()) return;
    float[] values = sensorEvent.values;
    float x = values[0];
    float y = values[1];
    int newOrientation = lastOrientation;

    if (x > -3 && x < 3 && y >= 4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    else if (y > -3 && y < 3 && x >= 4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    else if (y > -3 && y < 3 && x <= -4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    else if (x > -3 && x < 3 && y <= -4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

    if (lastOrientation != newOrientation) {
      lastOrientation = newOrientation;
      setRequestedOrientation(newOrientation);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }

  // 设置键盘监听
  private void setKeyEvent() {
    activityFullBinding.editText.requestFocus();
    activityFullBinding.editText.setInputType(InputType.TYPE_NULL);
    activityFullBinding.editText.setOnKeyListener((v, keyCode, event) -> {
      if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
        ClientController.handleControll(device.uuid, "writeByteBuffer", ControlPacket.createKeyEvent(event.getKeyCode(), event.getMetaState()));
        return true;
      }
      return false;
    });
  }
}