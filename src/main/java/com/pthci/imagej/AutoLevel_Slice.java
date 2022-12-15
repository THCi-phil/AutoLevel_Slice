/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
  *
 * Invert_image image code 2022 Prof Phil Threlfall-Holmes, TH Collaborative Innovation
 * modification from tutorial template, licence terms unmodified.
 */

package com.pthci.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;


public class AutoLevel_Slice implements PlugInFilter {
	protected ImagePlus image;

	// image property members
	private int width   ;
	private int height  ;
	private int type    ;
	private int nSlices ;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	} //end public int setup(String arg, ImagePlus imp)
	//-----------------------------------------------------


	@Override
	public void run(ImageProcessor ip) {
		width   = ip.getWidth();    //in pixel units
		height  = ip.getHeight();
		type    = image.getType();
		nSlices = image.getStackSize();
		process(image);
		image.updateAndDraw();
	} //end public void run(ImageProcessor ip)
	//-----------------------------------------------------


	/**
	 * Process an image.
	 * <p>
	 * Please provide this method even if {@link ij.plugin.filter.PlugInFilter} does require it;
	 * the method {@link ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)} can only
	 * handle 2-dimensional data.
	 * </p>
	 * <p>
	 * If your plugin does not change the pixels in-place, make this method return the results and
	 * change the {@link #setup(java.lang.String, ij.ImagePlus)} method to return also the
	 * <i>DOES_NOTHING</i> flag.
	 * </p>
	 *
	 * @param image the image (possible multi-dimensional)
	 */
	public void process(ImagePlus image) {
		// slice numbers start with 1 for historical reasons
		for (int i = 1; i <= nSlices; i++)
			process( image.getStack().getProcessor(i) );
	} //end public void process(ImagePlus image) 
	//-----------------------------------------------------


	// Select processing method depending on image type
	public void process(ImageProcessor ip) {
		if      (type == ImagePlus.GRAY8    ) process( (byte[])  ip.getPixels() );
		else if (type == ImagePlus.GRAY16   ) process( (short[]) ip.getPixels() );
		else if (type == ImagePlus.GRAY32   ) process( (float[]) ip.getPixels() );
		else if (type == ImagePlus.COLOR_RGB) process( (int[])   ip.getPixels() );                             
		else {
			throw new RuntimeException("not supported");
		}
	} //end public void process(ImageProcessor ip)
	//-----------------------------------------------------


	// processing of GRAY8 images
	public void process(byte[] pixels) {
		//pixels = ip.getPixels() is a 1-D array, not a 2D array as you would intuit, so pixels[x+y*width] instead of pixels[x,y]
		//here as per Invert_Image we can just pixelPos++ through the array for maximum speed
		//
		//Images are 8-bit (unsigned, i.e. values between 0 and 255).
		//Java has no data type for unsigned 8-bit integers: the byte type is signed , so we have to use the & 0xff dance
		//(a Boolean AND operation) to make sure that the value is treated as unsigned integer,
		//do the comparisons as integers, and Math.round() is a doublemethod, and then cast back to byte
		//The & operator promotes to int and then that int in a double subraction promotes to double, so doesn't need an explicit cast.

		int thisSliceMin = 255 ; //set as the max possible, update with each value lower
		int thisSliceMax =   0 ; //set as the min possible, update with each value larger
		int testedPixelValue ;
		double gradient ;
		
		//first pass to find min and max
		for( int pixelPos=0; pixelPos<(width*height); pixelPos++ ) {
			testedPixelValue = pixels[pixelPos] & 0xff ;
			if( testedPixelValue < thisSliceMin ) thisSliceMin = testedPixelValue ;
			if( testedPixelValue > thisSliceMax ) thisSliceMax = testedPixelValue ;
		}  //end for min-max scan
		//then second pass to re-level the values
		gradient = (double)( (double)255.0 / ( (double)thisSliceMax - (double)thisSliceMin ) );
		for( int pixelPos=0; pixelPos<(width*height); pixelPos++ ) {
			pixels[pixelPos] = (byte)( (int)Math.round( ( (pixels[pixelPos] & 0xff) -(double)thisSliceMin )*gradient ) );
		}  //end for set re-level
	} //end public void process(byte[] pixels)
  //-----------------------------------------------------


	// processing of GRAY16 images
	public void process(short[] pixels) {
		//Java short is 16 bit signed, so -32,768 to 32,767
		//Java int is 32 bit, signed -2,147,483,648 to 2,147,483,647
		//so we can safely promote to int type for tested pixed value
		
		int thisSliceMin = 65535 ; //set as the max possible, update with each value lower
		int thisSliceMax =     0 ; //set as the min possible, update with each value larger
		int testedPixelValue ;
		double gradient ;
		
		//first pass to find min and max
		for( int pixelPos=0; pixelPos<(width*height); pixelPos++ ) {
			testedPixelValue = pixels[pixelPos] & 0xff ;
			if( testedPixelValue < thisSliceMin ) thisSliceMin = testedPixelValue ;
			if( testedPixelValue > thisSliceMax ) thisSliceMax = testedPixelValue ;
		}  //end for min-max scan
		//then second pass to re-level the values
		gradient = (double)( (double)65535.0 / ( (double)thisSliceMax - (double)thisSliceMin ) );
		for( int pixelPos=0; pixelPos<(width*height); pixelPos++ ) {
			pixels[pixelPos] = (short)( (int)Math.round( ( (pixels[pixelPos] & 0xff) -(double)thisSliceMin )*gradient ) );
		}  //end for set re-level
	} //end public void process(short[] pixels)
  //-----------------------------------------------------


	// processing of GRAY32 images	
	public void process( float[] pixels ) {
		//IJ.log("public void process GREY32");
		//Java int is 32 bit, signed -2,147,483,648 to 2,147,483,647
		//Java long is 64 bit, signed -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807
		//So it would be possible for ImageJ to implement 32-bit greyscale using the int data type
		//in the same way as (signed)byte is used to implement (unsigned)8-bit
		// or (signed)short for (unsigned)16-bit greyscale
		//But it is actually implemented by float, 0.0 (black) to 1.0 (white)
		//float data type is 32 bit
		//With a float data type, we don't need to cast
		
		float thisSliceMin = (float)1.0 ; //set as the max possible, update with each value lower
		float thisSliceMax = (float)0.0 ; //set as the min possible, update with each value larger
		float testedPixelValue ;
		float gradient ;
		
		//first pass to find min and max
		for( int pixelPos=0; pixelPos<(width*height); pixelPos++ ) {
			testedPixelValue = pixels[pixelPos];
			if( testedPixelValue < thisSliceMin ) thisSliceMin = testedPixelValue ;
			if( testedPixelValue > thisSliceMax ) thisSliceMax = testedPixelValue ;
		}  //end for min-max scan
		//then second pass to re-level the values
		gradient = (float)1.0 / ( thisSliceMax - thisSliceMin ) ;
		for( int pixelPos=0; pixelPos<(width*height); pixelPos++ ) {
			pixels[pixelPos] = (pixels[pixelPos] - thisSliceMin )*gradient ;
		}  //end for set re-level
	} //end public void process(float[] pixels)
  //-----------------------------------------------------


	// processing of COLOR_RGB images
	public void process(int[] pixels ) {
		//IJ.log("public void process RGB");
		//Red   [ 255,  0 ,  0  ] -> Cyan    [  0 , 255, 255 ];
		//Green [  0 , 255,  0  ] -> Magenta [ 255,  0 , 255 ];
		//Blue  [  0 ,  0 , 255 ] -> Yellow  [ 255, 255,  0  ];
		//Black [  0 ,  0 ,  0  ] -> White   [ 255, 255, 255 ];
		//White [ 255, 255, 255 ] -> Black   [  0 ,  0 ,  0  ];
		
		ColorProcessor cp = new ColorProcessor(width, height, pixels);
		
		byte[] R = new byte[ width*height ];
		byte[] G = new byte[ width*height ];
		byte[] B = new byte[ width*height ];
		
		cp.getRGB( R, G, B);

		//call the process( byte pixels ) method for each channel
		process( R );
		process( G );
		process( B );
		
		cp.setRGB( R, G, B );

	} //end public void process(int[] pixels)
  //-----------------------------------------------------


	

/*=================================================================================*/


	public void showAbout() {
		IJ.showMessage("AutoLevel Slice",
			"Set each slice scaled 0 to 255 (8bit example)"
		);
	} //end public void showAbout()
  //-----------------------------------------------------


/*=================================================================================*/

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) throws Exception {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<?> clazz = AutoLevel_Slice.class;
		java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		java.io.File file = new java.io.File(url.toURI());
		System.setProperty("plugins.dir", file.getAbsolutePath());

		// start ImageJ
		new ImageJ();

		ImagePlus image = IJ.openImage("d:/vertical spray test_bkgndRmvd.tif");
		//ImagePlus image = IJ.openImage("d:/test16bitBandWinvert.tif");
		//ImagePlus image = IJ.openImage("d:/test32bitBandWinvert.tif");
		//magePlus image = IJ.openImage("d:/testRGB.tif");
		
		// open the Clown sample
		//ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}  //end public static void main(String[] args)
  
/*=================================================================================*/
  
}  //end public class AutoLevel_Slice
//========================================================================================
//                         end public class AutoLevel_Slice
//========================================================================================