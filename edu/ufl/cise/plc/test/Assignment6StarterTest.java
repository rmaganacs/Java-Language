package edu.ufl.cise.plc.test;

import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.Program;
import edu.ufl.cise.plc.runtime.*;
import edu.ufl.cise.plc.runtime.javaCompilerClassLoader.PLCLangExec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Assignment6StarterTest {

	// Default package name for generated code
	String packageName = "cop4020sp22Package";
//	String packageName = "";

	boolean VERBOSE = true;

	private Object show(Object obj) throws IOException {
		if (VERBOSE) {
			if (obj instanceof BufferedImage) {
				show((BufferedImage) obj);
				pauseImageDisplay();
			} else {
				System.out.println(obj);
			}
		}
		return obj;
	}

	private BufferedImage show(BufferedImage image) {
		if (VERBOSE) {
			ConsoleIO.displayImageOnScreen(image);
			try {
				pauseImageDisplay();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return image;
	}

	/**
	 * This is the same as show(BufferedImage) except that the image is placed in a different location: 
	 * [0,0] is in center of screen rather than the upper left corner.
	 */

	private BufferedImage showRef(BufferedImage image) {
		if (VERBOSE) {
			ConsoleIO.displayReferenceImageOnScreen(image);
			try {
				pauseImageDisplay();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return image;
	}

	boolean PAUSE = true;
	int MILLIS = 2000;

	private void pauseImageDisplay() throws IOException {
		if (PAUSE) {
			/*
			 * If you would like Junit test to pause for input, uncomment The next two lines
			 * and comment the try-catch block.
			 */
//			System.out.print("Enter any character to terminate: ");
//			System.in.read();
			try {
				Thread.sleep(MILLIS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	static final float DELTA = .001f;

	String getName(ASTNode ast) throws Exception {
		if (ast instanceof Program) {
			return ((Program) ast).getName();
		} else
			throw new Exception("bug--expected ast was program");
	}

	String getReturnType(ASTNode ast) throws Exception {
		if (ast instanceof Program) {
			return ((Program) ast).getReturnType().toString();
		} else
			throw new Exception("bug--expected ast was program");
	}

	Object check(String input, Object expectedResult) throws Exception {
		return check(input, null, expectedResult);
	}

	Object check(String input, Object[] params, Object expected) throws Exception {
		Object actual = exec(input, params);
		if (expected instanceof BufferedImage) {
			BufferedImage actualImage = (BufferedImage) actual;
			BufferedImage expectedImage = (BufferedImage) expected;
			int[] actualPixels = ImageOps.getRGBPixels(actualImage);
			int[] expectedPixels = ImageOps.getRGBPixels(expectedImage);
			assertArrayEquals(expectedPixels, actualPixels);
		} else {
			assertEquals(expected, actual);
		}
		return actual;
	}

	Object exec(String input, Object[] params) throws Exception {
		return (new PLCLangExec(packageName, VERBOSE)).exec(input, params);
	}

	Object exec(String input) throws Exception {
		return (new PLCLangExec(packageName, VERBOSE)).exec(input, null);
	}

	// need separate method to check floats due to DELTA. Note in simple cases, just
	// use normal check.
	void checkFloatResult(String input, float expectedResult) throws Exception {
		checkFloatResult(input, null, expectedResult);
	}

	void checkFloatResult(String input, Object[] params, float expectedResult) throws Exception {
		Object result = exec(input, params);
		show("result = " + result);
		assertEquals(expectedResult, (float) result, DELTA);
	}

	@AfterEach
	public void closeFiles() {
		FileURLIO.closeFiles();
	}

	@Test
	void lectureExample3() throws Exception {
		String input = """
				image a(string url, int width, int height)
				      image[width,height] b <- url;
				      ^b;
				      """;
		String url = "https://ufl.discovery.academicanalytics.com/assets/ufl/external/academics.jpg";
		Object[] params = { url, 300, 200 };
		BufferedImage refImage = showRef(FileURLIO.readImage(url, 300, 200));
		show(check(input, params, refImage));
	}

	@Test
	void lectureExample4() throws Exception {
		String input = """
				image f(int widthAndHeight) image[widthAndHeight,widthAndHeight] a;
				            a[x,y] = <<x-y, 0, y-x>>;
				            ^ a;
				""";
		int size = 500;
		// create reference image
		BufferedImage refImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				refImage.setRGB(x, y, (new ColorTuple(x - y, 0, y - x)).pack());
			}
		}
		// run PLCLang program
		Object[] params = { size };
		show(check(input, params, refImage));
	    //If you don't want to compare with ref image use show(exec(input,params));
	}
	
	
	@Test
	void lectureExample5() throws Exception {
		String input = """
				image flag(int size)
				   image[size,size] f;
				   int stripSize = size/2;
				   f[x,y] = if (y > stripSize) YELLOW else BLUE fi;
				   ^f;
				   """;
		int size = 1024;		
		BufferedImage refImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				refImage.setRGB(x, y, (y>size/2)? Color.YELLOW.getRGB() : Color.BLUE.getRGB());
			}
		}
		Object[] params = { size };
		show(check(input, params, refImage));
	}
	
	@Test
	void lectureExample6() throws Exception {
		String input = """
				image f(string url)
                image a <- url;
                write a -> console;
                image b = a/3;
                ^b;

				""";
		String url = "https://ufl.discovery.academicanalytics.com/assets/ufl/external/academics.jpg";
		BufferedImage inputImage = FileURLIO.readImage(url);
		int w = inputImage.getWidth();
		int h = inputImage.getHeight();
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				ColorTuple pixel = ColorTuple.unpack(inputImage.getRGB(x, y));
				int newPackedPixel = (new ColorTuple(pixel.red/3,pixel.green/3,pixel.blue/3)).pack();
				refImage.setRGB(x, y, newPackedPixel);
			}
		Object[] params = {url};
		ConsoleIO.displayReferenceImageOnScreen(inputImage);
		//this image should be the same size, but darker than inputImage
		show(check(input, params, refImage));
	}
	
	


	@Test
	void colorArithmetic0() throws Exception {
		String input = """
				color f()
				color a = <<50,60,70>>;
				color b = <<13,14,15>>;
				^ a+b;
				""";
		check(input, new edu.ufl.cise.plc.runtime.ColorTuple(50 + 13, 60 + 14, 70 + 15));
	}

	@Test
	void colorArithmetic1() throws Exception {
		String input = """
				color f()
				color a = 10;
				color b = <<13,14,15>>;
				^ a+b;
				""";
		check(input, new edu.ufl.cise.plc.runtime.ColorTuple(10 + 13, 10 + 14, 10 + 15));
	}

	@Test
	void imageArithemtic0() throws Exception {
		String input = """
				image testImageArithemtic0()
				image[500,500] blue;
				blue[x,y] = BLUE;
				image[500,500] green;
				green[a,b] = GREEN;
				image[500,500] teal;
				teal[x,y] = blue[x,y] + green[x,y];
				^teal;
				""";
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int teal = Color.GREEN.getRGB() | Color.BLUE.getRGB(); //bitwise or, teal = ff00ffff
		System.out.println("teal=" + Integer.toHexString(teal));
		int[] rgbArray = new int[size];
		Arrays.fill(rgbArray, teal);
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		show(check(input, refImage));
	}
	@Test
	void colorBool() throws Exception {
		String input = """
				boolean f()
				color a = <<50,60,70>>;
				color b = <<13,14,15>>;
				^ a == b;
				""";
		check(input, false);
		String input2 = """
				boolean f()
				color a = <<50,60,70>>;
				color b = <<13,14,15>>;
				^ a != b;
				""";
		check(input2, true);
	}

	//Possibly fixed, verify first
	@Test
	void testAssigningOneValueToImage() throws Exception{
		String input = """
        image f()
              image[500, 500] b;
              b = 100;
              ^b;
        """;
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int[] rgbArray = new int[size];
		Color hundred = new Color(100, 100, 100);
		Arrays.fill(rgbArray, hundred.getRGB());
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		show(check(input, refImage));
	}

	@Test
	void testAssigningOneColorToImage() throws Exception{
		String input = """
        image f()
              image[500, 500] b;
              b = BLUE;
              ^b;
        """;
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int blue = Color.BLUE.getRGB();
		int[] rgbArray = new int[size];
		Arrays.fill(rgbArray, blue);
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		ConsoleIO.displayReferenceImageOnScreen(refImage);
		show(check(input, refImage));
	}

	@Test
	void testAssignImageToImageWithDimension() throws Exception {
		String input = """
				image f()
				      image[500, 500] b;
				      b = BLUE;
				      image[200, 200] c;
				      c = b;
				      ^c;
				""";
		int w = 200;
		int h = 200;
		int size = w * h;
		BufferedImage refImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		int blue = Color.BLUE.getRGB();
		//int blue = ColorTuple.toColorTuple(Color.BLUE).pack(); //if above one doesn't work
		int[] rgbArray = new int[size];
		Arrays.fill(rgbArray, blue);
		refImage.setRGB(0, 0, w, h, rgbArray, 0, w);
		//ConsoleIO.displayReferenceImageOnScreen(refImage);
		show(check(input, refImage));
	}

	@Test
	void testAssignImageToImageWithoutDimension() throws Exception{
		String input = """
        image f(string url)
              image b <- url;
              image[300, 300] c = RED;
              b = c;
              ^b;
        """;
		int w = 300;
		int h = 300;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int blue = Color.RED.getRGB();
		//int blue = ColorTuple.toColorTuple(Color.BLUE).pack(); //if above one doesn't work
		int[] rgbArray = new int[size];
		Arrays.fill(rgbArray, blue);
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		//ConsoleIO.displayReferenceImageOnScreen(refImage);
		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		Object[] params = {url};
		//this image should be the same size, but darker than inputImage
		show(check(input, params, refImage));
	}

	@Test
	void testColorExpressions() throws Exception {
		String input = """
        image a(int width, int height)
              image[width, height] f;
              float x = width;
              float y = height;
              f[g,h] = <<(g/x*255), 0.0, (h/y*255)>>;
              ^f;
              """;
		int width = 640;
		int height = 480;
		Object[] params = {width, height};
		BufferedImage refImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				refImage.setRGB(x, y, (new ColorTupleFloat(x/(float)width*255, 0.0f, y/(float)height*255)).pack());
			}
		}
		showRef(refImage);
		show(check(input, params, refImage));
	}


	// Enter 100 200 100
	@Test
	void readColorFromConsole() throws Exception{
		String input = """
        image f()
              image[500, 500] b;
              color a;
              a <- console;
              b = a;
              ^b;
        """;
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int blue = Color.BLUE.getRGB();
		//int blue = ColorTuple.toColorTuple(Color.BLUE).pack(); //if above one doesn't work
		int[] rgbArray = new int[size];
		Color hundred = new Color(100, 200, 100);
		Arrays.fill(rgbArray, hundred.getRGB());
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		//ConsoleIO.displayReferenceImageOnScreen(refImage);
		show(check(input, refImage));
	}





	@Test
	void colorArithmetic3() throws Exception {
		String input = """
        color f()
        image[100,100] a;
        a[x,y] = 10;
        ^ a[0, 0];
        """;
		check(input, new edu.ufl.cise.plc.runtime.ColorTuple(10, 10, 10));
	}



	@Test
	void testExtractRed() throws Exception {
		String input = """
     image f(string url)
              image a <- url;
              image b = getRed(a); 
              ^b;

        """;
		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		BufferedImage inputImage = FileURLIO.readImage(url);
		int w = inputImage.getWidth();
		int h = inputImage.getHeight();
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				ColorTuple pixel = ColorTuple.unpack(inputImage.getRGB(x, y));
				int newPackedPixel = (new ColorTuple(pixel.red,0,0)).pack();
				refImage.setRGB(x, y, newPackedPixel);
			}
		Object[] params = {url};
		show(check(input, params, refImage));
	}









	@Test
	void testGetRGBFromColor() throws Exception {
		String input = """
     int f()
              color a = <<10, 20, 30>>;
              int r = getRed(a);
              int g = getBlue(a);
              int b = getGreen(a);
              ^r + g + b;

        """;

		show(check(input, 60));
	}


	@Test
	void testWriteToFile() throws Exception {
		String input = """
        void f(string url)
           image b <- url;
           image c = getBlue(b);
           write c -> "blueImage";
           """;

		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		BufferedImage inputImage = FileURLIO.readImage(url);
		int w = inputImage.getWidth();
		int h = inputImage.getHeight();
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				ColorTuple pixel = ColorTuple.unpack(inputImage.getRGB(x, y));
				int newPackedPixel = (new ColorTuple(0,0,pixel.blue)).pack();
				refImage.setRGB(x, y, newPackedPixel);
			}

		Object[] params = {url};
		exec(input, params);
		ConsoleIO.displayReferenceImageOnScreen(refImage);
		File file = new File("blueImage.jpeg");
		assertEquals(true, file.exists());
	}

	@Test
	void testWriteTextToFile() throws Exception {
		String input = """
        void f()
           string message = "Hello World!";
           write message -> "textFile";
           """;



		exec(input);
//writeValue writes to file with no extension
		File file = new File("textFile");
		assertEquals(true, file.exists());

//readValueFromFile returns a string
		Object File = FileURLIO.readValueFromFile("textFile");
		assertEquals("Hello World!", File);

	}



}
