package org.haack.virtualeyes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "Main::Activity";

    private MainView mOpenCvCameraView;
    
    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;
    
    Mat frame;
    int selectedRegion;
    boolean displayOn = false;
    
    Mat virtualContent = null;
    boolean contentLoaded = false;
        
    RegionDetector rd;
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main_surface_view);

        mOpenCvCameraView = (MainView) findViewById(R.id.main_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
        rd = new RegionDetector();
        selectedRegion = -1;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

        mColorEffectsMenu = menu.addSubMenu("Color Effect");
        mEffectMenuItems = new MenuItem[effects.size()];

        int idx = 0;
        ListIterator<String> effectItr = effects.listIterator();
        while(effectItr.hasNext()) {
           String element = effectItr.next();
           mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
           idx++;
        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
         }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	frame = inputFrame.rgba();
    	
        ArrayList<Region> regions = rd.detect(frame);
    	
        List<MatOfPoint> polys;
		polys = new ArrayList<MatOfPoint>();
        
		//draw the active regions
        for (Integer i = 0; i < regions.size(); i++) {
			if (regions.get(i).active) {
				org.opencv.core.Rect rect = regions.get(i).getBoundingRect();
//				Core.rectangle(frame, rect.tl(), rect.br(), new Scalar(100, 100, 100), 2);
				Core.putText(frame, i.toString(i), rect.tl(), Core.FONT_HERSHEY_COMPLEX_SMALL, 3.0, new Scalar(255, 255, 255), 2);
				
				if (i == selectedRegion) {
					polys.add(regions.get(i).contour);
				}
			}
		}

		Core.polylines(frame, polys, true, new Scalar(255, 255, 255), 2);
		
		//load virtual content into mat if not loaded already
		//doesn't work if called in on create (even without refering frame)
		if (!contentLoaded) {
//			File root = Environment.getExternalStorageDirectory();
//			virtualContent = BitmapFactory.decodeFile(root+"/watson.jpg"); //if on sdcard
			Bitmap virtualBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.watson);
			virtualContent = new Mat(150, 100, frame.type()); //same type as frame
			Utils.bitmapToMat(virtualBitmap, virtualContent);
			virtualContent.convertTo(virtualContent, frame.type());
			contentLoaded = true;
		}
		
		if (selectedRegion != -1) {
			Region region = rd.regions.get(selectedRegion);
			if (region.active) {
				//get transformed virtual content
				Mat virtual = rd.matchVirtual(virtualContent, region);
				
				//draw in bounded rect
				Rect bounds = region.getBoundingRect();
				for (int i = (int) bounds.y; i < virtual.height(); i++) {
				    for (int j = (int) bounds.x; j < virtual.width(); j++) {
				        double[] data = virtual.get(i, j);
				        if (!((data[0] == 0) && (data[1] == 0) && (data[2] == 0))) {
				        	frame.put(i, j, data);
				        }
				    }
				}
			}
		}
        return frame;
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.i(TAG,"onTouch event");
        
		if (selectedRegion != -1) {
	            saveMat(rd.extractRegion(frame, rd.regions.get(selectedRegion)), 600, 400, false);
	            	
		}
		
     	PointerCoords pc = new PointerCoords();
 		event.getPointerCoords(0, pc);
 		for (int i = 0; i < rd.regions.size(); i++) {
 			if (rd.regions.get(i).active) {
 				if (rd.regions.get(i).getBoundingRect().contains(new Point(pc.x, pc.y))) {
 					selectedRegion = i;
 					break;
 				}
 			}
 		}
		
		if (contentLoaded && (selectedRegion != -1)) {
//			Mat virtual = rd.matchVirtual(virtualContent, rd.regions.get(selectedRegion));
//			saveMat(virtual, rd.regions.get(selectedRegion).getBoundingRect().width, rd.regions.get(selectedRegion).getBoundingRect().height, true);
			
//			saveMat(virtualContent, 150, 100, true);
		}
		
        return true;
	}
	
	public void saveMat(Mat mat, int width, int height, boolean anysize) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() +
                               "/Vi_" + currentDateandTime + ".jpg";
//        mOpenCvCameraView.takePicture(fileName); //takes photo frame and saves automatically
        
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Config.ARGB_8888);

        Utils.matToBitmap(mat, bitmap);
		
        if (!anysize) {
        	bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        }
        try {
            File file = new File(fileName);
            OutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        bitmap.recycle();
        
        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
	}
}
