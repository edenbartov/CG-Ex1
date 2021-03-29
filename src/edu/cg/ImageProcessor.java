package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
	
	//MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;
	
	//MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
			RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //Initializing for each loops...
		
		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}
	
	public ImageProcessor(Logger logger,
			BufferedImage workingImage,
			RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}
	
	//MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	//MARK: Nearest neighbor - example
	public BufferedImage nearestNeighbor() {
		logger.log("applies nearest neighbor interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			int imgX = (int)Math.round((x*inWidth) / ((float)outWidth));
			int imgY = (int)Math.round((y*inHeight) / ((float)outHeight));
			imgX = Math.min(imgX,  inWidth-1);
			imgY = Math.min(imgY, inHeight-1);
			ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	//MARK: Unimplemented methods
	public BufferedImage greyscale() {
		logger.log("Preparing for greyscale...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int gray = (c.getRed()*r + c.getGreen()*g + c.getBlue()*b)/(r+g+b);
			Color color = new Color(gray,gray,gray);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Greyscale done!");

		return ans;
	}

	public BufferedImage gradientMagnitude() {

		logger.log("Preparing for Gradient Magnitude...");

		BufferedImage newWorkingImage = greyscale();
		BufferedImage ans = newEmptyInputSizedImage();
		forEach((y, x) -> {
			int nextX = 1;
			int nextY = 1;

			if(x >= inWidth - 1) {
				nextX = -1;
			}
			if(y >= inHeight - 1) {
				nextY = -1;
			}
			int currentColor =  new Color(newWorkingImage.getRGB(x, y)).getBlue();
			int Dx = currentColor - new Color(newWorkingImage.getRGB(x + nextX, y)).getBlue();
			int Dy = currentColor - new Color(newWorkingImage.getRGB(x, y + nextY)).getBlue();
			int nextColor = Math.min((int) Math.sqrt((Math.pow(Dx,2) + Math.pow(Dy, 2)) / 2), 255);
			Color color = new Color(nextColor,nextColor,nextColor);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Gradient Magnitude done!");

		return ans;
	}

	public BufferedImage bilinear() {
		logger.log("applies bilinear interpolation.");

		BufferedImage ans = newEmptyOutputSizedImage();
		double xRatio = (double)inWidth / ans.getWidth();
		double yRatio = (double)inHeight / ans.getHeight();
		setForEachOutputParameters();
		forEach((y,x) -> {
			double xTransform = x * xRatio;
			double yTransform = y * yRatio;
			int xUp = (int)Math.ceil(xTransform);
			xUp = Math.min(xUp,inWidth-1);
			int yUp = (int)Math.ceil(yTransform);
			yUp = Math.min(yUp,inHeight-1);
			int xDown = (int)Math.floor(xTransform);
			int yDown = (int)Math.floor(yTransform);
			Color upRight = new Color(workingImage.getRGB(xUp,yDown));
			Color upLeft = new Color(workingImage.getRGB(xDown,yDown));
			Color downRight =  new Color(workingImage.getRGB(xUp,yUp));
			Color downLeft = new Color(workingImage.getRGB(xDown,yUp));
			int r = (upRight.getRed() + upLeft.getRed() + downRight.getRed() + downLeft.getRed())/4;
			int g = (upRight.getGreen() + upLeft.getGreen() + downRight.getGreen() + downLeft.getGreen())/4;
			int b = (upRight.getBlue() + upLeft.getBlue() + downRight.getBlue() + downLeft.getBlue())/4;
			Color c = new Color(r,g,b);
			ans.setRGB(x,y,c.getRGB());
		});
		logger.log("done bilinear interpolation.");
		return ans;
	}
	
	//MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}
	
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}
	
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}
	
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		
		forEach((y, x) -> 
			output.setRGB(x, y, workingImage.getRGB(x, y))
		);
		
		return output;
	}
}
