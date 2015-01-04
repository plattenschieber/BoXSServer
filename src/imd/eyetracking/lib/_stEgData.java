package imd.eyetracking.lib;
import com.ochafik.lang.jnaerator.runtime.Structure;
import com.sun.jna.NativeLong;
/**
 * The stEgData structure contains the results of the Eyegaze image processing from a given field of video camera data. 
 * <i>native declaration : line 117</i><br>
 */
public class _stEgData extends Structure<_stEgData, _stEgData.ByValue, _stEgData.ByReference > {
	
	/**
	 * flag indicating whether the image processing software found the eye, i.e. found a valid glint pupil vector 
	 * (TRUE = 1, FALSE = 0)
	 */
	public int bGazeVectorFound;
	
	/**
	 * integer coordinates of the usergazepoint referenced to the full computer display space (pixels)
	 * 0,0 origin at upper left corner
	 * NOTE: In the case of multiple monitors, this is the upper left corner of the upper-most / left-most monitor
	 */
	
	
	/**
	 * positive rightward 
	 */
	public int iIGaze;
	
	/** 
	 * positive downward
	 */
	public int iJGaze;
	
	/**
	 * actual pupil radius (mm)  
	 */
	public float fPupilRadiusMm;
	
	/**
	 *  offset of the eyeball center from the camera axis (mm)
	 *  Notes on polarity:
	 *  x positive: head moves to user's right
	 *  y positive: head moves up
	 */
	public float fXEyeballOffsetMm;
	public float fYEyeballOffsetMm;
	
	/**
	 * distance from the camera sensor plane to the camera focus plane, at the time the camera captured the image(mm)
	 */
	public float fFocusRangeImageTime;
	
	/**
	 * range offset between the camera focus plane and the corneal surface of the eye, as measured from the size and
	 * orientation of the corneal reflection in the eye image - at image time (mm)
	 * A positive offset means the eye is beyond the lens' focus range.
	 */
	public float fFocusRangeOffsetMm;
	
	/**
	 * distance that the lens extension would have to be changed to bring the eye into clear focus (millimeters) (at image time)  
	 */
	public float fLensExtOffsetMm;
	
	/**
	 * number of camera fields, i.e. 60ths of a second, that have occurred since the starting reference time (midnight January 1, this year)
	 */
	public NativeLong ulCameraFieldCount;
	
	/**
	 * The application time that the gazepoint was actually valid.  
	 * (dGazeTimeSec represents the original image-capture time, not the time that the gazepoint calculation was completed.) 
	 */
	public double dGazeTimeSec;
	
	/**
	 * Pentium TSC counter value at the moment that the mark was incremented.
	 */
	public double dAppMarkTimeSec;
	
	/**
	 * Mark count used in logging functions.  
	 */
	public int iAppMarkCount;
	
	/**
	 * The application time that Eyegaze reported the gazepoint
	 */
	public double dReportTimeSec;
	
	public _stEgData() {
		super();
		initFieldOrder();
	}
	
	protected void initFieldOrder() {
		setFieldOrder(new String[]{"bGazeVectorFound", "iIGaze", "iJGaze", "fPupilRadiusMm", "fXEyeballOffsetMm", "fYEyeballOffsetMm", "fFocusRangeImageTime", "fFocusRangeOffsetMm", "fLensExtOffsetMm", "ulCameraFieldCount", "dGazeTimeSec", "dAppMarkTimeSec", "iAppMarkCount", "dReportTimeSec"});
	}
	
	protected ByReference newByReference() { return new ByReference(); }
	protected ByValue newByValue() { return new ByValue(); }
	protected _stEgData newInstance() { return new _stEgData(); }
	
	public static _stEgData[] newArray(int arrayLength) {
		return Structure.newArray(_stEgData.class, arrayLength);
	}
	
	public static class ByReference extends _stEgData implements Structure.ByReference {
		
	};
	
	public static class ByValue extends _stEgData implements Structure.ByValue {
		
	};
}
