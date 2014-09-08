package imd.eyetracking.lib;
import imd.eyetracking.lib.Lctigaze.LctigazeDll.BITMAPINFO;

import com.ochafik.lang.jnaerator.runtime.Structure;
import com.sun.jna.Pointer;

/**
 * The _stEyeImageInfo contains information about the eye image.
 * <i>native declaration : line 172</i><br>
 */
public class _stEyeImageInfo extends Structure<_stEyeImageInfo, _stEyeImageInfo.ByValue, _stEyeImageInfo.ByReference > {
	
	/**
	 * pointer to RGB eye image data<br>
	 * C type : unsigned char*
	 */
	public Pointer prgbEyeImage;
	
	/**
	 * bitmap information<br>
	 * C type : BITMAPINFO
	 */
	public BITMAPINFO bmiEyeImage;
	
	public int iWidth;
	
	public int iHeight;
	
	public _stEyeImageInfo() {
		super();
		initFieldOrder();
	}
	
	protected void initFieldOrder() {
		setFieldOrder(new String[]{"prgbEyeImage", "bmiEyeImage", "iWidth", "iHeight"});
	}
	
	/**
	 * @param prgbEyeImage pointer to RGB eye image data<br>
	 * C type : unsigned char*<br>
	 * @param bmiEyeImage bitmap information<br>
	 * C type : BITMAPINFO
	 */
	public _stEyeImageInfo(Pointer prgbEyeImage, BITMAPINFO bmiEyeImage, int iWidth, int iHeight) {
		super();
		this.prgbEyeImage = prgbEyeImage;
		this.bmiEyeImage = bmiEyeImage;
		this.iWidth = iWidth;
		this.iHeight = iHeight;
		initFieldOrder();
	}
	
	protected ByReference newByReference() { return new ByReference(); }
	protected ByValue newByValue() { return new ByValue(); }
	protected _stEyeImageInfo newInstance() { return new _stEyeImageInfo(); }
	public static _stEyeImageInfo[] newArray(int arrayLength) {
		return Structure.newArray(_stEyeImageInfo.class, arrayLength);
	}
	public static class ByReference extends _stEyeImageInfo implements Structure.ByReference {
		
	};
	public static class ByValue extends _stEyeImageInfo implements Structure.ByValue {
		
	};
}
