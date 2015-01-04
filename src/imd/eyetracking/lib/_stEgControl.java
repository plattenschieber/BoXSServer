package imd.eyetracking.lib;
import com.ochafik.lang.jnaerator.runtime.CharByReference;
import com.ochafik.lang.jnaerator.runtime.Structure;
import com.sun.jna.Pointer;

/**
 * The stEgControl structure contains control and status variables to be used by the Eyegaze application program to setup and control the
 * Eyegaze image processing software, and to access the Eyegaze data. 
 * <i>native declaration : line 32</i><br>
 */
public class _stEgControl extends Structure<_stEgControl, _stEgControl.ByValue, _stEgControl.ByReference > {
	/**
	 * pointer to the Eyegaze data structure where EgGetData() places the next gazepoint data sample. 
	 * This memory is allocated by the EgInit() function.  The pointer to data structure is returned to the application.
	 * C type : _stEgData*
	 */
	public _stEgData.ByReference pstEgData;
	
	/**
	 * number of gazepoint data samples in Eyegaze's internal ring buffer.
	 * The application must set the ringbuffer length in the stEgControl structure before calling EgInit()
	 */
	public int iNDataSetsInRingBuffer;
	
	/**
	 * flag controls whether eyetracking is presently on (TRUE = 1) or off (FALSE = 0).
	 * If the flag is on when a new camera field finishes, the Eyegaze thread processes the image and puts the results in the data ring buffer;
	 * if the flag is off, the camera field is not processed. The application may turn this tracking flag on or off at any time.
	 */
	public int bTrackingActive;
	
	/**
	 * Pixel width of the full computer screen 
	 */
	public int iScreenWidthPix;

	/**
	 * Pixel height of the full computer screen 
	 */
	public int iScreenHeightPix;

	/**
	 * flag controls whether or not the full image from the Eyegaze camera is displayed in a separate window on the VGA.
	 * The application must set this flag prior to calling EgInit() and may turn the display flag on and off at any time.
	 */
	public int bEgCameraDisplayActive;
	
	/**
	 * Screen position of the eye images.
	 * 0 = upper left, 1 = upper right 
	 */
	public int iEyeImagesScreenPos;
	
	/**
	 * Communication type:       Comp Config:
	 * EG_COMM_TYPE_LOCAL,       Single 
	 * EG_COMM_TYPE_SOCKET,      Double
	 */
	public int iCommType;
	
	/**
	 * Pointer to serial port name or IP adress of server machine
	 * used only in Double Computer Config
	 * C type : wchar_t*
	 */
	public CharByReference pszCommName;
	
	/**
	 * Reserved - set to 0 (unused)
	 */
	public int iVisionSelect;
	
	/** 				OUTPUTS TO APPLICATION:                */

	/**
	 * number of gazepoint data samples presently available for the application to retrive from Eyegaze's internal ring buffer.
	 */
	public int iNPointsAvailable;
	
	/**
	 * number of irretrievably missed gazepoint data samples, i.e. the number of valid data points at the tail of the ring buffer that the application
	 * did not retrieve and that Eyegaze overwrote since the application last called EgGetData().
	 */
	public int iNBufferOverflow;
	
	/**
	 * Eyegaze image processing rate - depends on the camera field rate: 
	 * RS_170               60 Hz
	 * CCIR                 50 Hz
	 */
	public int iSamplePerSec;
	
	/**
	 * Eyegaze monitor scale factors  
	 */
	public float fHorzPixPerMm;
	
	/**
	 *  (pixel / millimeter)
	 */
	public float fVertPixPerMm;
	
	/**
	 * address of the video buffer containing the most recently processed camera image field
	 * C type : void*
	 */
	public Pointer pvEgVideoBufferAddress;
	
	/**     INTERNAL EYEGAZE VARIABLE              */
	
	/**
	 * Eyegaze handle -- used internally by Eyegaze to keep track of which vision subsubsystem is in use.
	 * (not used by application)
	 * C type : void*
	 */
	public Pointer hEyegaze;
	
	/**
	 * Constructor of control structure
	 */
	public _stEgControl() {
		super();
		initFieldOrder();
	}
	
	protected void initFieldOrder() {
		setFieldOrder(new String[]{"pstEgData", "iNDataSetsInRingBuffer", "bTrackingActive", "iScreenWidthPix", "iScreenHeightPix", "bEgCameraDisplayActive", "iEyeImagesScreenPos", "iCommType", "pszCommName", "iVisionSelect", "iNPointsAvailable", "iNBufferOverflow", "iSamplePerSec", "fHorzPixPerMm", "fVertPixPerMm", "pvEgVideoBufferAddress", "hEyegaze"});
	}
	
	protected ByReference newByReference() { return new ByReference(); }
	
	protected ByValue newByValue() { return new ByValue(); }
	
	protected _stEgControl newInstance() { return new _stEgControl(); }
	
	public static _stEgControl[] newArray(int arrayLength) {
		return Structure.newArray(_stEgControl.class, arrayLength);
	}
	
	public static class ByReference extends _stEgControl implements Structure.ByReference {
		
	};
	
	public static class ByValue extends _stEgControl implements Structure.ByValue {
		
	};
}
