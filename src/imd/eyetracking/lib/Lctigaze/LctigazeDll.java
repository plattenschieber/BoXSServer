package imd.eyetracking.lib.Lctigaze;
import imd.eyetracking.lib._stEgControl;
import imd.eyetracking.lib._stEyeImageInfo;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ochafik.lang.jnaerator.runtime.LibraryExtractor;
import com.ochafik.lang.jnaerator.runtime.MangledFunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA Wrapper for library lctigaze.dll<br>
 * Wrapper is based on the EgWin.h header file
 * lctigaze.dll has to be available in path
 */
public interface LctigazeDll extends Library {
	
	
	public static final String JNA_LIBRARY_NAME = LibraryExtractor.getLibraryPath("lctigaze", true, LctigazeDll.class);
	public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(LctigazeDll.JNA_LIBRARY_NAME, MangledFunctionMapper.DEFAULT_OPTIONS);
	
	//load the library instance
	public static final LctigazeDll INSTANCE = (LctigazeDll)Native.loadLibrary(LctigazeDll.JNA_LIBRARY_NAME, LctigazeDll.class, MangledFunctionMapper.DEFAULT_OPTIONS);
	
	//define constants
	public static final int EG_CALIBRATE_DISABILITY_APP = (int)0;
	public static final int EG_CALIBRATE_NONDISABILITY_APP = (int)1;
	public static final int CAL_KEY_COMMAND_ESCAPE = (int)0;
	public static final int CAL_KEY_COMMAND_RESTART = (int)1;
	public static final int CAL_KEY_COMMAND_SKIP = (int)2;
	public static final int CAL_KEY_COMMAND_ACCEPT = (int)3;
	public static final int CAL_KEY_COMMAND_RETRIEVE = (int)4;
	public static final int CAL_KEY_COMMAND_SPACE = (int)5;
	public static final int EG_MESSAGE_TYPE_FILE_START_RECORDING = (int)36;
	public static final int EG_ERROR_EYEGAZE_ALREADY_INITIALIZED = (int)9101;
	public static final int EG_EVENT_TRACKING_INACTIVE = (int)7;
	public static final int EG_MESSAGE_TYPE_SET_DIAMETER = (int)15;
	public static final int EG_MESSAGE_TYPE_STOP_SENDING_DATA = (int)31;
	public static final int EG_MESSAGE_TYPE_BEGIN_SENDING_VERGENCE = (int)40;
	public static final int EG_COMM_TYPE_SOCKET = (int)1;
	public static final int EG_MESSAGE_TYPE_CLEAR_SCREEN = (int)13;
	public static final int EG_MESSAGE_TYPE_WORKSTATION_RESPONSE = (int)12;
	public static final int EG_MESSAGE_TYPE_DISPLAY_TEXT = (int)18;
	public static final int EG_MESSAGE_TYPE_VOICE_INACTIVE = (int)25;
	public static final int EG_MESSAGE_TYPE_CALIBRATION_ABORTED = (int)20;
	public static final int EG_EVENT_VOICE_INACTIVE = (int)9;
	public static final int EG_MESSAGE_TYPE_TRACKING_INACTIVE = (int)23;
	public static final int EG_MESSAGE_TYPE_FILE_STOP_RECORDING = (int)37;
	public static final int EG_MESSAGE_TYPE_BEGIN_SENDING_DATA = (int)30;
	public static final int EG_MESSAGE_TYPE_KEYBD_COMMAND = (int)3;
	public static final int EG_MESSAGE_TYPE_DRAW_CROSS = (int)17;
	public static final int EG_EVENT_TRACKING_ACTIVE = (int)6;
	public static final int EG_EVENT_MOUSEBUTTON = (int)3;
	public static final int EG_MESSAGE_TYPE_STOP_SENDING_VERGENCE = (int)41;
	public static final int EG_COMM_TYPE_LOCAL = (int)0;
	public static final int EG_MESSAGE_TYPE_GAZEINFO = (int)0;
	public static final int EG_MESSAGE_TYPE_MOUSEPOSITION = (int)1;
	public static final int EG_EVENT_MOUSEPOSITION = (int)1;
	public static final int EG_ERROR_TRACKING_TERMINATED = (int)9102;
	public static final int EG_MESSAGE_TYPE_FILE_CLOSE = (int)39;
	public static final int EG_MESSAGE_TYPE_CLOSE_AND_RECYCLE = (int)32;
	public static final int EG_MESSAGE_TYPE_FILE_APPEND_TEXT = (int)35;
	public static final int EG_EVENT_MOUSERELATIVE = (int)2;
	public static final int EG_MESSAGE_TYPE_FILE_MARK_EVENT = (int)38;
	public static final int EG_MESSAGE_TYPE_SET_COLOR = (int)14;
	public static final int EG_EVENT_KEYBOARD_COMMAND = (int)4;
	public static final int EG_MESSAGE_TYPE_VOICE_ACTIVE = (int)24;
	public static final int EG_MESSAGE_TYPE_DRAW_CIRCLE = (int)16;
	public static final int EG_MESSAGE_TYPE_WORKSTATION_QUERY = (int)11;
	public static final int EG_MESSAGE_TYPE_MOUSEBUTTON = (int)2;
	public static final int EG_MESSAGE_TYPE_CALIBRATE_ABORT = (int)21;
	public static final int EG_MESSAGE_TYPE_FILE_OPEN = (int)33;
	public static final int EG_ERROR_MEMORY_ALLOC_FAILED = (int)9103;
	public static final int EG_EVENT_NONE = (int)0;
	public static final int EG_MESSAGE_TYPE_CALIBRATE = (int)10;
	public static final int EG_MESSAGE_TYPE_CALIBRATION_COMPLETE = (int)19;
	public static final int EG_ERROR_LCT_COMM_OPEN_FAILED = (int)9104;
	public static final int EG_MESSAGE_TYPE_MOUSERELATIVE = (int)4;
	public static final int EG_MESSAGE_TYPE_TRACKING_ACTIVE = (int)22;
	public static final int EG_MESSAGE_TYPE_IMAGEDATA = (int)81;
	public static final int EG_EVENT_UPDATE_EYE_IMAGE = (int)5;
	public static final int EG_MESSAGE_TYPE_FILE_WRITE_HEADER = (int)34;
	public static final int EG_EVENT_VOICE_ACTIVE = (int)8;
	public static final int EG_MESSAGE_TYPE_VERGENCE = (int)5;
	
	void CalibrateDisplayEyeImages(Integer i);
	
	/**
	 * Initialize Eyegaze functions - create eyetracking thread  
	 * Original signature : <code>int EgInit(_stEgControl*)</code><br>
	 * <i>native declaration : line 183</i>
	 */
	int EgInit(_stEgControl pstEgControl);

	/**
	 * Performe the Eyegaze calibration procedure
	 * Original signature : <code>void EgCalibrate(_stEgControl*, HWND, int)</code><br>
	 * <i>native declaration : line 187</i>
	 */
	void EgCalibrate(_stEgControl pstEgControl, HWND hwnd, int iCalAppType);
	
	/**
	 * Performe the Eyegaze calibration procedure
	 * Original signature : <code>void EgCalibrate1(_stEgControl*, HWND, int)</code><br>
	 * <i>native declaration : line 192</i>
	 */
	void EgCalibrate1(_stEgControl pstEgControl, HWND hwnd, int iCalAppType);
	
	/**
	 * Performe the Eyegaze calibration procedure
	 * Original signature : <code>void EgCalibrate2(_stEgControl*, int)</code><br>
	 * <i>native declaration : line 197</i>
	 */
	@Deprecated
	void EgCalibrate2(_stEgControl pstEgControl, int iCalAppType);
	
	/**
	 *Retrieve data collected by the eyegaze image processing thread
	 *EgGetData() returns the buffer index of the last gazepoint sample measured by Eyegaze               
	 * Original signature : <code>int EgGetData(_stEgControl*)</code><br>
	 * <i>native declaration : line 201</i>
	 */
	int EgGetData(_stEgControl.ByReference pstEgControl);
	
	/**
	 * Original signature : <code>int EgGetEvent(_stEgControl*, void*)</code><br>
	 * <i>native declaration : line 208</i>
	 */
	int EgGetEvent(_stEgControl pstEgControl, Pointer pv);
	
	/**
	 * Returns the software version number<br>
	 * Original signature : <code>int EgGetVersion()</code><br>
	 * <i>native declaration : line 211</i>
	 */
	int EgGetVersion();
	
	/**
	 * Shut down Eyegaze operation - terminate eyetracking thread
	 * Original signature : <code>int EgExit(_stEgControl*)</code><br>
	 * <i>native declaration : line 212</i>
	 */
	int EgExit(_stEgControl pstEgControl);
	
	/**
	 * Original signature : <code>double EgGetApplicationStartTimeSec()</code><br>
	 * <i>native declaration : line 216</i>
	 */
	double EgGetApplicationStartTimeSec();
	
	/**
	 * Original signature : <code>int EgLogFileOpen(_stEgControl*, char*, char*)</code><br>
	 * <i>native declaration : line 220</i><br>
	 * @deprecated use the safer methods {@link #EgLogFileOpen(lctigaze._stEgControl, java.nio.ByteBuffer, java.nio.ByteBuffer)} and {@link #EgLogFileOpen(lctigaze._stEgControl, com.sun.jna.Pointer, com.sun.jna.Pointer)} instead
	 */
	int EgLogFileOpen(_stEgControl pstEgControl, String string, String string2);
	
	/**
	 * Original signature : <code>int EgLogFileOpen(_stEgControl*, char*, char*)</code><br>
	 * <i>native declaration : line 220</i>
	 */
	int EgLogFileOpen(_stEgControl pstEgControl, ByteBuffer pszFileName, ByteBuffer pszMode);
	
	/**
	 * Original signature : <code>void EgLogWriteColumnHeader(_stEgControl*)</code><br>
	 * <i>native declaration : line 224</i>
	 */
	void EgLogWriteColumnHeader(_stEgControl pstEgControl);
	
	/**
	 * Original signature : <code>void EgLogAppendText(_stEgControl*, char*)</code><br>
	 * <i>native declaration : line 226</i><br>
	 * @deprecated use the safer methods {@link #EgLogAppendText(lctigaze._stEgControl, java.nio.ByteBuffer)} and {@link #EgLogAppendText(lctigaze._stEgControl, com.sun.jna.Pointer)} instead
	 */
	@Deprecated 
	void EgLogAppendText(_stEgControl pstEgControl, Pointer pszText);
	
	/**
	 * Original signature : <code>void EgLogAppendText(_stEgControl*, char*)</code><br>
	 * <i>native declaration : line 226</i>
	 */
	void EgLogAppendText(_stEgControl pstEgControl, ByteBuffer pszText);
	
	/**
	 * Original signature : <code>void EgLogStart(_stEgControl*)</code><br>
	 * <i>native declaration : line 229</i>
	 */
	void EgLogStart(_stEgControl pstEgControl);
	
	/**
	 * Original signature : <code>void EgLogStop(_stEgControl*)</code><br>
	 * <i>native declaration : line 231</i>
	 */
	void EgLogStop(_stEgControl pstEgControl);
	
	/**
	 * Original signature : <code>int EgLogMark(_stEgControl*)</code><br>
	 * <i>native declaration : line 233</i>
	 */
	int EgLogMark(_stEgControl pstEgControl);
	
	/**
	 * Original signature : <code>void EgLogFileClose(_stEgControl*)</code><br>
	 * <i>native declaration : line 235</i>
	 */
	void EgLogFileClose(_stEgControl pstEgControl);
	
	/**
	 * Original signature : <code>void EgSetScreenDimensions(_stEgControl*, int, int, int, int, int, int, int, int)</code><br>
	 * <i>native declaration : line 238</i>
	 */
	void EgSetScreenDimensions(_stEgControl pstEgControl, int iEgMonWidthPix, int iEgMonHeightPix, int iEgMonHorzOffset, int iEgMonVertOffset, int iEgWindowWidthPix, int iEgWindowHeightPix, int iEgWindowHorzOffset, int iEgWindowVertOffset);
	
	/**
	 * Original signature : <code>void EgInitScreenDimensions(_stEgControl*, int, int, int, int, int, int, int, int)</code><br>
	 * <i>native declaration : line 248</i>
	 */
	void EgInitScreenDimensions(_stEgControl pstEgControl, int iEgMonWidthPix, int iEgMonHeightPix, int iEgMonHorzOffsetPix, int iEgMonVertOffsetPix, int iEgWindowWidthPix, int iEgWindowHeightPix, int iEgWindowHorzOffset, int iEgWindowVertOffset);
	
	/**
	 * Original signature : <code>void EgUpdateScreenResolutions(int, int)</code><br>
	 * <i>native declaration : line 258</i>
	 */
	void EgUpdateScreenResolutions(int iEgMonWidthPix, int iEgMonHeightPix);
	
	/**
	 * Original signature : <code>void EgUpdateMonPixelOffsets(int, int)</code><br>
	 * <i>native declaration : line 261</i>
	 */
	void EgUpdateMonPixelOffsets(int iEgMonHorzOffsetPix, int iEgMonVertOffsetPix);
	
	/**
	 * Original signature : <code>void EgUpdateWindowParameters(int, int, int, int)</code><br>
	 * <i>native declaration : line 264</i>
	 */
	void EgUpdateWindowParameters(int iEgWindowWidthPix, int iEgWindowHeightPix, int iEgWindowHorzOffset, int iEgWindowVertOffset);
	
	/**
	 * Original signature : <code>void EgWindowPixFromMonMm(int*, int*, float, float)</code><br>
	 * <i>native declaration : line 269</i><br>
	 * @deprecated use the safer methods {@link #EgWindowPixFromMonMm(java.nio.IntBuffer, java.nio.IntBuffer, float, float)} and {@link #EgWindowPixFromMonMm(com.sun.jna.ptr.IntByReference, com.sun.jna.ptr.IntByReference, float, float)} instead
	 */
	@Deprecated 
	void EgWindowPixFromMonMm(IntByReference piIEgWindowPix, IntByReference piJEgWindowPix, float fXMonMm, float fYMonMm);
	
	/**
	 * Original signature : <code>void EgWindowPixFromMonMm(int*, int*, float, float)</code><br>
	 * <i>native declaration : line 269</i>
	 */
	void EgWindowPixFromMonMm(IntBuffer piIEgWindowPix, IntBuffer piJEgWindowPix, float fXMonMm, float fYMonMm);
	
	/**
	 * Original signature : <code>void MonMmFromEgWindowPix(float*, float*, float*, int, int)</code><br>
	 * @param pfZMonMm pointer to Z may be null<br>
	 * <i>native declaration : line 274</i><br>
	 * @deprecated use the safer methods {@link #MonMmFromEgWindowPix(java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, int, int)} and {@link #MonMmFromEgWindowPix(com.sun.jna.ptr.FloatByReference, com.sun.jna.ptr.FloatByReference, com.sun.jna.ptr.FloatByReference, int, int)} instead
	 */
	@Deprecated 
	void MonMmFromEgWindowPix(FloatByReference pfXMonMm, FloatByReference pfYMonMm, FloatByReference pfZMonMm, int iIEgWindowPix, int iJEgWindowPix);
	
	/**
	 * Original signature : <code>void MonMmFromEgWindowPix(float*, float*, float*, int, int)</code><br>
	 * @param pfZMonMm pointer to Z may be null<br>
	 * <i>native declaration : line 274</i>
	 */
	void MonMmFromEgWindowPix(FloatBuffer pfXMonMm, FloatBuffer pfYMonMm, FloatBuffer pfZMonMm, int iIEgWindowPix, int iJEgWindowPix);
	
	/**
	 * Original signature : <code>void EgMonitorPixFromMonMm(int*, int*, float, float)</code><br>
	 * <i>native declaration : line 280</i><br>
	 * @deprecated use the safer methods {@link #EgMonitorPixFromMonMm(java.nio.IntBuffer, java.nio.IntBuffer, float, float)} and {@link #EgMonitorPixFromMonMm(com.sun.jna.ptr.IntByReference, com.sun.jna.ptr.IntByReference, float, float)} instead
	 */
	@Deprecated 
	void EgMonitorPixFromMonMm(IntByReference piIEgMontorPix, IntByReference piJEgMontorPix, float fXMonMm, float fYMonMm);
	
	/**
	 * Original signature : <code>void EgMonitorPixFromMonMm(int*, int*, float, float)</code><br>
	 * <i>native declaration : line 280</i>
	 */
	void EgMonitorPixFromMonMm(IntBuffer piIEgMontorPix, IntBuffer piJEgMontorPix, float fXMonMm, float fYMonMm);
	
	/**
	 * Original signature : <code>void MonMmFromEgMonitorPix(float*, float*, float*, int, int)</code><br>
	 * @param pfZMonMm pointer to Z may be null<br>
	 * <i>native declaration : line 285</i><br>
	 * @deprecated use the safer methods {@link #MonMmFromEgMonitorPix(java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, int, int)} and {@link #MonMmFromEgMonitorPix(com.sun.jna.ptr.FloatByReference, com.sun.jna.ptr.FloatByReference, com.sun.jna.ptr.FloatByReference, int, int)} instead
	 */
	@Deprecated 
	void MonMmFromEgMonitorPix(FloatByReference pfXMonMm, FloatByReference pfYMonMm, FloatByReference pfZMonMm, int iIEgMontorPix, int iJEgMontorPix);
	
	/**
	 * Original signature : <code>void MonMmFromEgMonitorPix(float*, float*, float*, int, int)</code><br>
	 * @param pfZMonMm pointer to Z may be null<br>
	 * <i>native declaration : line 285</i>
	 */
	void MonMmFromEgMonitorPix(FloatBuffer pfXMonMm, FloatBuffer pfYMonMm, FloatBuffer pfZMonMm, int iIEgMontorPix, int iJEgMontorPix);

	/**
	 * Original signature : <code>void EgMonitorPixFromEgWindowPix(int*, int*, int, int)</code><br>
	 * <i>native declaration : line 291</i><br>
	 * @deprecated use the safer methods {@link #EgMonitorPixFromEgWindowPix(java.nio.IntBuffer, java.nio.IntBuffer, int, int)} and {@link #EgMonitorPixFromEgWindowPix(com.sun.jna.ptr.IntByReference, com.sun.jna.ptr.IntByReference, int, int)} instead
	 */
	@Deprecated 
	void EgMonitorPixFromEgWindowPix(IntByReference piIEgMonitorPix, IntByReference piJEgMonitorPix, int iIEgWindowPix, int iJEgWindowPix);

	/**
	 * Original signature : <code>void EgMonitorPixFromEgWindowPix(int*, int*, int, int)</code><br>
	 * <i>native declaration : line 291</i>
	 */
	void EgMonitorPixFromEgWindowPix(IntBuffer piIEgMonitorPix, IntBuffer piJEgMonitorPix, int iIEgWindowPix, int iJEgWindowPix);
	
	/**
	 * Original signature : <code>void EgWindowPixFromEgMonitorPix(int*, int*, int, int)</code><br>
	 * <i>native declaration : line 296</i><br>
	 * @deprecated use the safer methods {@link #EgWindowPixFromEgMonitorPix(java.nio.IntBuffer, java.nio.IntBuffer, int, int)} and {@link #EgWindowPixFromEgMonitorPix(com.sun.jna.ptr.IntByReference, com.sun.jna.ptr.IntByReference, int, int)} instead
	 */
	@Deprecated 
	void EgWindowPixFromEgMonitorPix(IntByReference piIEgWindowPix, IntByReference piJEgWindowPix, int iIEgMonitorPix, int iJEgMonitorPix);
	
	/**
	 * Original signature : <code>void EgWindowPixFromEgMonitorPix(int*, int*, int, int)</code><br>
	 * <i>native declaration : line 296</i>
	 */
	void EgWindowPixFromEgMonitorPix(IntBuffer piIEgWindowPix, IntBuffer piJEgWindowPix, int iIEgMonitorPix, int iJEgMonitorPix);

	/**
	 * Original signature : <code>void GdsPixFromMonMm(int*, int*, float, float)</code><br>
	 * <i>native declaration : line 301</i><br>
	 * @deprecated use the safer methods {@link #GdsPixFromMonMm(java.nio.IntBuffer, java.nio.IntBuffer, float, float)} and {@link #GdsPixFromMonMm(com.sun.jna.ptr.IntByReference, com.sun.jna.ptr.IntByReference, float, float)} instead
	 */
	@Deprecated 
	void GdsPixFromMonMm(IntByReference piIGdsPix, IntByReference piJGdsPix, float fXMonMm, float fYMonMm);

	/**
	 * Original signature : <code>void GdsPixFromMonMm(int*, int*, float, float)</code><br>
	 * <i>native declaration : line 301</i>
	 */
	void GdsPixFromMonMm(IntBuffer piIGdsPix, IntBuffer piJGdsPix, float fXMonMm, float fYMonMm);
	
	/**
	 * Original signature : <code>void MonMmFromGdsPix(float*, float*, float*, int, int)</code><br>
	 * @param pfZMonMm pointer to Z may be null<br>
	 * <i>native declaration : line 306</i><br>
	 * @deprecated use the safer methods {@link #MonMmFromGdsPix(java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, int, int)} and {@link #MonMmFromGdsPix(com.sun.jna.ptr.FloatByReference, com.sun.jna.ptr.FloatByReference, com.sun.jna.ptr.FloatByReference, int, int)} instead
	 */
	@Deprecated 
	void MonMmFromGdsPix(FloatByReference pfXMonMm, FloatByReference pfYMonMm, FloatByReference pfZMonMm, int iIGdsPix, int iJGdsPix);
	
	/**
	 * Original signature : <code>void MonMmFromGdsPix(float*, float*, float*, int, int)</code><br>
	 * @param pfZMonMm pointer to Z may be null<br>
	 * <i>native declaration : line 306</i>
	 */
	void MonMmFromGdsPix(FloatBuffer pfXMonMm, FloatBuffer pfYMonMm, FloatBuffer pfZMonMm, int iIGdsPix, int iJGdsPix);
	
	/**
	 * Original signature : <code>void ScaleEgMonPixFromMm(int*, int*, float, float)</code><br>
	 * <i>native declaration : line 312</i><br>
	 * @deprecated use the safer methods {@link #ScaleEgMonPixFromMm(java.nio.IntBuffer, java.nio.IntBuffer, float, float)} and {@link #ScaleEgMonPixFromMm(com.sun.jna.ptr.IntByReference, com.sun.jna.ptr.IntByReference, float, float)} instead
	 */
	@Deprecated 
	void ScaleEgMonPixFromMm(IntByReference piIPix, IntByReference piJPix, float fXMm, float fYMm);
	
	/**
	 * Original signature : <code>void ScaleEgMonPixFromMm(int*, int*, float, float)</code><br>
	 * <i>native declaration : line 312</i>
	 */
	void ScaleEgMonPixFromMm(IntBuffer piIPix, IntBuffer piJPix, float fXMm, float fYMm);
	
	/**
	 * Original signature : <code>void ScaleEgMonMmFromPix(float*, float*, int, int)</code><br>
	 * <i>native declaration : line 317</i><br>
	 * @deprecated use the safer methods {@link #ScaleEgMonMmFromPix(java.nio.FloatBuffer, java.nio.FloatBuffer, int, int)} and {@link #ScaleEgMonMmFromPix(com.sun.jna.ptr.FloatByReference, com.sun.jna.ptr.FloatByReference, int, int)} instead
	 */
	@Deprecated 
	void ScaleEgMonMmFromPix(FloatByReference pfXMm, FloatByReference pfYMm, int iIPix, int iJPix);
	
	/**
	 * Original signature : <code>void ScaleEgMonMmFromPix(float*, float*, int, int)</code><br>
	 * <i>native declaration : line 317</i>
	 */
	void ScaleEgMonMmFromPix(FloatBuffer pfXMm, FloatBuffer pfYMm, int iIPix, int iJPix);
	
	
	/**
	 * Eye Image functions:<br>
	 * Original signature : <code>_stEyeImageInfo* EgEyeImageInit(_stEyeImageInfo*, int)</code><br>
	 * <i>native declaration : line 324</i>
	 */
	_stEyeImageInfo EgEyeImageInit(_stEyeImageInfo stEyeImageInfo, int iDivisor);
	
	/**
	 * Original signature : <code>void EgEyeImageDisplay(int, int, int, int, int, HDC)</code><br>
	 * <i>native declaration : line 328</i>
	 */
	void EgEyeImageDisplay(int iVis, int iX, int iY, int iWidth, int iHeight, HDC hdc);
	
	public static class BITMAPINFO extends PointerType {
		public BITMAPINFO(Pointer address) {
			super(address);
		}
		public BITMAPINFO() {
			super();
		}
	};
	

	
}
